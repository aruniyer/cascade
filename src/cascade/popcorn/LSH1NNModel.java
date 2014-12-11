package cascade.popcorn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class LSH1NNModel {
	
	public Result do1NearestNeighbour(String trainLocation0, String trainLocationE, String testLocation0, String testLocationE) throws Exception {
		DataSource dataSourceAt0 = new DataSource(trainLocation0);
		Instances instancesAt0 = dataSourceAt0.getDataSet();
		DataSource dataSourceAtE = new DataSource(trainLocationE);
		Instances instancesAtE = dataSourceAtE.getDataSet();
		DataSource testSourceAt0 = new DataSource(testLocation0);
		Instances testInstancesAt0 = testSourceAt0.getDataSet();
		DataSource testSourceAtE = new DataSource(testLocation0);
		Instances testInstancesAtE = testSourceAtE.getDataSet();
		
		EuclideanDistance distance = new EuclideanDistance(instancesAt0);
		
		Result result = new Result();
		result.rmse = new double[testInstancesAt0.numAttributes()];
		
		Random random = new Random(1);
		double[][] hashFunctions = new double[16][instancesAt0.numAttributes()];
		for (int i = 0; i < hashFunctions.length; i++) {
			for (int j = 0; j < hashFunctions[0].length; j++) {
				hashFunctions[i][j] = random.nextGaussian();
			}
		}
		
		for (int i = 0; i < instancesAt0.numInstances(); i++) {
			Instance instance = instancesAt0.get(i);
			BitSet bucket = getBucket(hashFunctions, instance);
			List<Integer> instanceList = lshBucket.get(bucket);
			if (instanceList == null) {
				instanceList = new LinkedList<>();
				lshBucket.put(bucket, instanceList);
			}
			instanceList.add(i);
		}
		
		int limit = testInstancesAt0.numInstances();
		long start = System.currentTimeMillis();
		for (int i = 0; i < limit; i++) {
			if (i % 1000 == 0) {
				long end = System.currentTimeMillis();
				System.out.print(i + " (" + ((end - start)/1000) + " secs) ... ");
			}
			Instance testInstanceAt0 = testInstancesAt0.get(i);
			BitSet bucket = getBucket(hashFunctions, testInstanceAt0);
			List<Integer> instanceList = lshBucket.get(bucket);
			double minDist = Double.MAX_VALUE;
			int nearestNeighbour = -1;
			if (instanceList != null) {
				for (int j : instanceList) {
					double dist = distance.distance(testInstanceAt0, instancesAt0.get(j));
					if (dist < minDist) {
						minDist = dist;
						nearestNeighbour = j;
					}
				}
			} else {
				for (int j = 0; j < instancesAt0.numInstances(); j++) {
					double dist = distance.distance(testInstanceAt0, instancesAt0.get(j));
					if (dist < minDist) {
						minDist = dist;
						nearestNeighbour = j;
					}
				}
			}
			Instance prediction = instancesAtE.get(nearestNeighbour);
			Instance target = testInstancesAtE.get(i);
			for (int j = 0; j < testInstanceAt0.numAttributes(); j++) {
				double diff = target.value(j) - prediction.value(j);
				result.rmse[j] = result.rmse[j] + diff * diff;
			}
		}
		for (int i = 0; i < result.rmse.length; i++) {
			result.rmse[i] = Math.sqrt(result.rmse[i] / limit);
		}
		System.out.println();
		System.out.println(Arrays.toString(result.rmse));
		return result;
	}
	
	private HashMap<BitSet, List<Integer>> lshBucket = new HashMap<>();
	
	private BitSet getBucket(double[][] hashFunctions, Instance instance) {
		BitSet bitSet = new BitSet(hashFunctions.length);
		for (int i = 0; i < hashFunctions.length; i++) {
			double sum = 0;
			for (int j = 0; j < instance.numAttributes(); j++) {
				sum += instance.value(j) * hashFunctions[i][j];
			}
			if (sum >= 0) {
				bitSet.set(i);
			}
		}
		return bitSet;
	}
	
	public static void main(String[] args) throws Exception {
		String[] trainLocations = GlobalParameters.FileParameters.trainLocations;
		String[] testLocations = GlobalParameters.FileParameters.testLocations;
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter("popcorn_results" + "_" + LSH1NNModel.class.getSimpleName() + ".txt"));
		for (int i = 0; i < GlobalParameters.FileParameters.testLocations.length - 1; i++) {
			System.out.println("At t = " + i);
			LSH1NNModel model = new LSH1NNModel();
			Result result = model.do1NearestNeighbour(trainLocations[i], trainLocations[trainLocations.length - 1], testLocations[i], testLocations[testLocations.length - 1]);
			outputWriter.write("At t = " + Math.pow(2, i+1));
			for (int j = 0; j < result.rmse.length; j++) {
				outputWriter.write("," + result.rmse[j]);
			}
			outputWriter.newLine();
			outputWriter.flush();
		}
		outputWriter.close();
	}

}
