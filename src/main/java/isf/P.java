package isf;

import org.ini4j.Profile.Section;
import org.ini4j.Wini;

public enum P {
	
	GENERAL_VERSION(Main.buildFullVersion()),
	LOG_COLORS_ENABLED("true"),
	LOG_INTERVAL("60"),
	LOG_PERFORMANCE_REPORT_INTERVAL("180"),
	LOG_TIME_FORMAT("HH:mm:ss"),
	POW_CORES(""+Math.max(1, Runtime.getRuntime().availableProcessors()-1)),
	POW_ABORT_TIME("30"),
	POW_USE_GO_MODULE("true"),
	SPAM_DEPTH("3"),
	NODES_LIST(""),
	NODES_AMOUNT_ROTATION("20"),
	NODES_THIRD_PARTY_NODE_LIST("true"),
	NODES_SYNC_CHECK_INTERVAL("300"),
	SPAMFUND_EMAIL(""),
	SPAMFUND_PASSWORD("");
	
	public final String parent, name;
	public final String defaultValue;
	
	P(String defaultValue) {
		String enumName = toString();
		parent = enumName.split("_")[0];
		name = enumName.substring(parent.length()+1, enumName.length());
		this.defaultValue = defaultValue;
	}
	
	public Object get(Wini wini) {
		Section sec = wini.getOrDefault(parent, null);
		if(sec == null) return defaultValue;
		return sec.getOrDefault(name, defaultValue);
	}
}