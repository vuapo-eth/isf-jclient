package isf;

import org.json.JSONObject;

import iota.GOldDiggerLocalPoW;
import isf.ui.UIManager;
import isf.ui.UIQuestion;

public class Main {
	
	private static final UIManager uim = new UIManager("Main");
	
	public static void main(String[] args) {
		
		uim.print(UIManager.ANSI_BOLD+"\n===== Welcome to the Spam Fund Java Client " + Main.buildFullVersion() + " =====");
		Configs.loadOrGenerate();
		
		mainMenu(args.length > 0 && args[0] != null ? args[0].toLowerCase() : "");
		
		if(Configs.getBln(P.POW_USE_GO_MODULE))
			GOldDiggerLocalPoW.download();
		
		JSONObject spamParameters = APIManager.requestSpamParameters();
		AddressManager.setAddressBase(spamParameters.getString("address"));
		SpamThread.setTag(spamParameters.getString("tag"));
		
		NodeManager.init();
		AddressManager.init();
		
		int powThreads = Configs.getInt(P.POW_CORES);
		uim.logDbg("starting " + powThreads + " pow thread"+(powThreads == 1 ? "" : "s"));
		
		Logger.init();
		TipPool.init();
		new SpamThread().start();
    	
    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		@Override
    		public void run() {
    			uim.logDbg("terminating ...");
    			AddressManager.updateTails();
    		}
    	});
	}
	
	public static void mainMenu(String command) {
		
		boolean lookForUpdates = !command.equals("start");
		
		if(lookForUpdates)
			uim.print("You can skip this menu by starting the .jar file with the parameter 'start' like this: "+UIManager.ANSI_BOLD+"'java -jar isf-jclient-[VERSION].jar start'\n");
		else
			uim.logWrn("skipping 'looking for updates' because of auto start (program was started with parameter 'start')");
			
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
		return "9";
	}
	
	public static String buildFullVersion() {
		return getVersion() + "." + getBuild();
	}
}