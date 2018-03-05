package isf.ui;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import isf.APIManager;
import isf.Configs;
import isf.FileManager;
import isf.P;

public class UIManager {
	
	private static boolean debug = true;
	
	public static String ANSI_RESET = "\u001B[0m";
	public static String ANSI_BRIGHT_BLACK = "\u001B[90m";
	public static String ANSI_BLACK = "\u001B[30m";
	public static String ANSI_RED = "\u001B[31m";
	public static String ANSI_GREEN = "\u001B[32m";
	public static String ANSI_YELLOW = "\u001B[33m";
	public static String ANSI_BLUE = "\u001B[34m";
	public static String ANSI_PURPLE = "\u001B[35m";
	public static String ANSI_CYAN = "\u001B[36m";
	public static String ANSI_WHITE = "\u001B[37m";
	public static String ANSI_BOLD = "\u001B[1m";

	private static final PrintStream ORIGINAL_STREAM = System.out, ORIGINAL_ERR = System.err;
	private static final PrintStream DUMMY_STREAM = new PrintStream(new OutputStream(){public void write(int b) { }});
	private static ArrayList<String> logs = new ArrayList<String>();
	private static long logFileID = System.currentTimeMillis(), lastLogSaved = System.currentTimeMillis();
	
	private static long pauseUntil;
	
	private final String identifier;
	
	public UIManager(String identifier) {
		this.identifier = identifier;
	}
	
	public static void toggleColors(boolean enabled) {
		ANSI_RESET = enabled ? "\u001B[0m" : "";
		ANSI_BRIGHT_BLACK = enabled ? "\u001B[90m" : "";
		ANSI_BLACK = enabled ? "\u001B[30m" : "";
		ANSI_RED = enabled ? "\u001B[31m" : "";
		ANSI_GREEN = enabled ? "\u001B[32m" : "";
		ANSI_YELLOW = enabled ? "\u001B[33m" : "";
		ANSI_BLUE = enabled ? "\u001B[34m" : "";
		ANSI_PURPLE = enabled ? "\u001B[35m" : "";
		ANSI_CYAN = enabled ? "\u001B[36m" : "";
		ANSI_WHITE = enabled ? "\u001B[37m" : "";
		ANSI_BOLD = enabled ? "\u001B[1m" : "";
	}
	
	static {
		System.setOut(DUMMY_STREAM);
		setSystemErrorEnabled(true);
	}
	
	public void print(String line) {
		if(pauseUntil > System.currentTimeMillis()) {
			try {
				Thread.sleep(pauseUntil - System.currentTimeMillis());
			} catch (InterruptedException e) {
				
			}
		}
		
		ORIGINAL_STREAM.println(line+ANSI_RESET);
		logs.add(line+ANSI_RESET);
		saveLogs();
	}
	
	public static void saveLogs() {
		
		if(lastLogSaved < System.currentTimeMillis()-60000) {
			lastLogSaved = System.currentTimeMillis();
			String logString = "";
			for(int i = 0; i < logs.size(); i++) logString += logs.get(i) + "\n";
			
			if(!FileManager.exists("logs")) FileManager.mkdirs("logs");
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			
			FileManager.write("logs/"+sdf.format(logFileID)+".txt", logString);
			
			if(logs.size() > 1000) {
				logFileID = System.currentTimeMillis();
				logs = new ArrayList<String>();
			}
		}
	}
	
	public void logPln(String msg) {
		String timeFormat = Configs.get(P.LOG_TIME_FORMAT);
		print(padRight("["+(new SimpleDateFormat(timeFormat == null ? "HH:mm:ss" : timeFormat)).format(Calendar.getInstance().getTime())+"] [" + identifier + "]", 22) + " " + msg);
	}
	
	public void logInf(String msg) { logPln("[INF] " + msg); }
	public void logWrn(String msg) { logPln(ANSI_BOLD+ANSI_YELLOW + "[WRN] " + msg); pause(1); }
	public void logErr(String msg) { logPln(ANSI_BOLD+ANSI_RED + "[ERR] " + msg); pause(1); }
	public void logDbg(String msg) { if(debug) logPln(ANSI_BRIGHT_BLACK + "[DBG] " + msg); }

	public static void setDebugEnabled(boolean enabled) {
		debug = enabled;
	}
	
	public static boolean isDebugEnabled() {
		return debug;
	}
	
	public static void setSystemErrorEnabled(boolean enabled) {
		System.setErr(enabled ? ORIGINAL_ERR : DUMMY_STREAM);
	}
	
	public void logException(Throwable e, boolean terminate) {
		logErr(e.getMessage());
		
		if(debug) {
			ORIGINAL_ERR.println(ANSI_BRIGHT_BLACK);
			setSystemErrorEnabled(true);
			e.printStackTrace();
			setSystemErrorEnabled(false);
			ORIGINAL_ERR.println(ANSI_RESET);
		}
		
		if(terminate) {
			logDbg("program will be terminated now due to above error");
			System.exit(0);
		}
	}
	
	private static void pause(int s) {
		pauseUntil = System.currentTimeMillis()+s*1000;
	}
	
	public String readLine(String msg) {
		print("");
		String line = System.console().readLine(msg);
		print("");
		return line;
	}
	
	public int askQuestionInt(UIQuestionInt question) {
		return Integer.parseInt(askQuestion(question));
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
	
	public String readPassword(String msg) {
		print("");
		String line = new String(System.console().readPassword(msg));
		print("");
		return line;
	}
	
	public void printUpdates() {
		logDbg("checking for updates (will appear below if there are any)");
		JSONObject jsonObj = APIManager.requestUpdates();
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
