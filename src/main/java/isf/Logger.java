package isf;

import java.text.DecimalFormat;

import iota.PearlDiver;
import isf.logic.CronJob;
import isf.logic.CronJobManager;
import isf.logic.TimeAbortCall;
import isf.spam.AddressManager;
import isf.spam.NodeManager;
import isf.spam.SpamThread;
import isf.spam.TipPool;
import isf.ui.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import iota.GOldDiggerLocalPoW;
import isf.ui.UIManager;

public class Logger {
	
	private static final UIManager uim = new UIManager("Logger");
    private static final DecimalFormat DF = new DecimalFormat("##0.00"), DF2 = new DecimalFormat("#00.0"), INT = new DecimalFormat("##0");
    private static final int MINUTES_PER_MONTH = 30*24*60;

    private static double priceUsd = 0;
    private static int balance = 0;
    private static double iotaPerTx = 0;

	public static void init() {

		final int logInterval = Configs.getInt(P.LOG_INTERVAL);
		final int performanceReportInterval = Configs.getInt(P.LOG_PERFORMANCE_REPORT_INTERVAL);
		uim.logDbg(String.format(R.STR.getString("log_starting"), logInterval));

		if(Main.isInOnlineMode()) {
            updateBalance();
            CronJobManager.addCronJob(new CronJob(120000, false, false) { @Override public void onCall() { updateBalance(); } });

            updateIotaTicker();
            CronJobManager.addCronJob(new CronJob(1800000, false, false) { @Override public void onCall() { updateIotaTicker(); } });
        }

        CronJobManager.addCronJob(new CronJob(logInterval * 1000, true, false) { @Override public void onCall() { log(); } });
		CronJobManager.addCronJob(new CronJob(performanceReportInterval * 1000, false, false) { @Override public void onCall() { performanceReport(); } });
	}

	private static void updateBalance() {
		final JSONObject objBalance = APIManager.requestBalance();
		balance = objBalance.getInt("balance");
        iotaPerTx = objBalance.getDouble("reward");
	}

	private static void log() {

		if(SpamThread.isPaused()) return;

        final long timeRunning = SpamThread.getTimeRunning();
        final int confirmedSpam = getConfirmedSpam();
        final int totalSpam = AddressManager.getTailsTotalTxs(-1);
		final double txsPerMinute = SpamThread.getSpamSpeed();
		final double confirmationRate = getConfirmationRate();
		final double miotaPerMonth = MINUTES_PER_MONTH*txsPerMinute*iotaPerTx*confirmationRate/1e6;

		String balanceString;
        if(balance >= 1e6) balanceString = DF.format(balance/1e6) + " Mi";
        else if(balance >= 1e4) balanceString = (int)(balance/1e3) + " Ki";
        else balanceString = balance + " i";
        balanceString = UIManager.padLeft(balanceString, 6) + " ($"+DF.format(balance/1e6*priceUsd)+")";

		final String logTime = UIManager.padLeft(formatInterval(timeRunning), 10);
        final String logSpam = UIManager.padLeft(SpamThread.getTotalSpam()+"", 7);
        final String logSpeed = txsPerMinute > 60 ?  DF.format(txsPerMinute/60) + " tps" : DF2.format(txsPerMinute) + " txs/min";
        final String logConfirmed = UIManager.padLeft(confirmedSpam + (totalSpam < 1000 ? UIManager.padRight("/"+totalSpam, 4) : "")  + " txs ("+DF2.format(100*confirmationRate)+"%)", 20);
        final String logBalance = balanceString;
        final String logRewardPerMonth = DF.format(miotaPerMonth) + " Mi" + (priceUsd > 0 ? " ($"+DF.format(miotaPerMonth*priceUsd)+")": "");
        final String logNodes = UIManager.padLeft(NodeManager.getAmountOfAvailableAPIs() + "/" + NodeManager.getAmountOfAPIs(), 5) + "[@" + UIManager.padLeft(NodeManager.getApiIndex()+"", ((NodeManager.getAmountOfAPIs()-1)+"").length())+"]";

        String log = String.format(R.STR.getString("log" + (Main.isInOnlineMode() ? "" : "_offline")), logTime, logSpam, logSpeed, logConfirmed, logBalance, logRewardPerMonth, logNodes);

		uim.logInf(log);
		//uim.logInf(buildThreadString());
	}

