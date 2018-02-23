package isf;

import java.text.DecimalFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import iota.GoldDiggerLocalPoW;
import isf.ui.UIManager;

public class LogThread extends Thread {
	
	private static final UIManager uim = new UIManager("LogThrd");
			
	private double priceUsd = 0;
	private long timeStarted;
	private int balance = 0, currentReward = 0;
	
	@Override
	public void run() {

		timeStarted = System.currentTimeMillis();
		
		int logInterval = Configs.getInt(P.LOG_INTERVAL);
		uim.logDbg("starting log thread, logs will appear in " + logInterval + "s intervals");
		
		TimeManager.addTask(new Task(1800000, true) { @Override void onCall() { updateIotaTicker(); } });
		TimeManager.addTask(new Task(120000, true) { @Override void onCall() { updateBalance(); } });
		TimeManager.addTask(new Task(120000, true) { @Override void onCall() { updateRemoteControl(); } });
		TimeManager.addTask(new Task(logInterval * 1000, true) { @Override void onCall() { log(); } });
	}
	
	private void updateRemoteControl() {
		JSONObject obj = APIManager.requestCommand();
		
		if(obj.getBoolean("pause") && !SpamThread.isPaused())
			uim.logWrn("spamming paused remotely by iotaspam.com: " + obj.getString("message"));
		else if(!obj.getBoolean("pause") && SpamThread.isPaused())
			uim.logWrn("spamming restarted remotely by iotaspam.com");
		
		SpamThread.setPaused(obj.getBoolean("pause")); // TODO 1 -> boolean
	}
	
	private void updateBalance() {
		JSONObject objBalance = APIManager.requestBalance();
		balance = objBalance.getInt("balance");
		currentReward = objBalance.getInt("reward");
	}
	
	private void log() {

		if(SpamThread.isPaused()) return;
		
		long timeRunning = getTimeRunning();
		int confirmedSpam = getConfirmedSpam(), totalSpam = AddressManager.getTailsTotalTxs(-1);

		DecimalFormat df = new DecimalFormat("##0.00");
		DecimalFormat df2 = new DecimalFormat("#00.00");
		DecimalFormat dfInt = new DecimalFormat("###,###,##0");
		
		int sec = (int)(timeRunning/1000);
		int min = sec/60;
		int hour = min/60;
		int day = hour/24;

		String timeString = day + ":" + (hour%24 < 10 ? "0" : "") + hour%24
				+ ":" + (min%60 < 10 ? "0" : "") + min%60
				+ ":" + (sec%60 < 10 ? "0" : "") + sec%60;
		
		double miotaPerMonth = (30*24*60*60000.0*SpamThread.getTotalTxs()/timeRunning)*currentReward/1e6*AddressManager.getTailsConfirmRate(15)/100;
		
		String speed = "SPEED " + UIManager.padLeft(df.format(getSpamSpeed()), 5) + " txs/min | ";
		if(SpamThread.isPaused())
			speed = "> REMOTELY PAUSED < | ";
		
		uim.logInf("TIME " + UIManager.padLeft(timeString + " | ", 13)
		+ "SPAM " + UIManager.padLeft(getTotalSpam()+"", 7) + " txs | "
		+  speed
		+ "CNFMD " + UIManager.padLeft(confirmedSpam
				+ (totalSpam < 1000 ? UIManager.padRight("/"+totalSpam, 4) : "")
				+ " txs ("+df2.format(getConfirmationRate())+"%)", 20) + " | "
		+ "BLNCE " + dfInt.format(balance) + "i" + (priceUsd > 0 ? " ($"+df.format(balance/1e6*priceUsd)+")": "") + " | "
		+ "EST. RWRD  " + df.format(miotaPerMonth) + " Mi" + (priceUsd > 0 ? " ($"+df.format(miotaPerMonth*priceUsd)+")": "") + " per month"
		+ " | NODES " + UIManager.padLeft(NodeManager.getAmountOfAvailableAPIs() + "/" + NodeManager.getAmountOfAPIs() + "[@"+NodeManager.getApiIndex()+"]", 10)
		+ " | POW/GTTA " + df2.format(GoldDiggerLocalPoW.getAvgPoWTime()) + "s/"+df2.format(NodeManager.getAvgTxsToApproveTime())+"s ("+
			(GttaThread.gttarsQueueSize() == 0 ? UIManager.ANSI_RED : "")+GttaThread.gttarsQueueSize()+UIManager.ANSI_RESET+"/"+GttaThread.gttarsLimit()+")");
	}

	private void updateIotaTicker() {
		DecimalFormat df = new DecimalFormat("###,##0.00"), dfInt = new DecimalFormat("###,##0");
		
		String jsonString = APIManager.request(APIManager.CMC_API_IOTA, null);
		
		try {
			JSONObject obj = new JSONArray(jsonString).getJSONObject(0);
			
			priceUsd = obj.getDouble("price_usd");
			double change24h = obj.getDouble("percent_change_24h"), mcap = obj.getDouble("market_cap_usd")/1e9;
			int priceSatoshi = (int) (100000000*obj.getDouble("price_btc"));
			
			String s = UIManager.ANSI_BOLD+"IOTA TICKER:     " + UIManager.ANSI_RESET;
			
			s += "$"+df.format(priceUsd)+"/Mi ("
						+(change24h<0?UIManager.ANSI_RED:UIManager.ANSI_GREEN+"+")
						+ df.format(change24h)+"%"+UIManager.ANSI_RESET+" in 24h)     ";
			s += dfInt.format(priceSatoshi) + " sat/Mi     ";
			s += "MCAP: $"+df.format(mcap)+"B (#"+obj.getInt("rank")+")";
			
			uim.logInf(s);
		} catch (JSONException e) {
			uim.logDbg(jsonString);
			uim.logException(e, false);
			return;
		}
	}
	
	public double getConfirmationRate() {
		return AddressManager.getTailsConfirmRate(15);
	}
	
	public int getConfirmedSpam() {
		return AddressManager.getTailsConfirmedTxs(-1);
	}
	
	public int getTimeRunning() {
		return (int) (System.currentTimeMillis() - timeStarted);
	}
	
	public int getTotalSpam() {
		return SpamThread.getTotalTxs() + AddressManager.getPreSessionTransactions();
	}
	
	public double getSpamSpeed() {
		int timeRunning = getTimeRunning();
		return timeRunning > 0 ? 60000.0*SpamThread.getTotalTxs()/timeRunning : 0;
	}
}
