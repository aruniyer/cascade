package cascade.popcorn;

import java.io.Serializable;

import weka.core.Instance;

public interface IMarkovModel extends Serializable {
	
	public void doClusteringAtEachTimeSteps(int numClusters) throws Exception;
	
	public void build() throws Exception;
	
	public Instance doPrediction(Instance instanceAtT1, int T1) throws Exception;
	
	public Result getTrainingMSE() throws Exception;
	
	public Result getTestMSE(String locationAtT1, String locationAtEnd, int T1) throws Exception;
	
	public void displayModel();
	
	public void displayClusterSizesAtLastLayer();
	
}
