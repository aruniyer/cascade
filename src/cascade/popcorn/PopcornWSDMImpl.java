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
	
	public static void main(String[] args) throws Exception {
		int seed = 10;
		int numClusters = 8;
		Class<PartialMarkovKMeansMax> clazz = PartialMarkovKMeansMax.class;
		String modelFile = "20141217_n" + numClusters + "_" + clazz.getSimpleName();
		modelFile += ".model";
		System.out.println("Building the model ... ");
		String[] trainLocations = GlobalParameters.WeinerFileParameters.trainLocations;
		String[] testLocations = GlobalParameters.WeinerFileParameters.testLocations;
		String[] testLocationsAbs = GlobalParameters.WeinerFileParameters.testLocations;
		
		buildModel(modelFile, clazz.getName(), trainLocations, numClusters, new int[]{0}, seed);
		IMarkovModel markovModel = readModel(modelFile);
		for (int i = 0; i < testLocations.length - 1; i++) {
		    String outputFile = "popcorn_results_n" + numClusters + "_" + clazz.getSimpleName();
	        outputFile += "_" + i + ".txt";
	        System.out.println(outputFile);
	        BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile));
			System.out.println("At t = " + i);
			Result result = markovModel.getTestMSE(testLocations[i], testLocationsAbs[testLocations.length - 1], i, outputWriter);
		    outputWriter.close();
		    System.out.println("t = " + i + ", " + Arrays.toString(result.re) + " :: " + Arrays.toString(result.rmse));
		}		
	}
	
}