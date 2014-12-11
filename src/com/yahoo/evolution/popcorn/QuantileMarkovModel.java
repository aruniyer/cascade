package com.yahoo.evolution.popcorn;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class QuantileMarkovModel implements IMarkovModel {
	
	private static final long serialVersionUID = 1L;
	protected String[] locations;
	protected DescriptiveStatistics[] layerStatistics;
	protected int[][] labels;
	protected List<double[][]> mm;
	protected double[] initialStateProbabilities;
	protected double[] partialCentroids;
	protected int numQuantiles;
	protected double[][] quantiles;
	
	public QuantileMarkovModel(String[] locations) throws Exception {
		this.locations = locations;
	}
	
	private DescriptiveStatistics computeStatistics(String location) throws Exception {
		DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
		DataSource dataSource = new DataSource(location);
		Instances dataSet = dataSource.getDataSet();
		for (Instance instance : dataSet) {
			double size = instance.value(0);
			descriptiveStatistics.addValue(size);
		}
		return descriptiveStatistics;
	}
	
	private void setQuantiles() {
		this.quantiles = new double[layerStatistics.length][numQuantiles - 1];
		for (int i = 0; i < layerStatistics.length; i++) {
			double quantileGap = 100.0 / numQuantiles;
			int label = 0;
			for (double top = quantileGap; top < 100; top+=quantileGap, label++) {
				double quantile = layerStatistics[i].getPercentile(top);
				this.quantiles[i][label] = quantile;
			}
		}
	}
	
	private void setLabels(String location, int timeStep) throws Exception {
		DataSource dataSource = new DataSource(location);
		Instances dataSet = dataSource.getDataSet();
		int[] labels = new int[dataSet.size()];
		for (int i = 0; i < labels.length; i++) {
			Instance instance = dataSet.get(i);
			labels[i] = getLabel(instance, timeStep);
		}
		this.labels[timeStep] = labels;
	}
	
	public void doClusteringAtEachTimeSteps(int numClusters) throws Exception {
		this.numQuantiles = numClusters;
		this.layerStatistics = new DescriptiveStatistics[locations.length];
		this.labels = new int[locations.length][];
		for (int i = 0; i < locations.length; i++) {
			String location = locations[i];
			System.out.print("Computing statistics for " + location + " ... ");
			long start = System.currentTimeMillis();
			this.layerStatistics[i] = computeStatistics(location);
			long end = System.currentTimeMillis();
			System.out.println("[DONE] (" + ((end - start) / 1000) + " secs)");
		}		
		for (int i = 0; i < locations.length; i++) {
			String location = locations[i];
			System.out.print("Computing quantiles for " + location + " ... ");
			long start = System.currentTimeMillis();
			setQuantiles();
			long end = System.currentTimeMillis();
			System.out.println("[DONE] (" + ((end - start) / 1000) + " secs)");
			System.out.print("Computing labels for " + location + " ... ");
			start = System.currentTimeMillis();
			setLabels(location, i);
			end = System.currentTimeMillis();
			System.out.println("[DONE] (" + ((end - start) / 1000) + " secs)");
		}
		this.partialCentroids = new double[numClusters];
		DescriptiveStatistics descriptiveStatistics = layerStatistics[locations.length - 1];
		double prevQuantile = descriptiveStatistics.getMin();
		int label = 0;
		for (; label < this.quantiles[locations.length - 1].length; label++) {
			double quantile = this.quantiles[locations.length - 1][label];
			this.partialCentroids[label] = (prevQuantile + quantile) / 2;
			prevQuantile = quantile;
		}
		this.partialCentroids[label] = (prevQuantile + descriptiveStatistics.getMax()) / 2;
	}
	
	private int getLabel(Instance instance, int timeStep) {
		double size = instance.value(0);
		int label = 0;
		for (; label < quantiles[timeStep].length; label++) {
			double quantile = this.quantiles[timeStep][label];
			if (size <= quantile) {
				break;
			}
		}
		return label;
	}
	
	public void build() throws Exception {
		int timeSteps = layerStatistics.length;
		
		mm = new LinkedList<>();
		int[] clusterSizesAt0 = new int[numQuantiles];
		Arrays.fill(clusterSizesAt0, 1);
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
			int numClustersAti = numQuantiles;
			int numClustersAtip1 = numQuantiles;
			double[][] transition = new double[numClustersAti][numClustersAtip1];
			for (int inst = 0; inst < labels[i].length; inst++) {
				int j = labels[i][inst];
				int k = labels[i+1][inst];
				transition[j][k]++;
			}
			for (int j = 0; j < transition.length; j++) {
				double sum = 0;
				for (int k = 0; k < transition.length; k++) {
					sum = sum + transition[j][k];
				}
				for (int k = 0; k < transition.length; k++) {
					transition[j][k] /= sum;
				}
			}
			mm.add(transition);
		}
		System.out.println("[DONE]");
	}
	
	protected double[] getFinalClusterDistribution(Instance instance, int timeStep) throws Exception {
		int timeSteps = locations.length;
		int label = getLabel(instance, timeStep);
		System.out.print("Quantile At Start = " + label + " :: ");
		int prevState = label;
		double[] finalClusterDistribution = new double[numQuantiles];
		for (int simulations = 0; simulations < 1000; simulations++) {
			for (int i = timeStep; i < timeSteps - 1; i++) {
				double[] cdf = new double[numQuantiles];
				double[][] transition = mm.get(i);
				cdf[0] = transition[prevState][0];
				for (int j = 1; j < cdf.length; j++) {
					cdf[j] = cdf[j - 1] + transition[prevState][j];
				}
				System.out.println(Arrays.toString(cdf));
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
		System.out.println(Arrays.toString(finalClusterDistribution));
		return finalClusterDistribution;
	}
	
	protected Instance[] centroids;
	protected void loadFullCentroids() throws Exception {
		if (centroids != null)
			return;
		DataSource dataSourceAtEnd = new DataSource(locations[locations.length - 1]);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		centroids = new Instance[numQuantiles];
		double[] minDistances = new double[centroids.length];
		Arrays.fill(minDistances, Double.MAX_VALUE);
		for (int i = 0; i < instancesAtEnd.numInstances(); i++) {
			Instance instance = instancesAtEnd.get(i);
			double size = instance.value(0);
			int label = getLabel(instance, locations.length - 1);
			double dist = Math.abs(partialCentroids[label] - size);
			if (dist < minDistances[label]) {
				minDistances[label] = dist;
				centroids[label] = instance;
			}
		}
	}
	
	public Instance doPrediction(Instance instance, int timeStep) throws Exception {
		double[] distribution = getFinalClusterDistribution(instance, timeStep);
		loadFullCentroids();
		Instance averagedInstance = new DenseInstance(instance);
		for (int i = 0; i < distribution.length; i++) {
			double[] vals = new double[instance.numAttributes()];
			for (int j = 0; j < instance.numAttributes(); j++) {
				vals[j] = vals[j] + distribution[i]*centroids[i].value(j);
			}
			for (int j = 0; j < instance.numAttributes(); j++) {
				averagedInstance.setValue(j, vals[j]);
			}
		}
		return averagedInstance;
	}
	
	public void displayModel() {
		for (double[][] transition : mm) {
			System.out.println("===");
			for (int i = 0; i < transition.length; i++) {
				System.out.println(Arrays.toString(transition[i]));
			}
		}
	}

	@Override
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
	
	protected Result getMSE(Instances instancesAt0, Instances instancesAtEnd, int timeStep) throws Exception {
		System.out.print("Computing MSE ... ");
		long start = System.currentTimeMillis();
		Result result = new Result();
		int countAttr = instancesAt0.numAttributes();
		result.rmse = new double[countAttr];
		result.clusterPredictionAccuracy = 0;
		int count = 0;
		for (int i = 0; i < instancesAt0.size(); i++) {
			Instance instanceAt0 = instancesAt0.get(i);
			Instance instanceAtEnd = instancesAtEnd.get(i);
			Instance prediction = doPrediction(instanceAt0, timeStep);
			if (prediction != null) {
				computeDiffSquare(result.rmse, prediction, instanceAtEnd);
				int actualCluster = getLabel(instanceAtEnd, locations.length - 1);
				int predictedCluster = getLabel(prediction, locations.length - 1);
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
		result.attributes = new String[instancesAtEnd.numAttributes()];
		for (int i = 0; i < result.attributes.length; i++) {
			result.attributes[i] = instancesAtEnd.attribute(i).name();
		}
		result.clusterPredictionAccuracy = (result.clusterPredictionAccuracy / count);
		long end = System.currentTimeMillis();
		System.out.println("[DONE] (" + ((end - start) / 1000) + "secs)");
		return result;
	}
	
	protected void computeDiffSquare(double[] out, Instance in1, Instance in2) {
		for (int i = 0; i < in1.numAttributes(); i++) {
			double diff = (in1.value(i) - in2.value(i));
			out[i] = out[i] + diff * diff;
		}		
	}

	@Override
	public Result getTestMSE(String locationAt0, String locationAtEnd, int timeStep) throws Exception {
		DataSource dataSourceAt0 = new DataSource(locationAt0);
		Instances instancesAt0 = dataSourceAt0.getDataSet();
		DataSource dataSourceAtEnd = new DataSource(locationAtEnd);
		Instances instancesAtEnd = dataSourceAtEnd.getDataSet();
		return getMSE(instancesAt0, instancesAtEnd, timeStep);
	}

	@Override
	public void displayClusterSizesAtLastLayer() {
		// TODO Auto-generated method stub
		
	}

}
