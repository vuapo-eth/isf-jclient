package isf;

import java.util.ArrayList;

import isf.ui.UIManager;

public class AddressManager {

	private static final UIManager uim = new UIManager("AddrMngr");
	
	public static final String DIR = "addr";

	private static final int MAX_TXS_PER_ADDRESS = 300;
	private static final int DURATION_UNTIL_CHECKING_TAIL = 1800;
	
	private static String addressBase;
	private static ArrayList<Tail> tails = new ArrayList<Tail>();
	private static int txCountSinceTailCreation, txCountInit = 0, preSessionTransactions;
	private static long lastTailCreated = 0;
	
	public static void init() {
		
		if(FileManager.exists(DIR+"/"+addressBase+".dat"))
			loadAddresses();
		else {
			FileManager.mkdirs(DIR);
			createNewAddressTail();
		}
		
		TimeManager.addTask(new Task(300000, true) { @Override void onCall() { AddressManager.getTail().update(); } });
		TimeManager.addTask(new Task(60000, true) { @Override void onCall() { AddressManager.updateTails(); } });
	}
	
	public static void setAddressBase(String addressBase) {
		AddressManager.addressBase = addressBase;
	}
	
	public static String getAddressBase() {
		return addressBase;
	}
	
	public static void updateTails() {
		
		boolean anythingChanged = false;
		for(int i = 0; i < tails.size()-1; i++) {
			Tail tail = tails.get(i);
			if(tail.getTimestamp() < System.currentTimeMillis()/1000-DURATION_UNTIL_CHECKING_TAIL && tail.getMilestone().equals("-")) {
				tail.update();
				anythingChanged = true;
			}
		}
		if(anythingChanged)
			writeTailsIntoFile();
	}
	
	public static void writeTailsIntoFile() {
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < tails.size(); i++)
			s.append(s.toString().length() > 0 ? "\n" : "").append(tails.get(i).toString());
		FileManager.write(DIR+"/"+addressBase+".dat", s.toString());
	}
	
	public static void readTailsFromFile() {
		String s = FileManager.read(DIR+"/"+addressBase+".dat");
		String[] tailStrings = s.replace(" ", "").split("\n");
		tails = new ArrayList<Tail>();
		for(int i = 0; i < tailStrings.length; i++) {
			if(tailStrings[i].length() > 0) tails.add(new Tail(tailStrings[i]));
		}
	}
	
	public static Tail getTail() {
		return tails.get(tails.size()-1);
	}
	
	public static String getSpamAddress() {
		String retAddress = addressBase + getTail().getTrytes();
		if(txCountInit+SpamThread.getTotalTxs()-txCountSinceTailCreation >= MAX_TXS_PER_ADDRESS && System.currentTimeMillis() - lastTailCreated > 10000) {
			getTail().update();
			writeTailsIntoFile();
			lastTailCreated = System.currentTimeMillis();
			createNewAddressTail();
		}
		return retAddress;
	}
	
	private static void createNewAddressTail() {
		if(tails.size() > 0) {
			getTail().setTimestamp((int)(System.currentTimeMillis()/1000));
			APIManager.broadcastTail(getTail());
		}
		txCountSinceTailCreation = SpamThread.getTotalTxs();
		txCountInit = 0;
		String tailTrytes = "";
		while(tailTrytes.length() < 81 - addressBase.length())
			tailTrytes += (char)((int)'A'+(int)(Math.random()*26));
		uim.logDbg("changing spam address to "  + addressBase + tailTrytes);
		
		tails.add(new Tail(tailTrytes + "|0|0|0|0"));
		writeTailsIntoFile();
	}
	
	public static void loadAddresses() {
		readTailsFromFile();
		
		if(tails.size() > 0) {
			Tail tail = getTail();
			tail.update();
			tail.setMilestone("-");
			writeTailsIntoFile();
			txCountInit = tail.getTotalTxs();
			uim.logDbg("picking up address from last session '"+getSpamAddress()+"' ("+tail.getConfirmedTxs() + "/" + tail.getTotalTxs() + " txs)");
		} else {
			createNewAddressTail();
		}
		
		txCountSinceTailCreation = 0;
		preSessionTransactions = getTailsTotalTxs(-1);
	}
	
	public static int getPreSessionTransactions() {
		return preSessionTransactions;
	}
	
	public static double getTailsConfirmRate(int amountOfTails) {
		int total = getTailsTotalTxs(amountOfTails), confirmed = getTailsConfirmedTxs(amountOfTails);
		return total == 0 ? 0 : 100.0 * confirmed / total;
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