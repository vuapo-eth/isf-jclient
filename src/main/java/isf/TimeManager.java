package isf;

import java.util.ArrayList;

public class TimeManager extends Thread {

	private final static ArrayList<Task> TASKS = new ArrayList<Task>();
	private final static TimeManager TM = new TimeManager();
	
	public TimeManager() {
		start();
	}
	
	@Override
	public void run() {
		long minTimeDif = 0;
		while (true) {
			
			synchronized (TM) {
				try {
					TM.wait(minTimeDif == 0 ? 2000 : Math.min(10000, minTimeDif));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			minTimeDif = 0;
			
			for(int i = 0; i < TASKS.size(); i++) {
				long timeDif = TASKS.get(i).getNextCallTimeStamp() - System.currentTimeMillis();
				if(timeDif <= 0)
					TASKS.get(i).call();
				else
					minTimeDif = minTimeDif == 0 ? timeDif : Math.min(minTimeDif, timeDif);
			}
		}
	}
	
	public static void addTask(Task t) {
		TASKS.add(t);
		
		synchronized (TM) {
			TM.notify();
		}
	}
}

abstract class Task {
	
	private long nextCallTimeStamp;
	private final long interval;
	
	Task(long interval, boolean callImmediately) {
		this.interval = interval;
		if(callImmediately) call();
		nextCallTimeStamp = System.currentTimeMillis() + interval;
	}
	
	abstract void onCall();
	
	long getNextCallTimeStamp() {
		return nextCallTimeStamp;
	}
	
	void call() {
		nextCallTimeStamp += interval;
		
		new Thread() {
			public void run() {
				onCall();
			};
		}.start();
	}
}