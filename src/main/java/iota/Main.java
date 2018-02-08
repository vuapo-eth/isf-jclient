package iota;

import iota.ui.UIManager;
import iota.ui.UIQuestion;

public class Main {
	
	private static final UIManager uim = new UIManager("Main");
	
	public static void main(String[] args) {
		
		UIManager.setDebugEnabled(true);
		
		String command;

		UIManager.init();
		uim.print("===== Welcome to the Spam Fund Java Client " + getVersion()+"."+getBuild() + " ===");
		
		
		Configs.init();
		
		if(args.length > 0 && args[0] != null && args[0].toLowerCase().equals("start")) {
			uim.logDbg("skipping 'looking for updates' because of auto start (program was started with parameter 'start')");
		} else {
			uim.print("You can skip this menu by starting the .jar file with the parameter 'start' like this: "+UIManager.ANSI_BOLD+"'java -jar isf-jclient-[VERSION].jar start'\n");
			
			do {
				command = uim.askQuestion(new UIQuestion() {
					
					@Override
					public boolean isAnswer(String str) {
						return str.equals("start") || str.equals("debug") || str.equals("rewards") || str.equals("config");
					}
					
				}.setQuestion("Please enter a command [start/rewards/config/debug]"));

				if(command.equals("debug")) {
					uim.logInf(UIManager.isDebugEnabled() ? "disabling debugging mode" : "activating debugging mode");
					UIManager.setDebugEnabled(!UIManager.isDebugEnabled());
				}
				
				if(command.equals("rewards"))
					SpamFundAPI.printRewards();
				
				if(command.equals("config"))
					Configs.editConfigs();
				
			} while (!command.equals("start"));
			
			uim.printUpdates();
		}
		
		NodeManager.shuffleNodeList();
		NodeManager nodeManager1 = new NodeManager(1);
		AddressManager.init(nodeManager1);
		
		new LogThread().start();
		
		uim.logDbg("starting " + Configs.threads_amount + " spam thread" + (Configs.threads_amount == 1 ? "" : "s") + " with "
				+ (Configs.threads_priority == 1 ? "minimum" : (Configs.threads_priority == 3 ? "maximum" : "normal")) + " priority");
		for(int i = 1; i <= Configs.threads_amount; i++)
			new SpamThread(i, i == 1 ? nodeManager1 : null).start();
		
	}
	
	public static String getVersion() {
		return "v1.0";
	}
	
	public static String getBuild() {
		return "3";
	}
}