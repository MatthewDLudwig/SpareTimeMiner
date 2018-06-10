package core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
	public static String[] modeStrings = { "Tiny", "Light", "Heavy" };
	
	public static Runtime globalRuntime;
	public static SimpleDateFormat globalTimeFormat;
	
	public static boolean running = true;
	public static boolean forceCheck = false;
	public static int beepLevel = 2;
	
	public static StreamConsumer consumer;
	
	public static void playSound(float hz, float time, float volume) {
		try {
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
		} catch(LineUnavailableException e) {
			System.err.println("Unable to play beeps, line unavailable!");
		}
	}
	
	public static void checkForMiner() throws IOException {
		Process findMiner;

	    if (System.getProperty("os.name").startsWith("Windows")) {
		    findMiner = Main.globalRuntime.exec("tasklist.exe");
	    } else {
	    	findMiner = Main.globalRuntime.exec("ps -aux");
	    }
        BufferedReader minerReader = new BufferedReader(new InputStreamReader(findMiner.getInputStream()));
        String minerLine = null;
        while ( (minerLine = minerReader.readLine()) != null) {
        	if (minerLine.contains("miner")) {
        		System.out.println(minerLine);
        		System.err.println("The Nimiq miner is most likely already running!");
        		System.err.println("If this is a mistake, please change your config file.");
        		System.exit(-3);
        	}
        }

        minerReader.close();
	}
	
	public static Process runCommand(String command) {
		Process p = null;
		
		try {
			TimeUnit.SECONDS.sleep(5);
			p = Main.globalRuntime.exec(command);
			
			consumer = new StreamConsumer(p.getInputStream());
			consumer.setDaemon(true);
			consumer.start();
		} catch (Exception e) {
			System.err.println("Failed to run the command: \"" + command + "\"");
			System.exit(-1);
		}
		
		return p;
	}
	
	public static String getTheTime() {
		return Main.globalTimeFormat.format(new Date());
	}
	
	public static void main(String[] args) {	
		Main.globalRuntime = Runtime.getRuntime();
		Main.globalTimeFormat = new SimpleDateFormat("HH:mm");

		int[] comboExit = {3, 6, 9};
		int[] comboMovie = {7, 8, 9};
		int[] comboForce = {1, 2, 3};
		int interactionBias = 60;
		int biasStrength = 5;
		
		String tinyCommand = "", lightCommand = "", heavyCommand = "";
		int recheckFrequency = 20;
		int counterTime = 15;	
		int startingMode = 0;
		boolean activityMode = true;
		List<TimeSlot> slots = new LinkedList<TimeSlot>();
		
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
            				startingMode = 0;
            			} else if (value.equals("1") || value.equalsIgnoreCase("light")) {
            				startingMode = 1;
            			} else if (value.equals("2") || value.equalsIgnoreCase("heavy")) {
            				startingMode = 2;
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
            		} else if (key.equals("counter")) {
                		counterTime = Integer.parseInt(value);
            		} else if (key.equals("check")) {
            			if (value.equalsIgnoreCase("activity")) {
            				activityMode = true;
            			} else if (value.equalsIgnoreCase("time")) {
            				activityMode = false;
             			} else {
             				System.err.println("Unrecognized value for key 'check': " + value);
             				throw new IOException();
             			}            		
            		} else if (key.equals("times")) {
            			String[] pieces = value.split(",");
            			
            			for (int i = 0; i < pieces.length; i++) {
            				String[] times = pieces[i].split("-");
            				
            				if (times[0].equalsIgnoreCase("tiny") || times[0].equalsIgnoreCase("light") || times[0].equalsIgnoreCase("heavy")) {
            					TimeStore start = new TimeStore(times[1]);
            					TimeStore end = new TimeStore(times[2]);
            					
            					if (!start.isNextTime(end)) {
            						System.err.println("Non-sequential times given for: " + pieces[i]);
            						throw new IOException();
            					} else {
            						slots.add(new TimeSlot(times[0], start, end));
            					}            					
            				} else {
        						System.err.println("Invalid time slot name: " + times[0]);
        						throw new IOException();
            				}
            			}
            			
            			System.out.println("# of time slots found: " + slots.size());
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
		
        Main.playSound(500, 150, 0);
        
        MainObject obj = new MainObject();
        
		try {            
	        Main.checkForMiner();
	        obj.initialize(tinyCommand, lightCommand, heavyCommand, recheckFrequency, counterTime, startingMode, activityMode, slots);
	        
            if (Main.beepLevel > 0) {
            	playSound(400, 150, 100);
            	playSound(500, 150, 100);
            	playSound(600, 150, 100);            	
            }

			Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
			logger.setUseParentHandlers(false);
			logger.setLevel(Level.WARNING);
			
			GlobalScreen.registerNativeHook();
			GlobalListener ex = new GlobalListener(interactionBias, biasStrength, comboExit, comboMovie, comboForce);
			GlobalScreen.addNativeKeyListener(ex);
			GlobalScreen.addNativeMouseListener(ex);

			while (Main.running) {
				obj.runLogic(ex);
			}
			
			GlobalScreen.unregisterNativeHook();
			consumer.shouldStop = true;
			obj.destroy();
		} catch (NativeHookException e) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (Main.beepLevel > 0) {
			playSound(600, 150, 100);
			playSound(500, 150, 100);
			playSound(400, 150, 100);			
		}
	}
}
