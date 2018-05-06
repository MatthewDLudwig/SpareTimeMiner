package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

public class Main {
	public static boolean running = true;
	public static boolean forceCheck = false;
	public static int beepLevel = 2;
	
	private static Process minerProcess;
	private static StreamConsumer consumer;
	
	public static void playSound(float hz, float time, float volume) throws LineUnavailableException {
		float frequency = 44100;
		byte[] buf = new byte[1];
	    AudioFormat af = new AudioFormat(frequency, 8, 1, true, false);
	    SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
	    
	    sdl.open();
	    sdl.start();
	    for (int i = 0; i < 250 * frequency / 1000; i++) {
	        double angle = i / (frequency / hz) * 2.0 * Math.PI;
	        buf[0] = (byte) (Math.sin(angle) * volume);

	        sdl.write(buf, 0, 1);
	    }
	    sdl.drain();
	    sdl.stop();
	}
	
	
	public static void runCommand(Runtime r, String command) throws IOException, InterruptedException {
		TimeUnit.SECONDS.sleep(5);
		minerProcess = r.exec(command);
		
		consumer = new StreamConsumer(minerProcess.getInputStream());
		consumer.setDaemon(true);
		consumer.start();
	}
	
	public static void main(String[] args) throws LineUnavailableException {	  
        playSound(500, 150, 0);

		String[] modeStrings = { "Tiny", "Light", "Heavy" };
		
		String tinyCommand = "", lightCommand = "", heavyCommand = "";
		int recheckFrequency = 20;
		int[] comboExit = {3, 6, 9};
		int[] comboMovie = {7, 8, 9};
		int[] comboForce = {1, 2, 3};
		
		int currentMode = 0;
		int interactionBias = 60;
		int biasStrength = 5;
		int lastAverage = 0;
		AverageValue cumulativeHashRate = new AverageValue(0, 0);
		
		try {
			File configFile = new File("SpareMiner.config");
			BufferedReader configReader = new BufferedReader(new FileReader(configFile));
			String configLine = null;

            while ( (configLine = configReader.readLine()) != null) {
            	String[] parts = configLine.split("=:=");
            	if (parts.length != 2) {
            		System.err.println("Too many parts present after splitting along '=:='.  Please ensure you only use '=:=' between the key and value.");
            		throw new IOException();
            	} else {
            		String key = parts[0].trim();
            		String value = parts[1].trim();
            		
            		if (key.equals("beeps")) {
            			if (value.equals("0") || value.equalsIgnoreCase("none")) {
            				Main.beepLevel = 0;
            			} else if (value.equals("1") || value.equalsIgnoreCase("onoff")) {
            				Main.beepLevel = 1;
            			} else if (value.equals("2") || value.equalsIgnoreCase("some")) {
            				Main.beepLevel = 2;
            			} else if (value.equals("3") || value.equalsIgnoreCase("all")) {
            				Main.beepLevel = 3;
            			} else {
            				System.err.println("Unrecognized value for key 'beeps': " + value);
            				throw new IOException();
            			}
            		} else if (key.equals("tiny")) {
            			tinyCommand = value;
            		} else if (key.equals("light")) {
            			lightCommand = value;
            		} else if (key.equals("heavy")) {
            			heavyCommand = value;
            		} else if (key.equals("frequency")) {
            			recheckFrequency = Integer.valueOf(value);
            		} else if (key.equals("exit") || key.equals("movie") || key.equals("force")) {
            			String[] pieces = value.split(",");
            			int[] theArr = new int[pieces.length];
            			for (int i = 0; i < pieces.length; i++) {
            				String piece = pieces[i].trim();
            				Integer val = Integer.parseInt(piece);
            				theArr[i] = val;
            			}
            			
            			if (key.equals("exit")) {
            				comboExit = theArr;            				
            			} else if (key.equals("movie")) {
                			comboMovie = theArr;
            			} else if (key.equals("force")) {
                			comboForce = theArr;
            			}
            		} else if (key.equals("initial")) {
            			if (value.equals("0") || value.equalsIgnoreCase("tiny")) {
            				currentMode = 0;
            			} else if (value.equals("1") || value.equalsIgnoreCase("light")) {
            				currentMode = 1;
            			} else if (value.equals("2") || value.equalsIgnoreCase("heavy")) {
            				currentMode = 2;
            			} else {
            				System.err.println("Unrecognized value for key 'initial': " + value);
            				throw new IOException();
            			}
            		} else if (key.equals("bias")) {
            			String[] pieces = value.split(",");
            			
            			if (pieces.length != 2) {
                    		System.err.println("Incorrect format for key 'bias': Format is 'bias,strength'");
                    		throw new IOException();
                    	} else {
                    		interactionBias = Integer.parseInt(pieces[0].trim());
                    		biasStrength = Integer.parseInt(pieces[1].trim());
                    	}
            		} else {
            			System.err.println("Unrecognized key: '" + key + "'");
            		}
            	}
            }
            
            configReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("No config file found, please ensure that SpareMiner.config is in the same folder as the jar file.");
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("A problem occurred while reading the config file!");
			System.exit(-2);
		}
		
		if (tinyCommand.isEmpty() || lightCommand.isEmpty() || heavyCommand.isEmpty()) {
			System.err.println("Config file is missing one of the 3 required miner commands (tiny, light, or heavy).");
			System.exit(-2);
		}
				
		try {
			Runtime r = Runtime.getRuntime();
			Process findMiner;

		    if (System.getProperty("os.name").startsWith("Windows")) {
			    findMiner = r.exec("tasklist.exe");
		    } else {
		    	findMiner = r.exec("ps -aux");
		    }
            BufferedReader minerReader = new BufferedReader(new InputStreamReader(findMiner.getInputStream()));
            String minerLine = null;
            while ( (minerLine = minerReader.readLine()) != null) {
            	if (minerLine.contains("miner")) {
            		System.err.println("The Nimiq miner is most likely already running!");
            		System.err.println("If this is a mistake, please change your config file.");
            		System.exit(-3);
            	}
            }

            minerReader.close();
            
            if (Main.beepLevel > 0) {
            	playSound(400, 150, 100);
            	playSound(500, 150, 100);
            	playSound(600, 150, 100);            	
            }

			Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
			logger.setUseParentHandlers(false);
			logger.setLevel(Level.WARNING);
			runCommand(r, currentMode == 0 ? tinyCommand : currentMode == 1 ? lightCommand : heavyCommand);
			
			GlobalScreen.registerNativeHook();
			GlobalListener ex = new GlobalListener(interactionBias, biasStrength, comboExit, comboMovie, comboForce);
			GlobalScreen.addNativeKeyListener(ex);
			GlobalScreen.addNativeMouseListener(ex);

			int sleepCounter = 0;
			while (Main.running) {
				System.out.println("{ " + modeStrings[currentMode] + " " + sleepCounter + "/" + recheckFrequency + " } - " + consumer.currentDescription());
				cumulativeHashRate.updateAverage(consumer.getAverageRate());
				TimeUnit.SECONDS.sleep(15);
				sleepCounter++;
				
				if (forceCheck || sleepCounter >= recheckFrequency) {
					forceCheck = false;
					sleepCounter = 0;
					
					int count = ex.takeCount();
					lastAverage = ex.getAverage();
					
					if (count < recheckFrequency) { //Less than 1 input every 15 seconds is considered AFK or close enough.
						if (currentMode != 2) {
							System.out.println("{ STATUS } Switching to heavy miner!");

				            if (Main.beepLevel > 2) {
				            	playSound(150, 150, 100);
				            	playSound(150, 150, 100);
				            	playSound(150, 150, 100);            	
				            }
							
							currentMode = 2;
							consumer.shouldStop = true;						
							minerProcess.destroy();
							
							runCommand(r, heavyCommand);
						}
					} else if (count < (lastAverage * 3) / 4) { //Less than 3/4 of average is considered light use.
						if (currentMode != 1) {
							System.out.println("{ STATUS } Switching to light miner!");

				            if (Main.beepLevel > 2) {
				            	playSound(500, 150, 100);
				            	playSound(500, 150, 100);
				            	playSound(500, 150, 100);            	
				            }

							currentMode = 1;
							consumer.shouldStop = true;
							minerProcess.destroy();
							
							runCommand(r, lightCommand);
						}
					} else { //Anything above 3/4 of the average is considered heavy use of the computer and so the miner is turned down.
						if (currentMode != 0) {
							System.out.println("{ STATUS } Switching to tiny miner!");

				            if (Main.beepLevel > 2) {
				            	playSound(700, 150, 100);
				            	playSound(700, 150, 100);
				            	playSound(700, 150, 100);            	
				            }

							currentMode = 0;						
							consumer.shouldStop = true;
							minerProcess.destroy();
							
							runCommand(r, tinyCommand);
						}
					}
				}
			}
			
			GlobalScreen.unregisterNativeHook();
			consumer.shouldStop = true;
			minerProcess.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NativeHookException e) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(e.getMessage());
		}
		
		System.out.println("SpareMiner stopped with stats:");
		System.out.println("\tAverage interactions: " + lastAverage);
		System.out.println("\tAverage hash rate: " + cumulativeHashRate.getAverage());

		if (Main.beepLevel > 0) {
			playSound(600, 150, 100);
			playSound(500, 150, 100);
			playSound(400, 150, 100);			
		}
	}
}
