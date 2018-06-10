package core;

public class TimeStore {
	private int hours;
	private int minutes;
	
	public TimeStore(String time) {
		String[] parts = time.split(":");
		
		try {
			hours = Integer.parseInt(parts[0].trim());
			minutes = Integer.parseInt(parts[1].trim());
		} catch (Exception e) {
			System.err.println("Issue occurred while parsing time: " + time);
			hours = 0;
			minutes = 0;
		}
	}
	
	public boolean isNextTime(TimeStore ts) {
		if (ts.hours > this.hours) {
			return true;
		} else if (ts.hours == this.hours) {
			return ts.minutes > this.minutes;
		}
		
		return false;
	}
}