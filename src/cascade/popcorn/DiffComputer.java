package cascade.popcorn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class DiffComputer {

	private static void computeDiff(String sourceAtT1, String sourceAtT2, String outputFile) throws Exception {
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
		BufferedReader readerT1 = new BufferedReader(new FileReader(sourceAtT1));
		BufferedReader readerT2 = new BufferedReader(new FileReader(sourceAtT2));
		String lineT1 = readerT1.readLine(), lineT2 = readerT2.readLine(); // skip header
		bufferedWriter.write(lineT1);
		for (int i = 0; i < 4; i++) {
			bufferedWriter.write(",d" + i);
		}
		bufferedWriter.newLine();
		while (((lineT1 = readerT1.readLine()) != null) && ((lineT2 = readerT2.readLine()) != null)) {
			String[] cols1 = lineT1.split(",");
			String[] cols2 = lineT2.split(",");
			// write the absolutes
			for (int i = 0; i < cols2.length; i++) {
				double v2 = Double.parseDouble(cols2[i]);
				if (i == 0)
					bufferedWriter.write("" + v2);
				else
					bufferedWriter.write("," + v2);
			}
			
			// followed by the differences
			for (int i = 0; i < 4; i++) {
				double v1 = Double.parseDouble(cols1[i]);
				double v2 = Double.parseDouble(cols2[i]);
				bufferedWriter.write("," + (v2 - v1));
			}
			bufferedWriter.newLine();
		}
		bufferedWriter.close();
		readerT1.close();
		readerT2.close();
	}
	
	public static void main(String[] args) throws Exception {
		String[] trainLocations = GlobalParameters.FileParameters.trainLocations;
		String[] diffTrainLocations = GlobalParameters.DiffFileParameters.trainLocations;
		for (int i = 1; i < trainLocations.length; i++) {
			computeDiff(trainLocations[i - 1], trainLocations[i], diffTrainLocations[i]);
		}
		
		String[] testLocations = GlobalParameters.FileParameters.testLocations;
		String[] diffTestLocations = GlobalParameters.DiffFileParameters.testLocations;
		for (int i = 1; i < trainLocations.length; i++) {
			computeDiff(testLocations[i - 1], testLocations[i], diffTestLocations[i]);
		}
	}
}
