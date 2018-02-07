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
	}.setQuestion("what is your password?").hideInput(true);
	
	public static final UIQuestion Q_EMAIL = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(str);
	        return matcher.find();
		}
	}.setQuestion("what is your email address?");
	
	public static final UIQuestion Q_NODES = new UIQuestion() {
		@Override
		public boolean isAnswer(String str) {
			Matcher matcher = VALID_NODE_ADDRESS_REGEX .matcher(str);
	        return matcher.find();
		}
	}.setQuestion("which node/nodes do you want to add?");
	
	
	private boolean hideInput = false;
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
}