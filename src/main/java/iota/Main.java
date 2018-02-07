package iota;

import iota.ui.UIManager;
import iota.ui.UIQuestion;

public class Main {
	
	private static final UIManager uim = new UIManager("Main");
	
	public static void main(String[] args) {
		
		UIManager.setDebugEnabled(true);
		
		String command;

		UIManager.init();
		Configs.init();
		
		do {
			command = uim.askQuestion(new UIQuestion() {
				
				@Override
				public boolean isAnswer(String str) {
					return str.equals("start") || str.equals("debug") || str.equals("rewards");
				}
				
			}.setQuestion("Please enter a command [start/debug/rewards]"));

			if(command.equals("debug")) {
				uim.logInf(UIManager.isDebugEnabled() ? "disabling debugging mode" : "activating debugging mode");
				UIManager.setDebugEnabled(!UIManager.isDebugEnabled());
			}
			
			if(command.equals("rewards")) {
				SpamFundAPI.printRewards();
			}
		} while (!command.equals("start"));
		
		uim.printUpdates();
		
		NodeManager.shuffleNodeList();
		NodeManager nodeManager1 = new NodeManager(1);
		AddressManager.init(nodeManager1);
		
		new LogThread().start();
		
		uim.logDbg("starting " + Configs.spam_threads + " spam thread" + (Configs.spam_threads == 1 ? "" : "s"));
		for(int i = 1; i <= Configs.spam_threads; i++)
			new SpamThread(i, i == 1 ? nodeManager1 : null).start();
		
	}
	
	public static String getVersion() {
		return "v1.0";
	}
	
	public static String getBuild() {
		return "2018-02-06_1";
	}
}