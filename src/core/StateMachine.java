package main;

class StateMachine {
	private int[] order;
	private int current;
	
    public StateMachine(int[] o) {
        order = o;
        current = 0;
    }

    public boolean checkCurrent(int i) {
    	if (order[current] == i) {
    		current++;
    	} else {
    		current = 0;
    	}
    	
    	if (current == order.length) {
    		current = 0;
    		return true;
    	} else {
    		return false;
    	}
    }
}