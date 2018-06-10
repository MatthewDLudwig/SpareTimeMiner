package core;

public class TimeSlot {
	private String type;
	private TimeStore start;
	private TimeStore end;
	
	public TimeSlot(String t, TimeStore s, TimeStore e) {
		this.type = t.toLowerCase();
		this.start = s;
		this.end = e;
	}
	
	public boolean matches(TimeStore t) {
		if (this.type.equals(type)) {
			if (this.start.isNextTime(t)) {
				if (t.isNextTime(this.end)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public String getType() {
		return this.type;
	}
}