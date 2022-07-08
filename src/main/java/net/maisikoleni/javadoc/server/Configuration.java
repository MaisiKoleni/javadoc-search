package net.maisikoleni.javadoc.server;

public final class Configuration {

	public static final String PACKAGE = "net.maisikoleni.javadoc.search.server";

	public static final String QUERY_LENGHT_LIMIT_KEY = PACKAGE + ".query-char-limit";
	public static final String QUERY_LENGHT_LIMIT_DEFAULT = "1000";

	public static final String SUGGESTION_COUNT_KEY = PACKAGE + ".html.suggestion-count";
	public static final String SUGGESTION_COUNT_DEFAULT = "" + JavadocSearch.DEFAULT_COUNT;

	public static final String SUGGESTION_COUNT_LIMIT_KEY = PACKAGE + ".suggestion-count-limit";
	public static final String SUGGESTION_COUNT_LIMIT_DEFAULT = "50";

	private Configuration() {
	}
}
