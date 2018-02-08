package iota.ui;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import iota.Configs;
import iota.FileManager;
import iota.SpamFundAPI;

public class UIManager {
	
	private static boolean debug = false;
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BRIGHT_BLACK = "\u001B[90m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_BOLD = "\u001B[1m";

	private static final PrintStream ORIGINAL_STREAM = System.out;
	private static final PrintStream DUMMY_STREAM = new PrintStream(new OutputStream(){public void write(int b) { }});
	private static ArrayList<String> logs = new ArrayList<String>();
	private static long logFileID = System.currentTimeMillis(), lastLogSaved = System.currentTimeMillis();
	
	private final String identifier;
	
	public UIManager(String identifier) {
		this.identifier = identifier;
	}
	
	public static void init() {
		System.setOut(DUMMY_STREAM);
	}
	
	public void print(String line) {
		ORIGINAL_STREAM.println(line+ANSI_RESET);
		logs.add(line+ANSI_RESET);
		saveLogs();
	}
	
	public static void saveLogs() {
		
		if(lastLogSaved < System.currentTimeMillis()-60000) {
			lastLogSaved = System.currentTimeMillis();
			String logString = "";
			for(int i = 0; i < logs.size(); i++) logString += logs.get(i) + "\n";
			
			if(!FileManager.exists("logs/")) FileManager.mkdirs("logs/");
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			
			FileManager.write("logs/"+sdf.format(logFileID)+".txt", logString.getBytes());
			
			if(logs.size() > 1000) {
				logFileID = System.currentTimeMillis();
				logs = new ArrayList<String>();
			}
		}
	}
	
	public void logPln(String msg) {
		print(padRight("["+(new SimpleDateFormat(Configs.time_format)).format(Calendar.getInstance().getTime())+"] [" + identifier + "]", 22) + " " + msg);
	}
	
	public void logInf(String msg) { logPln("[INF] " + msg); }
	public void logWrn(String msg) { logPln(ANSI_BOLD+ANSI_YELLOW + "[WRN] " + msg); }
	public void logErr(String msg) { logPln(ANSI_BOLD+ANSI_RED + "[ERR] " + msg); }
	public void logDbg(String msg) { if(debug) logPln(ANSI_BRIGHT_BLACK + "[DBG] " + msg); }

	public static void setDebugEnabled(boolean enabled) {
		debug = enabled;
	}
	
	public static boolean isDebugEnabled() {
		return debug;
	}
	
	public void logException(Throwable e, boolean terminate) {
		logErr(e.getMessage());
		
		if(debug) {
			ORIGINAL_STREAM.println(ANSI_BRIGHT_BLACK);
			e.printStackTrace();
			ORIGINAL_STREAM.println(ANSI_RESET);
		}
		
		if(terminate) {
			logDbg("program will be terminated now due to above error");
			System.exit(0);
		}
	}
	
	public String readLine(String msg) {
		print("");
		String line = System.console().readLine(msg);
		print("");
		return line;
	}
	
	public String askQuestion(UIQuestion question) {

		String answer = null;
		do {
			if(answer != null)
				print(ANSI_BOLD + ANSI_YELLOW+(answer.length() == 0 ? "please answer the above question" : "answer '"+answer+"' is not a valid answer"));
			print("\n"+ANSI_BOLD+question.getQuestion());
			answer = question.hidesInput() ? readPassword("  > ") : readLine("  > ");
			if(!question.isCaseSensitive()) answer = answer.toLowerCase();
		} while(!question.isAnswer(answer));
		return answer;
	}
	
	public boolean askForBoolean(final String questionString) {

		return askQuestion(new UIQuestion() {
			@Override
			public boolean isAnswer(String str) {
				return str.equals("yes") || str.equals("no");
			}
			
		}.setQuestion(questionString + " [yes/no]")).equals("yes");
	}
	
	public int askForInteger(final String questionString, final int min, final int max) {

		return Integer.parseInt(askQuestion(new UIQuestion() {
			@Override
			public boolean isAnswer(String str) {
				try {
					int n = Integer.parseInt(str);
					return n >= min && n <= max;
				} catch (NumberFormatException e) {
					return false;
				}
			}
			
		}.setQuestion(questionString + " [any integer from " +min+" to "+max+"]")));
	}
	
	public String readPassword(String msg) {
		print("");
		String line = new String(System.console().readPassword(msg));
		print("");
		return line;
	}
	
	public void printUpdates() {
		logDbg("checking for updates (will appear below if there are any)");
		JSONObject jsonObj = new JSONObject(SpamFundAPI.requestUpdates());
		int screenIndex = 0;
		
		if(jsonObj.getJSONArray("screens").length() == 0)
			return;
			
		do {

			final JSONObject screen = jsonObj.getJSONArray("screens").getJSONObject(screenIndex);
			final JSONArray answers = screen.getJSONArray("answers");
			
			UIQuestion uiQuestion = new UIQuestion() {
				
				@Override
				public boolean isAnswer(String str) {
					for(int i = 0; i < answers.length(); i++)
						if(answers.getJSONObject(i).getString("answer").equals(str))
							return true;
					return false;
				}
				
			}.setQuestion(screen.getString("text"));
			
			String answer = askQuestion(uiQuestion);

			screenIndex = -1;
			for(int i = 0; i < answers.length(); i++)
				if(answers.getJSONObject(i).getString("answer").equals(answer))
					screenIndex = answers.getJSONObject(i).getInt("goto");
		} while(screenIndex >= 0);
	}
	
	public static String padRight(String s, int n) {
	    return String.format("%1$-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}
}
