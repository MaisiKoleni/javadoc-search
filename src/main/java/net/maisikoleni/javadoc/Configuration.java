package net.maisikoleni.javadoc;

import net.maisikoleni.javadoc.server.JavadocSearch;

public final class Configuration {

	public static final String PACKAGE = "net.maisikoleni.javadoc.search";

	public static final String DB_PATH_KEY = PACKAGE + ".db.path";
	public static final String DB_PATH_DEFAULT = "database";

	public static final String QUERY_LENGHT_LIMIT_KEY = PACKAGE + ".server.query-char-limit";
	public static final String QUERY_LENGHT_LIMIT_DEFAULT = "1000";

	public static final String SUGGESTION_COUNT_KEY = PACKAGE + ".server.html.suggestion-count";
	public static final String SUGGESTION_COUNT_DEFAULT = "" + JavadocSearch.DEFAULT_COUNT;

	public static final String SUGGESTION_COUNT_LIMIT_KEY = PACKAGE + ".server.suggestion-count-limit";
	public static final String SUGGESTION_COUNT_LIMIT_DEFAULT = "50";

	private Configuration() {
	}
}
