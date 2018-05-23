package isf.spam;

import java.util.ArrayList;
import java.util.regex.Pattern;

import isf.*;
import isf.logic.CronJob;
import isf.logic.CronJobManager;
import isf.ui.R;
import isf.ui.UIManager;
import org.apache.commons.lang3.StringUtils;

public class AddressManager {

	private static final UIManager UIM = new UIManager("AddrMngr");
	
	public static final String DIR = "addr";

	private static final int MAX_TXS_PER_ADDRESS = 300;
	private static final int DURATION_UNTIL_CHECKING_TAIL = 1800;
	
	private static String addressBase;
	private static ArrayList<Tail> tails = new ArrayList<Tail>();
	private static int preTailSessionTxCount, preSessionTailTxCount = 0, preSessionTxCount;
	private static long lastTailCreated = 0;
	private static int sessionTxCount = 0;
	
	public static void init() {

	    if(!Main.isInOnlineMode())
	        return;

		if(FileManager.exists(DIR+"/"+addressBase+(Main.getNetSuffix())+".dat"))
			loadAddresses();
		else {
			FileManager.mkdirs(DIR);
			createNewAddressTail();
		}

		CronJobManager.addCronJob(new CronJob(300000, true, false) { @Override public void onCall() { AddressManager.getTail().update(); } });
		CronJobManager.addCronJob(new CronJob(60000, true, false) { @Override public void onCall() { AddressManager.updateTails(); } });
	}
	
	public static void setAddressBase(String addressBase) {
		AddressManager.addressBase = addressBase;
	}
	
	public static void incrementSessionTxCount() {
		sessionTxCount++;
	}
	
	protected static int getSessionTxCount() {
		return sessionTxCount;
	}
	
	public static String getAddressBase() {
		return addressBase;
	}
	
	public static void updateTails() {

	    if(!Main.isInOnlineMode())
	        return;

		boolean anythingChanged = false;
		for(int i = 0; i < tails.size()-1; i++) {
			final Tail tail = tails.get(i);
			if(tail.getTimestamp() < System.currentTimeMillis()/1000-DURATION_UNTIL_CHECKING_TAIL && !tail.isLastCheckCompleted()) {
				tail.update();
				tail.setLastCheckCompleted(true);
				anythingChanged = true;
			}
		}
		if(anythingChanged)
			writeTailsIntoFile();
	}
	
	public static void writeTailsIntoFile() {
		if(!Main.isInOnlineMode())
		    return;

		StringBuilder s = new StringBuilder();
		for(int i = 0; i < tails.size(); i++)
			s.append(s.toString().length() > 0 ? "\n" : "").append(tails.get(i).toString());
		FileManager.write(DIR+"/"+addressBase+Main.getNetSuffix()+".dat", s.toString());
	}
	
	public static void readTailsFromFile() {
		String s = FileManager.read(DIR+"/"+addressBase+Main.getNetSuffix()+".dat");
		String[] tailStrings = s.replace(" ", "").split("\n");
		tails = new ArrayList<Tail>();
		for(int i = 0; i < tailStrings.length; i++) {
			if(tailStrings[i].length() > 0) tails.add(new Tail(tailStrings[i].split("\\|")));
		}
	}
	
	public static Tail getTail() {
		return tails.get(tails.size()-1);
	}
	
	public static String getSpamAddress() {
	    if(!Main.isInOnlineMode()) {
	        return addressBase;
        }

		String retAddress = addressBase + getTail().getTrytes();
		if(Main.isInOnlineMode() && preSessionTailTxCount+getSessionTxCount()-preTailSessionTxCount >= MAX_TXS_PER_ADDRESS && System.currentTimeMillis() - lastTailCreated > 10000) {
			getTail().update();
			if(getTail().getTotalTxs() >= MAX_TXS_PER_ADDRESS) {
				writeTailsIntoFile();
				lastTailCreated = System.currentTimeMillis();
				createNewAddressTail();
			} else {
				sessionTxCount = getTail().getTotalTxs() + preTailSessionTxCount - preSessionTailTxCount;
			}
		}
		return retAddress;
	}
	
	private static void createNewAddressTail() {
		if(tails.size() > 0) {
			getTail().setTimestamp((int)(System.currentTimeMillis()/1000));
			APIManager.broadcastTail(getTail());
		}
		preTailSessionTxCount = sessionTxCount;
		preSessionTailTxCount = 0;
		String tailTrytes = "";
		while(tailTrytes.length() < 81 - addressBase.length())
			tailTrytes += (char)((int)'A'+(int)(Math.random()*26));
        UIM.logDbg(String.format(R.STR.getString("address_changing"),addressBase + tailTrytes));
		
		tails.add(new Tail(tailTrytes, 0, 0, 0, false));
		writeTailsIntoFile();
	}
	
	public static void loadAddresses() {
		readTailsFromFile();
		
		if(tails.size() > 0) {
			Tail tail = getTail();
			tail.update();
			writeTailsIntoFile();
			preSessionTailTxCount = tail.getTotalTxs();
			UIM.logDbg(String.format(R.STR.getString("address_last_session"), getSpamAddress(), tail.getConfirmedTxs(), tail.getTotalTxs()));
		} else {
			createNewAddressTail();
		}
		
		preTailSessionTxCount = 0;
		preSessionTxCount = getTailsTotalTxs(-1);
	}
	
	public static int getPreSessionTransactions() {
		return preSessionTxCount;
	}
	
	public static double getTailsConfirmRate(int amountOfTails) {
		int total = getTailsTotalTxs(amountOfTails), confirmed = getTailsConfirmedTxs(amountOfTails);
		return total == 0 ? 0 : 1.0 * confirmed / total;
	}
	
	public static int getTailsConfirmedTxs(int amountOfTails) {
		int confirmed = 0;
		for(int i = tails.size()-1; i >= (amountOfTails < 0 ? 0 : Math.max(0, tails.size()-amountOfTails)); i--)
			confirmed += tails.get(i).getConfirmedTxs();
		return confirmed;
	}
	
	public static int getTailsTotalTxs(int amountOfTails) {
		int total = 0;
		for(int i = tails.size()-1; i >= (amountOfTails < 0 ? 0 : Math.max(0, tails.size()-amountOfTails)); i--)
			total += tails.get(i).getTotalTxs();
		return total;
	}

}