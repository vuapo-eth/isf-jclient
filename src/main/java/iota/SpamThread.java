package iota;

import java.util.ArrayList;

import jota.model.Input;
import jota.model.Transfer;

public class SpamThread extends Thread {

	private static int totalTxs = 0;
	private final int id;
	private NodeManager nodeManager;
	private int lastSyncCheck;
	
	private static int command;
	
	public SpamThread(int id, NodeManager nodeManager) {
		this.id = id;
		lastSyncCheck = (int)(System.currentTimeMillis() / 1000) + (int)(1.0*id/Configs.spam_threads*Configs.sync_check_interval);
		this.nodeManager = nodeManager;
	}
	
	@Override
	public void run() {
		
		setPriority(MAX_PRIORITY);
		
		if(nodeManager == null)
			nodeManager = new NodeManager(id);

		if(id == 1)
			AddressManager.updateTails(nodeManager);
		
		while(true) {
			
			ArrayList<Transfer> transfers = new ArrayList<Transfer>();

			String nonce = "";
			while(nonce.length() < 7)
				nonce += (char)((int)'A'+(int)(Math.random()*26));
			
			String message = UploadDataManager.getNextData();
			
			transfers.add(new Transfer(AddressManager.getSpamAddress(), 0, message, "99999999999IOTASPAM9DOT9COM"));
			transfers.get(0).setTag("99999999999IOTASPAM9DOT9COM");
			Input[] inputs = {};
			nodeManager.sendTransfer(transfers, inputs);
			totalTxs++;
			
			if(command == 1) {
				nodeManager.getUIM().logWrn("spamming thread paused remotely by ISF website");
				while(command == 1)
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				nodeManager.getUIM().logInf("spamming thread restarted remotely by ISF website");
			}
			
			if(totalTxs % 10 == 0)
				AddressManager.updateTails(nodeManager);
				
			if(totalTxs % 50 == 0)
				AddressManager.getTail().update(nodeManager);
				
			if(lastSyncCheck < (int)(System.currentTimeMillis() / 1000) - Configs.sync_check_interval) {
				nodeManager.reconnect();
				lastSyncCheck = (int)(System.currentTimeMillis() / 1000);
			}
		}
	}
	
	protected static int getTotalTxs() {
		return totalTxs;
	}

	public static void setCommand(int command) {
		SpamThread.command = command;
	}
}