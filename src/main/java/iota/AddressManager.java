package iota;

import java.util.ArrayList;

import iota.ui.UIManager;

public class AddressManager {

	private static final UIManager uim = new UIManager("AddrMngr");
	
	public static final String DIR = "addr";

	private static final int MAX_TXS_PER_ADDRESS = 300;
	private static final int DURATION_UNTIL_CHECKING_TAIL = 1800;
	
	public static final String ADDRESS_BASE = SpamFundAPI.requestSpamAddress();
	private static ArrayList<Tail> tails = new ArrayList<Tail>();
	private static int txCountSinceTailCreation, txCountInit = 0, preSessionTransactions;
	private static long lastTailCreated = 0;
	
	public static void init(NodeManager nodeManager) {
		
		if(FileManager.exists(DIR+"/"+ADDRESS_BASE+".dat"))
			loadAddresses(nodeManager);
		else {
			FileManager.mkdirs(DIR);
			createNewAddressTail();
		}
	}
	
	public static void updateTails(NodeManager nodeManager) {
		
		for(int i = 0; i < tails.size()-1; i++) {
			Tail tail = tails.get(i);
			if(tail.getTimestamp() < System.currentTimeMillis()/1000-DURATION_UNTIL_CHECKING_TAIL && tail.getTimestamp() != 0) {

				String lastTailOldString = tail.toString();
				
				tail.setTimestamp(0);
				tail.update(nodeManager);
				
				uim.logDbg("checked address '"+ADDRESS_BASE + tail.getTrytes()+"': " + tail.getConfirmedTxs() + "/" + tail.getTotalTxs() + " confirmed txs");
				SpamFundAPI.saveTail(tail);
				
				FileManager.write(DIR+"/"+ADDRESS_BASE+".dat", FileManager.readFile(DIR+"/"+ADDRESS_BASE+".dat").replace(lastTailOldString, tail.toString()));
			}
		}
	}
	
	public static Tail getTail() {
		return tails.get(tails.size()-1);
	}
	
	public static String getSpamAddress() {
		String retAddress = ADDRESS_BASE + tails.get(tails.size()-1).getTrytes();
		if(txCountInit+SpamThread.getTotalTxs()-txCountSinceTailCreation >= MAX_TXS_PER_ADDRESS && System.currentTimeMillis() - lastTailCreated > 10000) {
			lastTailCreated = System.currentTimeMillis();
			createNewAddressTail();
		}
		return retAddress;
	}
	
	private static void createNewAddressTail() {
		String lastTailOldString = tails.size() == 0 ? "99999999999999999999999999999999" : tails.get(tails.size()-1).toString();
		if(tails.size() > 0)
			tails.get(tails.size()-1).setTimestamp((int)(System.currentTimeMillis()/1000));
		String lastTailNewString = tails.size() == 0 ? "99999999999999999999999999999999" : tails.get(tails.size()-1).toString();
		txCountSinceTailCreation = SpamThread.getTotalTxs();
		txCountInit = 0;
		String tailTrytes = "";
		while(tailTrytes.length() < 81 - ADDRESS_BASE.length())
			tailTrytes += (char)((int)'A'+(int)(Math.random()*26));
		uim.logDbg("changing spam address to "  + ADDRESS_BASE + tailTrytes);
		Tail tail = new Tail(tailTrytes + "|0|0|0|0");
		tails.add(tail);
		FileManager.write(DIR+"/"+ADDRESS_BASE+".dat", (FileManager.exists(DIR+"/"+ADDRESS_BASE+".dat") ? FileManager.readFile(DIR+"/"+ADDRESS_BASE+".dat").replace(lastTailOldString, lastTailNewString) + "\n" : "") + tail.toString());
	}
	
	public static void loadAddresses(NodeManager nodeManager) {
		String[] tailStrings = FileManager.readFile(DIR+"/"+ADDRESS_BASE + ".dat").split("\n");
		for(String tailString : tailStrings) {
			if((tailString = tailString.replace(" ", "")).length() > 0)
				tails.add(new Tail(tailString));
		}
		
		Tail tail = getTail();
		tail.update(nodeManager);
		txCountInit = tail.getTotalTxs();
		uim.logDbg("picking up address from last session '"+getSpamAddress()+"' ("+tail.getConfirmedTxs() + "/" + tail.getTotalTxs() + " txs)");
		txCountSinceTailCreation = 0;
		preSessionTransactions = getTailsTotalTxs(999999);
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