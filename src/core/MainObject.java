package core;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainObject {
	public String[] allCommands; //An array of 3 strings with 0 being tiny, and 2 being heavy.
	public int recheckFrequency; //Will recheck every "recheckFrequency" * "sleepTime".
	public int sleepTime;
	
	public int currentMode;
	public int sleepCounter;
	public int lastAverage;
	public boolean checkType;
	public List<TimeSlot> timeSlots;

	public Process minerProcess;
	public AverageValue cumulativeHashRate;

	
	public MainObject() {
		allCommands = new String[3];
		recheckFrequency = 20;
		sleepTime = 15;
		
		currentMode = 0;
		sleepCounter = 0;
		
		cumulativeHashRate = new AverageValue(0, 0);
	}
	
	public void initialize(String tiny, String light, String heavy, int frequency, int time, int mode, boolean type, List<TimeSlot> slots) {
		if (tiny.isEmpty() || light.isEmpty() || heavy.isEmpty()) {
			System.err.println("Config file is missing one of the 3 required miner commands (tiny, light, or heavy).");
			System.exit(-2);
		}
		
		allCommands[0] = tiny;
		allCommands[1] = light;
		allCommands[2] = heavy;
		recheckFrequency = frequency;
		sleepTime = time;
		checkType = type;
		timeSlots = slots;
		
		currentMode = mode;
		minerProcess = Main.runCommand(allCommands[currentMode]);
	}
	
	public void switchTiny() {
		if (currentMode != 0 && !allCommands[currentMode].equals(allCommands[0])) {
			System.out.println("{ STATUS } Switching to tiny miner!");
	
	        if (Main.beepLevel > 2) {
	        	Main.playSound(700, 150, 100);
	        	Main.playSound(700, 150, 100);
	        	Main.playSound(700, 150, 100);            	
	        }
	
			currentMode = 0;						
			Main.consumer.shouldStop = true;
			minerProcess.destroy();
			
			minerProcess = Main.runCommand(allCommands[0]);
		}
	}
	
	public void switchLight() {
		if (currentMode != 1 && !allCommands[currentMode].equals(allCommands[1])) {
			System.out.println("{ STATUS } Switching to light miner!");
	
	        if (Main.beepLevel > 2) {
	        	Main.playSound(500, 150, 100);
	        	Main.playSound(500, 150, 100);
	        	Main.playSound(500, 150, 100);            	
	        }
	
			currentMode = 1;
			Main.consumer.shouldStop = true;
			minerProcess.destroy();
			
			minerProcess = Main.runCommand(allCommands[1]);
		}
	}
	
	public void switchHeavy() {
		if (currentMode != 2 && !allCommands[currentMode].equals(allCommands[2])) {
			System.out.println("{ STATUS } Switching to heavy miner!");
	
	        if (Main.beepLevel > 2) {
	        	Main.playSound(150, 150, 100);
	        	Main.playSound(150, 150, 100);
	        	Main.playSound(150, 150, 100);            	
	        }
			
			currentMode = 2;
			Main.consumer.shouldStop = true;						
			minerProcess.destroy();
		
			minerProcess = Main.runCommand(allCommands[2]);
		}
	}
	
	public void recheckActivity(GlobalListener ex) {
		int count = ex.takeCount();
		lastAverage = ex.getAverage();
		
		if (count < recheckFrequency) { //Less than 1 input every 15 seconds is considered AFK or close enough.
			this.switchHeavy();
		} else if (count < lastAverage) { //Less than the average is considered light use.
			this.switchLight();
		} else { //Anything above the average is considered heavy use of the computer and so the miner is turned down.
			this.switchTiny();
		}
	}
	
	public void recheckTime(GlobalListener ex) {
		if (ex.isMovieMode()) {
			this.switchTiny();
		} else {
			TimeStore ts = new TimeStore(Main.getTheTime());
			boolean slotFound = false;
			
			for (TimeSlot slot : this.timeSlots) {
				if (slot.matches(ts)) {
					if (slot.getType().equals("tiny")) {
						this.switchTiny();
					} else if (slot.getType().equals("light")) {
						this.switchLight();
					} else if (slot.getType().equals("heavy")) {
						this.switchHeavy();
					} else {
						System.err.println("Incorrect slot value slipped past config check: " + slot.getType());
						System.exit(-1);
					}
					
					slotFound = true;
					break;
				}
			}		
			
			if (!slotFound) {
				this.switchLight();
			}
		}
	}
	
	public void runLogic(GlobalListener ex) throws InterruptedException {
		System.out.println("{ " + Main.modeStrings[currentMode] + " " + sleepCounter + "/" + recheckFrequency + " } - " + Main.consumer.currentDescription());
		cumulativeHashRate.updateAverage(Main.consumer.getAverageRate());
		TimeUnit.SECONDS.sleep(sleepTime);
		sleepCounter++;
		
		if (Main.forceCheck || sleepCounter >= recheckFrequency) {
			Main.forceCheck = false;
			sleepCounter = 0;		
			
			if (this.checkType) {
				this.recheckActivity(ex);				
			} else {
				this.recheckTime(ex);
			}
		}
	}
	
	public void destroy() {
		this.minerProcess.destroy();
		
		System.out.println("SpareMiner stopped with stats:");
		System.out.println("\tAverage hash rate: " + cumulativeHashRate.getAverage());
		
		if (this.checkType) {
			System.out.println("\tAverage interactions: " + lastAverage);			
		}
	}
}
