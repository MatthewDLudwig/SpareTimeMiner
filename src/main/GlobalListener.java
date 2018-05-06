package main;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;

public class GlobalListener implements NativeKeyListener, NativeMouseInputListener {
	private int counter;
	private AverageValue average;
	
	private boolean movieMode;
	private StateMachine exitTracker;
	private StateMachine movieTracker;
	private StateMachine forceTracker;
	
	public GlobalListener(int bias, int strength, int[] exit, int[] movie, int[] force) {
		counter = 0;
		average = new AverageValue(bias, strength);
		movieMode = false;
		exitTracker = new StateMachine(exit);
		movieTracker = new StateMachine(movie);
		forceTracker = new StateMachine(force);
	}
		
	public int takeCount() {
		if (movieMode) {
			counter = 0;
			return (int) average.getAverage();
		} else if (counter == 0) {
			return 0;
		} 
		
		average.updateAverage(counter);
		int tempCount = counter;
		counter = 0;
		
		return tempCount;
	}
	
	public int getAverage() {
		return (int) average.getAverage();
	}
	
	public void nativeKeyPressed(NativeKeyEvent e) {
		String theKey = NativeKeyEvent.getKeyText(e.getKeyCode());
		int number = 0;
		
		if (theKey.length() > 1 && theKey.charAt(0) == 'F') {
			number = theKey.charAt(1) - '0';
			
			if (theKey.length() == 3) {
				number = (number * 10) + (theKey.charAt(2) - '0');
			}
		}
		
		if (number != 0) {
			if (this.exitTracker.checkCurrent(number)) {
				Main.running = false;
			}
			
			if (this.movieTracker.checkCurrent(number)) {
				this.movieMode = !this.movieMode;
			}
			
			if (this.forceTracker.checkCurrent(number)) {
				Main.forceCheck = true;
			}
		}
		
		counter++;
	}

	public void nativeMouseMoved(NativeMouseEvent e) {
		counter++;
	}
	
	public void nativeMouseClicked(NativeMouseEvent e) {
		counter++;
	}

	public void nativeMousePressed(NativeMouseEvent e) { }
	public void nativeMouseReleased(NativeMouseEvent e) { }
	public void nativeMouseDragged(NativeMouseEvent e) { }
	public void nativeKeyReleased(NativeKeyEvent e) { }
	public void nativeKeyTyped(NativeKeyEvent e) { }
}