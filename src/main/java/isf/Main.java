package isf;

import iota.GOldDiggerLocalPoW;
import isf.spam.UploadDataManager;
import isf.spam.*;
import isf.ui.R;
import isf.ui.UIManager;
import isf.ui.UIQuestion;

public class Main {
	
	private static final UIManager UIM = new UIManager("Main");
	private static boolean onlineMode, testnetMode;

	public static final ThreadGroup SUPER_THREAD = new ThreadGroup( "Super-Thread" );

	public static void main(String[] args) {

		testnetMode = findParameterIndex("-testnet", args) != -1;
		onlineMode = !testnetMode && findParameterIndex("-offline", args) == -1;

        UIM.print("\n"+UIManager.ANSI_BOLD+String.format(R.STR.getString("main_welcome"), buildFullVersion()));
		Configs.loadOrGenerate();

		APIManager.login(findParameter("-email", args), findParameter("-pass", args));

		mainMenu(findParameterIndex("-autostart", args) != -1);

        if(testnetMode)
            UIM.logWrn(R.STR.getString("main_testnet_mode"));
		else if(!onlineMode)
            UIM.logWrn(R.STR.getString("main_offline_mode"));

		if(Configs.getBln(P.POW_USE_GO_MODULE))
			GOldDiggerLocalPoW.downloadPowIfNonExistent();

		APIManager.requestSpamParameters();

		NodeManager.init();

        UIM.logDbg(R.STR.getString("nodes_waiting"));
		while (NodeManager.getAmountOfAvailableAPIs() == 0) try { Thread.sleep(200); } catch (InterruptedException e) {}

        AddressManager.init();
        int powCores = Configs.getInt(P.POW_CORES);
        UIM.logDbg(String.format(R.STR.getString("main_start_pow_cores"), powCores));

		TipPool.init();
        UploadDataManager.start();
		new SpamThread().init();
        Logger.init();

    	Runtime.getRuntime().addShutdownHook(new Thread(Main.SUPER_THREAD, "ShutDownHook") {
    		@Override
    		public void run() {
                UIM.logDbg(R.STR.getString("main_terminate"));
                GOldDiggerLocalPoW.shutDown();
    			do {
    			    int amountQueued = TxBroadcaster.getAmountQueued();
    			    if(amountQueued == 0) break;
                    UIM.logDbg(String.format(R.STR.getString("main_terminate_broadcast"), amountQueued));
                    try { sleep(1000); } catch(InterruptedException e) {}
                } while (true);
                AddressManager.updateTails();
    		}
    	});
	}

    private static String findParameter(String name, String[] args) {
	    int index = findParameterIndex(name, args);
	    return index == -1 || index == args.length-1 ? null : args[index+1];
    }

    private static int findParameterIndex(String name, String[] args) {
        for(int i = 0; i < args.length; i++)
            if(args[i].equals(name))
                return i;
        return -1;
    }

	public static void mainMenu(boolean autostart) {

	    if(autostart) {
            UIM.logWrn(R.STR.getString("main_skip_updates"));
            return;
        }

        UIM.print(String.format(R.STR.getString("main_skip_menu_instructions"), UIManager.ANSI_BOLD+"java -jar isf-jclient-[VERSION].jar -autostart" + UIManager.ANSI_RESET));
	    String command;
		do {
            command = UIM.askQuestion(UIQuestion.Q_START_MENU);
		    switch (command) {
                case ("r"):
                    APIManager.printRewards();
                    break;
                case ("c"):
                    Configs.edit();
                    break;
            }
		} while (!command.equals("s"));
        UIM.printUpdates();
	}

	public static String getVersion() {
		return "v1.0";
	}

	public static String getBuild() {
		return "13";
	}

	public static String buildFullVersion() {
		return getVersion() + "." + getBuild();
	}

	public static boolean isInOnlineMode() {
		return onlineMode;
	}

	public static boolean isInTestnetMode() {
		return testnetMode;
	}

    public static void setOnlineMode(boolean onlineMode) {
        Main.onlineMode = onlineMode;
    }
}