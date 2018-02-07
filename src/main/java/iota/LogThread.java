package iota;

import java.text.DecimalFormat;

import iota.ui.UIManager;

public class LogThread extends Thread {
	
	private static final UIManager uim = new UIManager("LogThrd");
			
	@Override
	public void run() {
	
		if(UIManager.isDebugEnabled()) uim.logDbg("starting log thread, logs will appear in " + Configs.log_interval + "s intervals");
		else uim.logInf("starting log thread, logs will appear in " + Configs.log_interval + "s intervals");
		
		long lastCommandRequest = 0;
		long timeStarted = System.currentTimeMillis();
		int counter = 0;
		double iotaprice = SpamFundAPI.getIotaPrice();
		
		while(true) {
			long timeNow = System.currentTimeMillis();
			long timeRunning = timeNow-timeStarted;
			
			DecimalFormat df = new DecimalFormat("##0.00");
			
			int sec = (int)(timeRunning/1000);
			int min = sec/60;
			int hour = min/60;
			int day = hour/24;

			String timeString = day + ":" + (hour%24 < 10 ? "0" : "") + hour%24
					+ ":" + (min%60 < 10 ? "0" : "") + min%60
					+ ":" + (sec%60 < 10 ? "0" : "") + sec%60;
			
			double miotaPerMonth = (30*24*60*60000.0*SpamThread.getTotalTxs()/timeRunning)*0.000015*AddressManager.getTailsConfirmRate(15)/100;
			
			uim.logInf("TIME " + UIManager.padLeft(timeString + " | ", 13)
			+ "SPAM " + UIManager.padLeft((SpamThread.getTotalTxs() + AddressManager.getPreSessionTransactions()) + " txs | ", 14)
			+ "SPEED " + UIManager.padLeft((timeRunning > 0 ? df.format(60000.0*SpamThread.getTotalTxs()/timeRunning).replaceAll("\\,", ".") + "" : "--") + " txs/min | ", 16)
			+ "CNFMD " + UIManager.padLeft(AddressManager.getTailsConfirmedTxs(-1)
					+ (AddressManager.getTailsConfirmedTxs(-1) < 100 ? "/" + AddressManager.getTailsTotalTxs(-1) : "")
					+ " txs ("+df.format(AddressManager.getTailsConfirmRate(15))+"%)", 20) + " | "
			+ "EST. RWRD  " + df.format(miotaPerMonth) + " Mi" + (iotaprice > 0 ? " ($"+df.format(miotaPerMonth*iotaprice)+")": "") + " per month");
			
			if(System.currentTimeMillis()-lastCommandRequest > 120000)  {
				SpamThread.setCommand(SpamFundAPI.requestCommand());
				if(lastCommandRequest > 0 && (counter+=(System.currentTimeMillis()-lastCommandRequest)/60000) > 30) {
					counter -= 30;
					double newIotaprice = SpamFundAPI.getIotaPrice();
					if(newIotaprice > 0) iotaprice = newIotaprice;
				}
				lastCommandRequest = System.currentTimeMillis();
			}
			
			try { sleep(Configs.log_interval * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
