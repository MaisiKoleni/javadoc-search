package net.maisikoleni.javadoc;

import java.util.regex.Pattern;

import net.maisikoleni.javadoc.server.JavadocSearch;

public final class Constants {

	public static final String PACKAGE = "net.maisikoleni.javadoc.search";

	public static final String DB_PATH_DEFAULT = "database";

	public static final String QUERY_LENGHT_LIMIT_DEFAULT = "1000";

	public static final String SUGGESTION_COUNT_DEFAULT = "" + JavadocSearch.DEFAULT_COUNT;

	public static final String SUGGESTION_COUNT_LIMIT_DEFAULT = "50";

	public static final String LOG_SEARCH_THRESHOLD_NANOS_DEFAULT = "" + 20_000_000;

	public static final String LIBRARY_ID_PATTERN_STRING = "[\\p{Alnum}_-]{2,13}";
	public static final Pattern LIBRARY_ID_PATTERN = Pattern.compile(LIBRARY_ID_PATTERN_STRING);

	public static final String LIBRARY_ID_PARAMETER = "libraryId";

	public static final int HTTP_CACHE_MAX_AGE = 60;

	private Constants() {
	}
}
