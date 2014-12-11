package com.yahoo.evolution.popcorn;

import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class FullMarkovKMeansAveraged extends FullMarkovKMeansMax {

	private static final long serialVersionUID = 1L;

	public FullMarkovKMeansAveraged(String[] locations, int numClusters, int seed) throws Exception {
		super(locations, numClusters, seed);
	}
	
	public Instance doPrediction(Instance instance, int timeStep) throws Exception {
		double[] distribution = getFinalClusterDistribution(instance, timeStep);
		Instance averagedInstance = new DenseInstance(instance);
		Instances centroidInstances = simpleKMeans[simpleKMeans.length - 1].getClusterCentroids(); 
		for (int i = 0; i < distribution.length; i++) {
			Instance centroid = centroidInstances.get(i);
			double[] vals = new double[centroid.numAttributes()];
			for (int j = 0; j < instance.numAttributes(); j++) {
				vals[j] = vals[j] + distribution[i]*centroid.value(j);
			}
			for (int j = 0; j < instance.numAttributes(); j++) {
				averagedInstance.setValue(j, vals[j]);
			}
		}
		return averagedInstance;
	}

}