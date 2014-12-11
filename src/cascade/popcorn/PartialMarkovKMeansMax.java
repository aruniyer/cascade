package cascade.popcorn;

import java.util.Arrays;

import weka.clusterers.AbstractClusterer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.unsupervised.attribute.Remove;

public class PartialMarkovKMeansMax extends FullMarkovKMeansMax {

	private static final long serialVersionUID = 1L;
	protected Remove remove;
	
	public PartialMarkovKMeansMax(String[] locations, int numClusters, int[] clusteringAttributes, int seed) throws Exception {
		super(locations, numClusters, seed);
		if (clusteringAttributes != null) {
			this.remove = new Remove();
			this.remove.setInvertSelection(true);
			this.remove.setAttributeIndicesArray(clusteringAttributes);
		} else {
			throw new IllegalStateException("Clustering attributes cannot be null!");
		}
	}
	
	protected double[] getFinalClusterDistribution(Instance instance, int timeStep) throws Exception {
		int timeSteps = simpleKMeans.length;
		double[] finalClusterDistribution = new double[simpleKMeans[simpleKMeans.length - 1].getNumClusters()];
		for (int simulations = 0; simulations < 1000; simulations++) {
			int label = simpleKMeans[timeStep].clusterInstance(filterAttributes(instance));
			int prevState = label;
			for (int i = timeStep; i < timeSteps - 1; i++) {
				double[] cdf = new double[simpleKMeans[i + 1].getNumClusters()];
				double[][] transition = mm.get(i);
//				System.out.print(i + ":: size of transition = " + transition.length + "," + transition[0].length);
//				System.out.println(" :: cluster sizes = " + simpleKMeans[i].getNumClusters() + "," + simpleKMeans[i + 1].getNumClusters());
				cdf[0] = transition[prevState][0];
				for (int j = 1; j < cdf.length; j++) {
					cdf[j] = cdf[j - 1] + transition[prevState][j];
				}
				double rand = Math.random();
				int currState = -1;
				for (int j = 0; j < cdf.length; j++) {
					if (rand <= cdf[j]) {
						currState = j;
						break;
					}
				}
				prevState = currState;
			}
			finalClusterDistribution[prevState]++;
		}
		double total = 0;
		for (int i = 0; i < finalClusterDistribution.length; i++) {
			total += finalClusterDistribution[i];
		}
		for (int i = 0; i < finalClusterDistribution.length; i++) {
			finalClusterDistribution[i] /= total;
		}
		return finalClusterDistribution;
	}
	
	protected Instance[] centroids;
	
	protected void loadFullCentroids() throws Exception {
		if (centroids != null)
			return;
		DataSource dataSourceAtEnd = new DataSource(locations[locations.length - 1]);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		Instances partialCentroids = simpleKMeans[simpleKMeans.length - 1].getClusterCentroids();
		centroids = new Instance[simpleKMeans[simpleKMeans.length - 1].getNumClusters()];
		double[] minDistances = new double[centroids.length];
		Arrays.fill(minDistances, Double.MAX_VALUE);
		for (int i = 0; i < instancesAtEnd.numInstances(); i++) {
			Instance instance = instancesAtEnd.get(i);
			Instance filtered = filterAttributes(instance);
			int label = simpleKMeans[simpleKMeans.length - 1].clusterInstance(filtered);
			double dist = getDistance(partialCentroids.get(label), filtered);
			if (dist < minDistances[label]) {
				minDistances[label] = dist;
				centroids[label] = instance;
			}
		}
	}
	
	protected double getDistance(Instance in1, Instance in2) {
		return simpleKMeans[simpleKMeans.length - 1].getDistanceFunction().distance(in1, in2);
	}
	
	public Instance doPrediction(Instance instance, int timeStep) throws Exception {
		double[] distribution = getFinalClusterDistribution(instance, timeStep);
		double max = 0;
		int maxIndex = -1;
		for (int i = 0; i < distribution.length; i++) {
			if (max < distribution[i]) {
				max = distribution[i];
				maxIndex = i;
			}
		}
		if (remove != null) {
			loadFullCentroids();
		}
		return centroids[maxIndex];
	}
	
	protected Result getMSE(Instances instancesAt0, Instances instancesAtEnd, int timeStep) throws Exception {
		System.out.print("Computing MSE ... ");
		long start = System.currentTimeMillis();
		Result result = new Result();
		int countAttr = instancesAt0.numAttributes();
		result.rmse = new double[countAttr];
		result.re = new double[countAttr];
		result.clusterPredictionAccuracy = 0;
		int count = 0;
		for (int i = 0; i < instancesAt0.size(); i++) {
			Instance instanceAt0 = instancesAt0.get(i);
			Instance instanceAtEnd = instancesAtEnd.get(i);
			Instance prediction = doPrediction(instanceAt0, timeStep);
			if (prediction != null) {
				relativeError(result.re, instanceAtEnd, prediction);
				computeDiffSquare(result.rmse, instanceAtEnd, prediction);				
				int actualCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(filterAttributes(instanceAtEnd));
				int predictedCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(filterAttributes(prediction));
				if (actualCluster == predictedCluster) {
					result.clusterPredictionAccuracy++;
				}
				count++;
			}
		}
		for (int i = 0; i < result.rmse.length; i++) {
			result.re[i] /= count;
			result.rmse[i] /= count;
			result.rmse[i] = Math.sqrt(result.rmse[i]);
		}
		result.attributes = new String[instancesAtEnd.numAttributes()];
		for (int i = 0; i < result.attributes.length; i++) {
			result.attributes[i] = instancesAtEnd.attribute(i).name();
		}
		result.clusterPredictionAccuracy = (result.clusterPredictionAccuracy / count);
		long end = System.currentTimeMillis();
		System.out.println("[DONE] (" + ((end - start) / 1000) + "secs)");
		return result;
	}
	
	protected void doClustering(String location, int numClusters, AbstractClusterer clusterer) throws Exception {
		DataSource dataSource = new DataSource(location);
		Instances instances = dataSource.getDataSet();
		Instances filteredInstances = instances;
		if (remove != null) {
			remove.setInputFormat(instances);
			filteredInstances = remove.getOutputFormat();
			for (Instance instance : instances) {
				filteredInstances.add(filterAttributes(instance));
			}
		}
		clusterer.buildClusterer(filteredInstances);
	}
	
	private Instance filterAttributes(Instance instance) {
		if (remove != null) {
			remove.input(instance);
			return remove.output();
		} else {
			return instance;
		}
	}
	
}