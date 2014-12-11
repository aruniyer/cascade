package com.yahoo.evolution.popcorn;

class Result {
	
	double[] rmse;
	double[] re;
	double clusterPredictionAccuracy;
	String[] attributes;
	
	public String toString() {
		StringBuilder display = new StringBuilder("Cluster Prediction Accuracy = ");
		display.append(clusterPredictionAccuracy).append(System.lineSeparator());
		display.append("MSE:").append(System.lineSeparator());
		for (int i = 0; i < rmse.length; i++) {
			display.append(attributes[i]).append(" = ").append(rmse[i]).append(System.lineSeparator());
		}
		return display.toString();
	}
	
	public void display() {
		System.out.println(this);
	}
	
	public void displayClusterAccuracy() {
		System.out.println("Cluster Prediction Accuracy = " + clusterPredictionAccuracy);
	}

}
