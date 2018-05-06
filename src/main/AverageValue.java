package main;

public class AverageValue {
	private float average;
	private int counts;
	
	public AverageValue(float bias, int strength) {
		average = bias;
		counts = strength;
	}
	
	public void updateAverage(float number) {
		float tempAve = average * counts;
		float tempSum = tempAve + number;

		counts = counts + 1;
		average = tempSum / counts;
		
		if (counts > Integer.MAX_VALUE - 55555) counts = 555;
	}
	
	public float getAverage() {
		return average;
	}
}