package iota;

import java.util.ArrayList;

import iota.ui.UIManager;

public class AddressManager {

	private static final UIManager uim = new UIManager("AddrMngr");
	
	public static final String DIR = "addr";

	private static final int MAX_TXS_PER_ADDRESS = 300;
	private static final int DURATION_UNTIL_CHECKING_TAIL = 1800;
	
	private static String addressBase;
	private static ArrayList<Tail> tails = new ArrayList<Tail>();
	private static int txCountSinceTailCreation, txCountInit = 0, preSessionTransactions;
	
	public static void init(String parAddressBase, NodeManager nodeManager) {
		addressBase = parAddressBase;
		
		if(FileManager.exists(DIR+"/"+addressBase+".dat"))
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
				String[] hashes = nodeManager.findTransactionsByAddress(addressBase + tail.getTrytes());
				String latestMilestone = nodeManager.getLatestMilestone();
				boolean[] states = nodeManager.getInclusionStates(hashes, latestMilestone);
				if(states.length == 0)
					continue;
				
				int confirmedTxs = 0;
				for(boolean state : states)	if(state) confirmedTxs++;
				
				tail.setTotalTxs(states.length);
				tail.setMilestone(latestMilestone);
				tail.setConfirmedTxs(confirmedTxs);
				
				uim.logDbg("checked address '"+addressBase + tail.getTrytes()+"': " + tail.getConfirmedTxs() + "/" + tail.getTotalTxs() + " confirmed txs");
				SpamFundAPI.saveTail(tail);
				
				FileManager.write(DIR+"/"+addressBase+".dat", FileManager.readFile(DIR+"/"+addressBase+".dat").replace(lastTailOldString, tail.toString()));
			}
		}
	}
	
	public static String getSpamAddress() {
		String retAddress = addressBase + tails.get(tails.size()-1).getTrytes();
		if(txCountInit+SpamThread.getTotalTxs()-txCountSinceTailCreation >= MAX_TXS_PER_ADDRESS)
			createNewAddressTail();
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
		while(tailTrytes.length() < 81 - addressBase.length())
			tailTrytes += (char)((int)'A'+(int)(Math.random()*26));
		uim.logDbg("changing spam address to "  + addressBase + tailTrytes);
		Tail tail = new Tail(tailTrytes + "|0|0|0|0");
		tails.add(tail);
		FileManager.write(DIR+"/"+addressBase+".dat", (FileManager.exists(DIR+"/"+addressBase+".dat") ? FileManager.readFile(DIR+"/"+addressBase+".dat").replace(lastTailOldString, lastTailNewString) + "\n" : "") + tail.toString());
	}
	
	public static void loadAddresses(NodeManager nodeManager) {
		String[] tailStrings = FileManager.readFile(DIR+"/"+addressBase + ".dat").split("\n");
		for(String tailString : tailStrings) {
			if((tailString = tailString.replace(" ", "")).length() > 0)
				tails.add(new Tail(tailString));
		}
		txCountInit = nodeManager.findTransactionsByAddress(getSpamAddress()).length;
		uim.logDbg("picking up spam address from last session '"+getSpamAddress()+"' ("+txCountInit+" txs)");
		txCountSinceTailCreation = 0;
		preSessionTransactions = getTailsTotalTxs(999999);
	}
	
	public static int getPreSessionTransactions() {
		return preSessionTransactions;
	}
	
	public static double getTailsConfirmRate(int amountOfTails) {
		return 100.0 * getTailsConfirmedTxs(amountOfTails) / getTailsTotalTxs(amountOfTails);
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