    private static String formatInterval(final long interval) {
        int sec = (int)Math.round(interval/1000.0);
        int min = sec/60;
        int hour = min/60;
        int day = hour/24;

        final DecimalFormat dblDig = new DecimalFormat("00");
        final String intervalFormat = "%1$d:%2$s:%3$s:%4$s";

        return String.format(intervalFormat, day, dblDig.format(hour%24), dblDig.format(min%60), dblDig.format(sec%60));
    }

    private static void performanceReport() {
		if(SpamThread.isPaused()) return;
		DecimalFormat df2 = new DecimalFormat("#00.00");
		double powPercentage = 100.0 * GOldDiggerLocalPoW.getAvgPoWTime() * SpamThread.getSpamSpeed() / 60;

		final String performanceReport = String.format(R.STR.getString("log_performance_report"),
                df2.format(GOldDiggerLocalPoW.getAvgPoWTime()),
                (powPercentage > 95 ? UIManager.ANSI_GREEN : (powPercentage < 75) ? UIManager.ANSI_RED : "")+df2.format(powPercentage)+"%"+UIManager.ANSI_RESET,
                df2.format(NodeManager.getAvgTxsToApproveTime()),
                (TipPool.gttarsQueueSize() == 0 ? UIManager.ANSI_RED : "")+TipPool.gttarsQueueSize()+UIManager.ANSI_RESET+"/"+TipPool.gttarsLimit(),
                //buildThreadString(),
                buildHeapString());

		uim.logInf(performanceReport);
	}

    public static String buildHeapString() {
		final long heapSize = Runtime.getRuntime().totalMemory()/1024/1024;
		final long heapMaxSize = Runtime.getRuntime().maxMemory()/1024/1024;
		final long heapFreeSize = Runtime.getRuntime().freeMemory()/1024/1024;
        return INT.format(heapSize) + "/" + INT.format(heapMaxSize) + "/" + INT.format(heapFreeSize) + " MB";
	}

	public static String buildThreadString() {
	    /*return "M"+Main.SUPER_THREAD.activeCount() + "/P" +
                PearlDiver.POW_THREAD.activeCount() + "/A" +
                TimeAbortCall.TIME_ABORT_CALL_THREAD.activeCount() + "/C" +
                CronJob.CRONJOB_THREAD.activeCount() + "/T" +
                TipPool.TIP_POOL_THREAD_GROUP.activeCount() + "/N" +
                NodeManager.CONNECT_TO_ANY_NODE_THREAD_GROUP.activeCount() + "/S" +
                NodeManager.DO_SYNC_CHECK_THREAD_GROUP.activeCount();*/
	    return "A" + TimeAbortCall.NODE_INFO.activeCount() + "/B" +
				TimeAbortCall.TA_BROADCAST.activeCount() + "/C" +
				TimeAbortCall.TA_SPAM.activeCount() + "/D" +
				TimeAbortCall.TIPS.activeCount() + "/E" +
				TimeAbortCall.TIME_ABORT_CALL_THREAD.activeCount();
	}

	private static void updateIotaTicker() {
		DecimalFormat df = new DecimalFormat("###,##0.00"), dfInt = new DecimalFormat("###,##0");
		
		String jsonString = APIManager.request(APIManager.CMC_API_IOTA, null);
		
		try {
			JSONObject obj = new JSONArray(jsonString).getJSONObject(0);
			
			priceUsd = obj.getDouble("price_usd");
			if(SpamThread.isPaused()) return;

			final double change24h = obj.getDouble("percent_change_24h");

			final String priceUsdString = df.format(priceUsd);
			final String priceSats = dfInt.format((int) (100000000*obj.getDouble("price_btc")));
            final String change = (change24h<0?UIManager.ANSI_RED:UIManager.ANSI_GREEN+"+")+df.format(change24h)+"%"+UIManager.ANSI_RESET;
            final String mcap = df.format(obj.getDouble("market_cap_usd")/1e9);

			final String format = R.STR.getString("log_price_ticker").replace("[b]", UIManager.ANSI_BOLD).replace("[r]", UIManager.ANSI_RESET);
			final String log = String.format(format, priceUsdString, change, priceSats, mcap, obj.getInt("rank"));

            uim.logInf(log);

		} catch (JSONException e) {
			uim.logDbg(jsonString);
			uim.logException(e, false);
			return;
		}
	}
	
	private static double getConfirmationRate() {
		return AddressManager.getTailsConfirmRate(15);
	}
	
	private static int getConfirmedSpam() {
		return AddressManager.getTailsConfirmedTxs(-1);
	}
}
