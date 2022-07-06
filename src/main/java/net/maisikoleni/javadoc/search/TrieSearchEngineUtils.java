package net.maisikoleni.javadoc.search;

import static net.maisikoleni.javadoc.util.regex.CharPredicate.caseIndependent;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.util.regex.CharClass;
import net.maisikoleni.javadoc.util.regex.Concatenation;
import net.maisikoleni.javadoc.util.regex.Literal;
import net.maisikoleni.javadoc.util.regex.Regex;
import net.maisikoleni.javadoc.util.regex.Star;

public final class TrieSearchEngineUtils {

	private static final String SKIP_CHAR = "~";
	private static final String SEPARATOR_CHAR_CLASS = ".,()<>/\\[\\]";
	private static final Pattern SEPARATORS = Pattern.compile("[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Pattern QUERY_SPLIT = Pattern
			.compile("(?<!" + SKIP_CHAR + ")\\b(?!" + SKIP_CHAR + ")|(?<=[" + SEPARATOR_CHAR_CLASS + "])");
	private static final Pattern IDENTIFIER_SPLIT = Pattern.compile("(?=[\\p{javaUpperCase}_])");
	private static final Pattern SKIP_TO_CHAR = Pattern.compile(SKIP_CHAR + "++");

	private static final Pattern QUERY_WHITESPACE = Pattern.compile("\\s++");
	private static final Pattern QUERY_UNECESSARY_SPACE = Pattern.compile(" \\B|\\B ");

	private static final Regex REGEX_WHITESPACE = new CharClass(caseIndependent(Character::isWhitespace), "\\s");
	private static final Regex REGEX_ANY_WHITESPACE = new Star(REGEX_WHITESPACE);
	private static final Regex REGEX_LOWER_CASE = new CharClass(caseIndependent(Character::isLowerCase),
			"\\p{javaLowerCase}");
	private static final Regex REGEX_ANY_LOWER_CASE = new Star(REGEX_LOWER_CASE);
	private static final Regex REGEX_ANY_NON_DIVIDER = new Star(
			new CharClass(caseIndependent(c -> c != '.' && c != '/'), "[^./]"));
	private static final Regex REGEX_WHITESPACE_WITH_ANY_LOWER = new Concatenation(REGEX_ANY_LOWER_CASE,
			new CharClass(caseIndependent(c -> c == '.' || c == '/' || Character.isWhitespace(c)), "[\\s/.]"));

	private static final Regex REGEX_USEFUL_CHARS = new Concatenation(
			new Star(new CharClass(c -> !Character.isAlphabetic(c) && !Character.isDigit(c), "[^\\p{Alnum}]")),
			new CharClass(c -> Character.isAlphabetic(c) || Character.isDigit(c), "[\\p{Alnum}]"),
			new Star(CharClass.ANY));
	private static final Regex REGEX_DIVIDER = new CharClass(c -> c == '.' || c == '/',
			"[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Regex REGEX_START_AFTER = new CharClass(c -> isSeparator(c) || c == '_' || c == ' ',
			"[" + SEPARATOR_CHAR_CLASS + "_ ]");
	private static final Regex REGEX_START_BEFORE = new CharClass(
			c -> isSeparator(c) || Character.isUpperCase(c) || c == '_',
			"[" + SEPARATOR_CHAR_CLASS + "_\\p{javaUpperCase}]");

	private static final char[] SPEPARATOR_CHARS = new char[] { '.', ',', '(', ')', '<', '>', '/', '[', ']' };

	private TrieSearchEngineUtils() {
	}

	static boolean isSeparator(char c) {
		for (int i = 0; i < SPEPARATOR_CHARS.length; i++)
			if (c == SPEPARATOR_CHARS[i])
				return true;
		return false;
	}

	static <T extends SearchableEntity> void subdivideEntity(T se, SubdividedEntityConsumer<T> consumer) {
		var preProcessedName = preProcessEntry(se.qualifiedName());
		consumer.accept(preProcessedName, se, 3.0);
		int length = preProcessedName.length();
		for (int i = 1; i < length; i++) {
			double rank = 0.0;
			if (REGEX_DIVIDER.matches(preProcessedName, i - 1, i) || REGEX_DIVIDER.matches(preProcessedName, i, i + 1))
				rank = 2.5;
			else if (REGEX_START_AFTER.matches(preProcessedName, i - 1, i)
					|| REGEX_START_BEFORE.matches(preProcessedName, i, i + 1))
				rank = 1.25;
			if (rank > 0.0) {
				var namePart = preProcessedName.subSequence(i, length);
				if (isUseful(namePart))
					consumer.accept(namePart, se, rank);
			}
		}
	}

	@FunctionalInterface
	interface SubdividedEntityConsumer<T> {
		void accept(CharSequence name, T entity, double rank);
	}

	static boolean isUseful(CharSequence cs) {
		return !cs.isEmpty() && (cs.length() > 2 || REGEX_USEFUL_CHARS.matches(cs));
	}

	static CharSequence preProcessEntry(CharSequence query) {
		var queryWithSpaces = QUERY_WHITESPACE.matcher(query).replaceAll(" ");
		return QUERY_UNECESSARY_SPACE.matcher(queryWithSpaces).replaceAll("");
	}

	static Regex generateRegex(CharSequence cleanQuery) {
		if (cleanQuery.isEmpty() || cleanQuery.length() == 1 && isSeparator(cleanQuery.charAt(0)))
			return new Concatenation();
		return Concatenation.of(Stream.of(QUERY_SPLIT.split(cleanQuery)).map(String::strip).map(part -> {
			// preserve "significant" whitespace between words
			if (part.isEmpty())
				return REGEX_WHITESPACE_WITH_ANY_LOWER;
			// keep separators as they are
			if (SEPARATORS.matcher(part).matches())
				return Concatenation.of(REGEX_ANY_LOWER_CASE, new Literal(part));
			// insert patterns for partial identifier matches
			return Stream.of(IDENTIFIER_SPLIT.split(part)).map(x ->
			// allow skipping lower case chars with ~ and use the next as char class
			Stream.of(SKIP_TO_CHAR.split(x)).map(Literal::new).collect(Concatenation.joining((prev, next) -> {
				var nextChar = next.chars().charAt(0);
				var charClass = new CharClass(caseIndependent(c -> c != nextChar && Character.isLowerCase(c)),
						"[^" + nextChar + "]");
				return Concatenation.of(charClass, new Star(charClass));
			}))).collect(Concatenation.joining(REGEX_ANY_LOWER_CASE));
		}).collect(
				Concatenation.joining(REGEX_ANY_WHITESPACE, (prev, next) -> next != REGEX_WHITESPACE_WITH_ANY_LOWER)),
				REGEX_ANY_NON_DIVIDER);
	}

	static Regex generateRegexFromQuery(String query) {
		var cleanQuery = preProcessEntry(query.strip());
		// TODO: keep?
		if (cleanQuery.toString().endsWith("^^"))
			cleanQuery = cleanQuery.subSequence(0, cleanQuery.length() - 2).toString().toUpperCase(Locale.ROOT);
		return generateRegex(cleanQuery);
	}
}
