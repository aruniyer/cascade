package cascade.popcorn;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;

public class PopcornWSDMImpl {

	public static void buildModel(String modelFile, String className, String[] locations, int numClusters, int[] clusteringAttributes, int seed) throws Exception {
		@SuppressWarnings("unchecked")
		Class<IMarkovModel> clazz = (Class<IMarkovModel>) Class.forName(className);
		IMarkovModel markovModel;
		if (className.contains("Full")) {
			Constructor<IMarkovModel> constructor = clazz.getConstructor(String[].class, int.class, int.class);
			markovModel = constructor.newInstance(locations, numClusters, seed);
		} else if (className.contains("Partial")) {
			Constructor<IMarkovModel> constructor = clazz.getConstructor(String[].class, int.class, int[].class, int.class);
			markovModel = constructor.newInstance(locations, numClusters, clusteringAttributes, seed);
		} else {
			Constructor<IMarkovModel> constructor = clazz.getConstructor(String[].class);
			markovModel = constructor.newInstance((Object) locations);
		}
		markovModel.doClusteringAtEachTimeSteps(numClusters);
		markovModel.build();
		FileOutputStream fileOutputStream = new FileOutputStream(modelFile);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		objectOutputStream.writeObject(markovModel);
		objectOutputStream.close();
	}
	
	public static IMarkovModel readModel(String modelFile) throws Exception {
		FileInputStream fileInputStream = new FileInputStream(modelFile);
		ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
		IMarkovModel markovModel = (IMarkovModel) objectInputStream.readObject();
		objectInputStream.close();
		return markovModel;
	}
	
	public static void getTrainingAccuracy(String modelFile) throws Exception {
		IMarkovModel markovModel = readModel(modelFile);
		System.out.println(markovModel.getTrainingMSE());
	}
	
	public static void getTestingAccuracy(String locationAt1, String locationAtEnd, int timeStep, String modelFile) throws Exception {
		IMarkovModel markovModel = readModel(modelFile);
		System.out.println(markovModel.getTestMSE(locationAt1, locationAtEnd, timeStep));
	}
	
	public static void main1(int numClusters, Class<? extends IMarkovModel> clazz, int[] clusteringAttributes) throws Exception {
		int seed = 10;
		String modelFile = "20141104_n" + numClusters + "_" + clazz.getSimpleName();
		if (clusteringAttributes != null)
			modelFile += Arrays.toString(clusteringAttributes);
		modelFile += ".model";
		System.out.println("Building the model ... ");
		String[] trainLocations = GlobalParameters.SizeFileParameters.trainLocations;
//		String[] testLocations = GlobalParameters.FileParameters.testLocations;
		buildModel(modelFile, clazz.getName(), trainLocations, numClusters, clusteringAttributes, seed);
		System.out.println("Reading the model ... ");
		IMarkovModel markovModel = readModel(modelFile);
		String outputFile = "popcorn_results_n" + numClusters + "_" + clazz.getSimpleName();
		if (clusteringAttributes != null)
			outputFile = outputFile + Arrays.toString(clusteringAttributes);
		outputFile += ".txt";
		System.out.println(outputFile);
//		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
//		for (int i = 0; i < testLocations.length - 1; i++) {
//			System.out.println("At t = " + i);
//			Result result = markovModel.getTestMSE(testLocations[i], testLocations[testLocations.length - 1], i);
//			outputWriter.write("At t = " + Math.pow(2, i+1));
//			for (int j = 0; j < result.rmse.length; j++) {
//				outputWriter.write("," + result.rmse[j]);
//			}
//			for (int j = 0; j < result.re.length; j++) {
//				outputWriter.write("," + result.re[j]);
//			}
//			outputWriter.newLine();
//			outputWriter.flush();
//		}		
//		outputWriter.close();
		markovModel.displayModel();
//		markovModel.displayClusterSizesAtLastLayer();
//		
//		((PartialMarkovKMeansAveraged) markovModel).displayClusterStdDevsForLastLayer();
//		((PartialMarkovKMeansAveraged) markovModel).displayClusterCentroidsForLastLayer();
	}
	
	public static void main(String[] args) throws Exception {
		int[] clusters = {2};
		int[] clusteringAttributes = new int[]{0};
		for (int nc : clusters) {
			main1(nc, PartialMarkovKMeansMax.class, clusteringAttributes);
//			main2(nc);
		}
	}
	
	public static void main2(int numClusters) throws Exception {
		int seed = 10;
		Class<DiffMarkovKMeansAveraged> clazz = DiffMarkovKMeansAveraged.class;
//		String modelFile = "20141104_n" + numClusters + "_" + clazz.getSimpleName();
//		modelFile += ".model";
		System.out.println("Building the model ... ");
		String[] trainLocations = GlobalParameters.SAFileParameters.trainLocations;
		String[] testLocations = GlobalParameters.SAFileParameters.testLocations;
		String[] testLocationsAbs = GlobalParameters.FileParameters.testLocations;
		
		Constructor<DiffMarkovKMeansAveraged> constructor = clazz.getConstructor(String[].class, String[].class, int.class, int.class);
		DiffMarkovKMeansAveraged markovModel = constructor.newInstance(trainLocations, null, numClusters, seed);
		
		markovModel.doClusteringAtEachTimeSteps(numClusters);
		markovModel.build();
		
		String outputFile = "popcorn_results_n" + numClusters + "_" + clazz.getSimpleName();
		outputFile += ".txt";
		System.out.println(outputFile);
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
		for (int i = 0; i < testLocations.length - 1; i++) {
			System.out.println("At t = " + i);
			Result result = markovModel.getTestMSE(testLocations[i], testLocationsAbs[i], testLocationsAbs[testLocations.length - 1], i);
			outputWriter.write("At t = " + Math.pow(2, i+1));
			for (int j = 0; j < result.rmse.length; j++) {
				outputWriter.write("," + result.rmse[j]);
			}
			for (int j = 0; j < result.rmse.length; j++) {
				outputWriter.write("," + result.re[j]);
			}
			outputWriter.newLine();
			outputWriter.flush();
		}		
		outputWriter.close();
//		markovModel.displayModel();
//		markovModel.displayClusterSizesAtLastLayer();
//		
//		((PartialMarkovKMeansAveraged) markovModel).displayClusterStdDevsForLastLayer();
//		((PartialMarkovKMeansAveraged) markovModel).displayClusterCentroidsForLastLayer();
	}
	
}