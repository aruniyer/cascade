package com.yahoo.evolution.popcorn;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import weka.clusterers.AbstractClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ConverterUtils.DataSource;

public class DiffMarkovKMeansMax implements IMarkovModel {

	protected static final long serialVersionUID = 1L;
	protected double[] initialStateProbabilities;
	protected List<double[][]> mm;
	protected SimpleKMeans[] simpleKMeans;
	protected String[] locations; 
	
	public DiffMarkovKMeansMax(String[] locations, String[] max, int numClusters, int seed) throws Exception {
		this.locations = locations;
		// TODO: load max values
		System.out.print("Setting clustering parameters ... ");
		this.setClusteringParameters(locations.length, numClusters, seed);
		System.out.println("[DONE]");
		System.out.println("Will run clustering with following options:");
		System.out.println(Arrays.asList(simpleKMeans[0].getOptions()));
	}
	
	double[][] maxAt = {
			{101},
			{94},
			{146},
			{258},
			{295},
			{133},
			{159},
			{978}
	};
	protected double getFinalClusterDistribution(Instance instance, int timeStep) throws Exception {
		int timeSteps = simpleKMeans.length;
		int label = simpleKMeans[timeStep].clusterInstance(instance);
		int prevState = label;

		int numSimulations = 1000;
		double maxPathProb = Double.NEGATIVE_INFINITY;
		double maxPredictSize = -1;
		for (int simulations = 0; simulations < numSimulations; simulations++) {
			Double finalInstance = new Double(0);
			double pathProb = 0;
			for (int i = timeStep; i < timeSteps - 1; i++) {
				double[] cdf = new double[simpleKMeans[i + 1].getNumClusters()];
				double[][] transition = mm.get(i);
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
				double edgeProb = transition[prevState][currState];
				pathProb += Math.log(edgeProb);
				prevState = currState;
				Instance toAdd = simpleKMeans[i + 1].getClusterCentroids().get(currState);
				finalInstance += toAdd.value(0) * maxAt[i + 1][0]; 
			}
			if (pathProb > maxPathProb) {
				maxPathProb = pathProb;
				maxPredictSize = finalInstance;
			}
		}
		return maxPredictSize;
	}
	
	public Instance doPrediction(Instance instance, int timeStep) throws Exception {
		double avgPredict = getFinalClusterDistribution(instance, timeStep);
		Instance prediction = new DenseInstance(1);
		prediction.setValue(0, avgPredict);
		return prediction;
	}
	
	public Result getTrainingMSE() throws Exception {
		System.out.print("Reading first location ... ");
		DataSource dataSourceAt0 = new DataSource(locations[0]);
		Instances instancesAt0 = dataSourceAt0.getDataSet();
		System.out.println("[DONE]");
		System.out.print("Reading end location ... ");
		DataSource dataSourceAtEnd = new DataSource(locations[locations.length - 1]);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		System.out.println("[DONE]");
		return getMSE(instancesAt0, instancesAtEnd, 0);		
	}
	
	protected Result getMSE(Instances instancesAt0, Instances absInstancesAtEnd, int timeStep) throws Exception {
		System.out.print("Computing MSE ... ");
		long start = System.currentTimeMillis();
		Result result = new Result();
		int countAttr = instancesAt0.numAttributes();
		result.rmse = new double[countAttr];
		result.clusterPredictionAccuracy = 0;
		int count = 0;
		for (int i = 0; i < instancesAt0.size(); i++) {
			Instance instanceAt0 = instancesAt0.get(i);
			Instance instanceAtEnd = absInstancesAtEnd.get(i);
			Instance prediction = doPrediction(instanceAt0, timeStep);
			if (prediction != null) {
				computeDiffSquare(result.rmse, instanceAtEnd, prediction);
				int actualCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(instanceAtEnd);
				int predictedCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(prediction);
				if (actualCluster == predictedCluster) {
					result.clusterPredictionAccuracy++;
				}
				count++;
			}
		}
		for (int i = 0; i < result.rmse.length; i++) {
			result.rmse[i] /= count;
			result.rmse[i] = Math.sqrt(result.rmse[i]);
		}
		result.attributes = new String[absInstancesAtEnd.numAttributes()];
		for (int i = 0; i < result.attributes.length; i++) {
			result.attributes[i] = absInstancesAtEnd.attribute(i).name();
		}
		result.clusterPredictionAccuracy = (result.clusterPredictionAccuracy / count);
		long end = System.currentTimeMillis();
		System.out.println("[DONE] (" + ((end - start) / 1000) + "secs)");
		return result;
	}
	
