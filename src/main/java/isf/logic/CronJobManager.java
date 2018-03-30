package isf.logic;

import java.util.ArrayList;

public class CronJobManager extends Thread {

	private final static ArrayList<CronJob> CRON_JOBS = new ArrayList<CronJob>();
	private final static CronJobManager TM = new CronJobManager();
	
	public CronJobManager() {
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
			
			for(int i = 0; i < CRON_JOBS.size(); i++) {
				long timeDif = CRON_JOBS.get(i).getNextCallTimeStamp() - System.currentTimeMillis();
				if(timeDif <= 0)
					CRON_JOBS.get(i).call();
				else
					minTimeDif = minTimeDif == 0 ? timeDif : Math.min(minTimeDif, timeDif);
			}
		}
	}
	
	public static void addCronJob(CronJob t) {
		CRON_JOBS.add(t);
		
		synchronized (TM) {
			TM.notify();
		}
	}
}