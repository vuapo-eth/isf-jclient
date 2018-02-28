package isf;

import org.json.JSONObject;

import isf.ui.UIManager;
import isf.ui.UIQuestion;

public class Main {
	
	private static final UIManager uim = new UIManager("Main");
	
	public static void main(String[] args) {
		
		uim.print("\n===== Welcome to the Spam Fund Java Client " + Main.buildFullVersion() + " ===");
		Configs.loadOrGenerate();
		
		mainMenu(args.length > 0 && args[0] != null ? args[0].toLowerCase() : "");
		
		JSONObject spamParameters = APIManager.requestSpamParameters();
		AddressManager.setAddressBase(spamParameters.getString("address"));
		SpamThread.setTag(spamParameters.getString("tag"));
		
		NodeManager.init();
		AddressManager.init();
		
		int powThreads = Configs.getInt(P.THREADS_AMOUNT_POW);
		uim.logDbg("starting " + powThreads + " pow thread"+(powThreads == 1 ? "" : "s")+" at priority " + Configs.getInt(P.THREADS_PRIORITY_POW));
		
		Logger.init();
		new TipPool().start();
		for(int i = 0; i < 10; i++) new TxBroadcaster().start();
		new SpamThread().start();
	}
	
	public static void mainMenu(String command) {
		
		boolean lookForUpdates = !command.equals("start");
		
		if(lookForUpdates)
			uim.print("You can skip this menu by starting the .jar file with the parameter 'start' like this: "+UIManager.ANSI_BOLD+"'java -jar isf-jclient-[VERSION].jar start'\n");
		else
			uim.logDbg("skipping 'looking for updates' because of auto start (program was started with parameter 'start')");
			
		while (!command.equals("start")) {
			
			if(command.equals("rewards")) APIManager.printRewards();
			if(command.equals("config")) Configs.edit();
			if(command.equals("debug")) {
				uim.logInf(UIManager.isDebugEnabled() ? "disabling debugging mode" : "activating debugging mode");
				UIManager.setDebugEnabled(!UIManager.isDebugEnabled());
			}
			
			command = uim.askQuestion(UIQuestion.Q_START_MENU);
		}
		
		if(lookForUpdates) uim.printUpdates();
	}
	
	public static String getVersion() {
		return "v1.0";
	}
	
	public static String getBuild() {
		return "6";
	}
	
	public static String buildFullVersion() {
		return getVersion() + "." + getBuild();
	}
}