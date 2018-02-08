package iota.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UIQuestion {

	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
	private static final Pattern VALID_NODE_ADDRESS_REGEX = Pattern.compile("^(http|https)(://)[A-Z0-9.]*(:)[0-9]{1,5}$", Pattern.CASE_INSENSITIVE);
	
	public static final UIQuestion Q_PASSWORD = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return str.length() > 0;
		}
	}.setQuestion("what is your password? (recover it on http://iotaspam.com/account?p=recover)").hideInput(true);
	
	public static final UIQuestion Q_EMAIL = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(str);
	        return matcher.find();
		}
	}.setQuestion("what is your email address? (sign up on http://iotaspam.com/account?p=signup)");
	
	public static final UIQuestion Q_NODES = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			Matcher matcher = VALID_NODE_ADDRESS_REGEX .matcher(str);
	        return matcher.find();
		}
	}.setQuestion("please enter the API port of the node you want to add [format: 'protocol://host:port', e.g. 'http://node.example.org:14265']");
	
	public static final UIQuestion Q_TIME_FORMAT = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			return true;
		}
	}.setQuestion("what time format do you wish for the logger [e.g. 'HH:mm:ss' or 'yyyy-MM-dd HH:mm:ss']").setCaseSensitive(true);
	
	
	private boolean hideInput = false, caseSensitive = false;
	private String question;
	
	public abstract boolean isAnswer(String str);
	
	public boolean hidesInput() {
		return hideInput;
	}
	
	public UIQuestion hideInput(boolean hideInput) {
		this.hideInput = hideInput;
		return this;
	}
	
	public UIQuestion setQuestion(String question) {
		this.question = question;
		return this;
	}

	public String getQuestion() {
		return question;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public UIQuestion setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
		return this;
	}
}