	public Result getMSE(Instances instancesAt0, Instances absInstancesAt0, Instances absInstancesAtEnd, int timeStep) throws Exception {
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
			Instance absInstanceAt0 = absInstancesAt0.get(i);
			Instance instanceAtEnd = absInstancesAtEnd.get(i);
			Instance prediction = doPrediction(instanceAt0, timeStep);
			if (prediction != null) {
				prediction.setValue(0, (prediction.value(0) + absInstanceAt0.value(0)));
				computeDiffSquare(result.rmse, prediction, instanceAtEnd);
				relativeError(result.re, instanceAtEnd, prediction);
//				int actualCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(instanceAtEnd);
//				int predictedCluster = simpleKMeans[simpleKMeans.length - 1].clusterInstance(prediction);
//				if (actualCluster == predictedCluster) {
//					result.clusterPredictionAccuracy++;
//				}
				count++;
			}
		}
		for (int i = 0; i < result.rmse.length; i++) {
			result.re[i] /= count;
			result.rmse[i] /= count;
			result.rmse[i] = Math.sqrt(result.rmse[i]);
		}
		result.attributes = new String[absInstancesAtEnd.numAttributes()];
		for (int i = 0; i < result.attributes.length; i++) {
			result.attributes[i] = absInstancesAtEnd.attribute(i).name();
		}
		result.clusterPredictionAccuracy = (result.clusterPredictionAccuracy / count);
		long end = System.currentTimeMillis();
		System.out.println("[DONE] (" + ((end - start) / 1000) + "secs)");
		return result;
	}
	
	public Result getTestMSE(String locationAt0, String locationAtEnd, int timeStep) throws Exception {
		DataSource dataSourceAt0 = new DataSource(locationAt0);
		Instances instancesAt0 = dataSourceAt0.getDataSet();
		DataSource dataSourceAtEnd = new DataSource(locationAtEnd);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		return getMSE(instancesAt0, instancesAtEnd, timeStep);
	}
	
	public Result getTestMSE(String locationAt0, String absLocationAt0, String absLocationAtEnd, int timeStep) throws Exception {
		DataSource dataSourceAt0 = new DataSource(locationAt0);
		Instances instancesAt0 = dataSourceAt0.getDataSet();
		DataSource absDataSourceAt0 = new DataSource(absLocationAt0);
		Instances absInstancesAt0 = absDataSourceAt0.getDataSet();
		DataSource dataSourceAtEnd = new DataSource(absLocationAtEnd);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		return getMSE(instancesAt0, absInstancesAt0, instancesAtEnd, timeStep);
	}
	
	public void displayClusterStdDevsForLastLayer() {
		for (Instance instance : simpleKMeans[simpleKMeans.length - 1].getClusterStandardDevs()) {
			System.out.println(instance);
		}		
	}
	
	public void displayClusterCentroidsForLastLayer() {
		for (Instance instance : simpleKMeans[simpleKMeans.length - 1].getClusterCentroids()) {
			System.out.println(instance);
		}
	}
	
	public void displayClusterSizesAtLastLayer() {
		System.out.println(Arrays.toString(simpleKMeans[simpleKMeans.length - 1].getClusterSizes()));
	}
	
	protected void computeDiffSquare(double[] out, Instance in1, Instance in2) {
		for (int i = 0; i < 1; i++) {
			double diff = (in1.value(i) - in2.value(i));
			out[i] = out[i] + diff * diff;
		}		
	}
	
	protected void relativeError(double[] out, Instance target, Instance prediction) {
		for (int i = 0; i < 1; i++) {
			double xt = target.value(i);
			double xp = prediction.value(i);
			if ((xp - xt) != 0 && xt != 0) {
				double re = Math.abs((xp - xt)/xt);
//				System.out.println("|" + xp + " - " +  xt + " / " + xt + "| = " + re);
				out[i] = out[i] + re;
			}
		}		
	}
	
	protected void setClusteringParameters(int timeSteps, int numClusters, int seed) throws Exception {
		this.simpleKMeans = new SimpleKMeans[timeSteps];
		for (int i = 0; i < simpleKMeans.length; i++) {
			simpleKMeans[i] = new SimpleKMeans();
			simpleKMeans[i].setNumClusters(numClusters);
			simpleKMeans[i].setInitializationMethod(new SelectedTag(SimpleKMeans.CANOPY, SimpleKMeans.TAGS_SELECTION));
			simpleKMeans[i].setPreserveInstancesOrder(true);
			simpleKMeans[i].setSeed(seed);
			simpleKMeans[i].setDisplayStdDevs(true);
//			((NormalizableDistance) simpleKMeans[i].getDistanceFunction()).setDontNormalize(true);
		}
	}
	
	protected void doClustering(String location, int numClusters, AbstractClusterer clusterer) throws Exception {
		DataSource dataSource = new DataSource(location);
		Instances instances = dataSource.getDataSet();
		clusterer.buildClusterer(instances);
	}
	
	public void doClusteringAtEachTimeSteps(int numClusters) throws Exception {
		for (int i = 0; i < locations.length; i++) {
			System.out.print("Running clustering on " + locations[i] + " ... ");
			long start = System.currentTimeMillis();
			doClustering(locations[i], numClusters, simpleKMeans[i]);
			long end = System.currentTimeMillis();
			System.out.println("[DONE] (" + (Math.round(end - start) / 1000.0) + " secs)");
		}
	}
	
	public void build() throws Exception {
		int timeSteps = simpleKMeans.length;
		
		mm = new LinkedList<>();
		int[] clusterSizesAt0 = simpleKMeans[0].getClusterSizes();
		int total = 0;
		for (int i = 0; i < clusterSizesAt0.length; i++)
			total += clusterSizesAt0[i];

		// only cluster states at time t = 0 will have initial state probability
		// all others have to be set to 0
		System.out.print("Setting initial state probabilities ... ");
		initialStateProbabilities = new double[clusterSizesAt0.length];
		for (int i = 0; i < initialStateProbabilities.length; i++) {
			initialStateProbabilities[i] = 	((double) clusterSizesAt0[i]) / total;
		}
		System.out.println("[DONE]");
		
		System.out.print("Setting transition probabilities ... ");
		for (int i = 0; i < timeSteps - 1; i++) {
			// Compute all transitions from i to i+1
			int[] labelsAtT1 = simpleKMeans[i].getAssignments();
			int[] labelsAtT2 = simpleKMeans[i + 1].getAssignments();
			
			int numClustersAti = simpleKMeans[i].getNumClusters();
			int numClustersAtip1 = simpleKMeans[i+1].getNumClusters();
			double[][] transition = new double[numClustersAti][numClustersAtip1];
			for (int inst = 0; inst < labelsAtT1.length; inst++) {
				int j = labelsAtT1[inst];
				int k = labelsAtT2[inst];
				transition[j][k]++;
			}
			int[] clusterSizes = simpleKMeans[i].getClusterSizes();
			for (int j = 0; j < numClustersAti; j++) {
				for (int k = 0; k < numClustersAtip1; k++) {
					transition[j][k] /= clusterSizes[j];
				}
			}
			mm.add(transition);
		}
		System.out.println("[DONE]");
	}	
	
	public void displayModel() {
		for (double[][] transition : mm) {
			System.out.println("===");
			for (int i = 0; i < transition.length; i++) {
				System.out.println(Arrays.toString(transition[i]));
			}
		}
	}

}