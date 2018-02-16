package isf;

public enum P {
	
	GENERAL_VERSION("general", "version"),
	LOG_INTERVAL("log", "interval"),
	LOG_TIME_FORMAT("log", "time_format"),
	THREADS_AMOUNT("threads", "amount"),
	THREADS_PRIORITY("threads", "priority"),
	NODES_LIST("nodes", "node_list"),
	NODES_AMOUNT_ROTATION("nodes", "amount_rotation"),
	NODES_THIRD_PARTY_LIST("nodes", "third_party_node_list"),
	NODES_SYNC_CHECK_INTERVAL("nodes", "sync_check_interval"),
	SPAMFUND_EMAIL("spamfund", "email"),
	SPAMFUND_PASSWORD("spamfund", "password");
	
	public final String parent, name;
	
	P(String parent, String name) {
		this.parent = parent;
		this.name = name;
	}
}