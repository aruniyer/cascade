package com.yahoo.evolution.popcorn;

import weka.core.Instance;

public class DiffMarkovKMeansAveraged extends DiffMarkovKMeansMax {

	public DiffMarkovKMeansAveraged(String[] locations, String[] max, int numClusters, int seed) throws Exception {
		super(locations, max, numClusters, seed);
	}

	private static final long serialVersionUID = 1L;
	
	protected double getFinalClusterDistribution(Instance instance, int timeStep) throws Exception {
		int timeSteps = simpleKMeans.length;
		int label = simpleKMeans[timeStep].clusterInstance(instance);
		int prevState = label;

		double averagePrediction = 0;
		int numSimulations = 1000;
		for (int simulations = 0; simulations < numSimulations; simulations++) {
			Double finalInstance = new Double(0);
//			double pathProb = 0;
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
//				pathProb += Math.log(edgeProb);
				prevState = currState;
				Instance toAdd = simpleKMeans[i + 1].getClusterCentroids().get(currState);
				finalInstance += toAdd.value(0) * maxAt[i + 1][0]; 
			}
			averagePrediction += finalInstance;
		}
		return averagePrediction / numSimulations;
	}

}
