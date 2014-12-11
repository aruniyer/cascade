package cascade.popcorn;

import weka.core.DenseInstance;
import weka.core.Instance;

public class PartialMarkovKMeansAveraged extends PartialMarkovKMeansMax {

	private static final long serialVersionUID = 1L;
	
	public PartialMarkovKMeansAveraged(String[] locations, int numClusters, int[] clusteringAttributes, int seed) throws Exception {
		super(locations, numClusters, clusteringAttributes, seed);
	}
	
	public Instance doPrediction(Instance instance, int timeStep) throws Exception {
		double[] distribution = getFinalClusterDistribution(instance, timeStep);
		if (remove != null) {
			loadFullCentroids();
		}
		
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
	
}