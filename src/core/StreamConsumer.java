package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class StreamConsumer extends Thread {
	public boolean shouldStop;
    private InputStream is;
    
    private AverageValue averageRate;
    private float currentBalance;
    private int blocksPassed;
    private int currentBlock;

    // reads everything from "is" until empty. 
    StreamConsumer(InputStream is) {
        this.is = is;
		shouldStop = false;
		averageRate = new AverageValue(0, 0);
		currentBalance = 0;
		blocksPassed = 0;
		currentBlock = 0;
    }
    
    public String currentDescription() {
    	return String.format("Average Hashrate: %f H/s, Balance: %f NIM, Blocks Passed: %d", averageRate.getAverage(), currentBalance, blocksPassed);
    }
    
    public float getAverageRate() {
    	return averageRate.getAverage();
    }

    public void run() {
    	String hashCheck = "Hashrate: ";
    	String balanceCheck = "Balance: ";
    	String blockCheck = "Now at block: ";
    	
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
            	int hashRate = line.indexOf(hashCheck);
            	int balance = line.indexOf(balanceCheck);
            	int block = line.indexOf(blockCheck);

            	if (hashRate != -1) {
            		int endIndex = line.indexOf(' ', hashRate + hashCheck.length());
            		String part = line.substring(hashRate + hashCheck.length(), endIndex == -1 ? line.length() - 1 : endIndex);
            		
            		try {
            			float value = Float.parseFloat(part);
            			averageRate.updateAverage(value);   
            		} catch (NumberFormatException e) { }    			
            	}
            	
            	if (balance != -1) {
            		int endIndex = line.indexOf(' ', balance + balanceCheck.length());
            		String part = line.substring(balance + balanceCheck.length(), endIndex == -1 ? line.length() - 1 : endIndex);
            		try {
	            		float value = Float.parseFloat(part);
	            		currentBalance = value;
            		} catch (NumberFormatException e) { }
            	}
            	
            	if (block != -1) {
            		int endIndex = line.indexOf(' ', block + blockCheck.length());
            		String part = line.substring(block + blockCheck.length(), endIndex == -1 ? line.length() - 1 : endIndex);
            		
            		try {
	            		int value = Integer.parseInt(part);
	            		if (currentBlock == 0) {
	            			currentBlock = value;
	            		} else if (currentBlock != value) {
	            			blocksPassed += (value - currentBlock);
	            			currentBlock = value;
	            		}
            		} catch (NumberFormatException e) { }
            	}

                if (shouldStop) {
                	break;
                }
            }
            
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}