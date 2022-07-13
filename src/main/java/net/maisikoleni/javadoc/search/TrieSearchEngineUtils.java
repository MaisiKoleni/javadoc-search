package net.maisikoleni.javadoc.search;

import static net.maisikoleni.javadoc.util.regex.CharPredicate.caseIndependent;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.util.regex.CharClass;
import net.maisikoleni.javadoc.util.regex.Concatenation;
import net.maisikoleni.javadoc.util.regex.Literal;
import net.maisikoleni.javadoc.util.regex.Regex;
import net.maisikoleni.javadoc.util.regex.Star;

public final class TrieSearchEngineUtils {

	static final char SEGMENT_DIVIDER_CHAR = '\u0378';
	static final String SEGMENT_DIVIDER_STRING = "" + SEGMENT_DIVIDER_CHAR;
	static {
		if (Character.isDefined(SEGMENT_DIVIDER_CHAR))
			throw new IllegalStateException("SEGMENT_DIVIDER_CHAR must be undefined");
	}
	static final String SEPARATOR_CHAR_CLASS = ".,()<>/\\[\\]";

	private static final String SKIP_CHAR = "~";
	private static final Pattern SEPARATORS = Pattern.compile("[" + SEPARATOR_CHAR_CLASS + "]");
	private static final Pattern QUERY_SPLIT = Pattern
			.compile("(?=[" + SEPARATOR_CHAR_CLASS + " ])|(?<=[" + SEPARATOR_CHAR_CLASS + " ])");
	private static final Pattern IDENTIFIER_SPLIT = Pattern.compile("(?=[\\p{javaUpperCase}_])");
	private static final Pattern SKIP_TO_CHAR = Pattern.compile(SKIP_CHAR + "++");

	private static final Pattern QUERY_WHITESPACE = Pattern.compile("\\s++");
	private static final Pattern QUERY_UNECESSARY_SPACE = Pattern.compile(" \\B(?!~)|\\B(?<!~) ");

	private static final Regex REGEX_WHITESPACE = new CharClass(caseIndependent(Character::isWhitespace), "\\s");
	private static final Regex REGEX_ANY_WHITESPACE = new Star(REGEX_WHITESPACE);
	private static final Regex REGEX_LOWER_CASE = new CharClass(caseIndependent(Character::isLowerCase),
			"\\p{javaLowerCase}");
	private static final Regex REGEX_ANY_LOWER_CASE = new Star(REGEX_LOWER_CASE);
	private static final Regex REGEX_ANY_LOWER_CASE_AND_OPTIONAL_DIVIDER = Concatenation.of(REGEX_ANY_LOWER_CASE,
			new Star(new Literal(SEGMENT_DIVIDER_STRING)));
	private static final Regex REGEX_ANY_NON_DIVIDER = new Star(
			new CharClass(caseIndependent(c -> c != SEGMENT_DIVIDER_CHAR), "[^" + SEGMENT_DIVIDER_CHAR + "]"));
	private static final Regex REGEX_WHITESPACE_WITH_ANY_LOWER = Concatenation.of(
			REGEX_ANY_LOWER_CASE_AND_OPTIONAL_DIVIDER,
			new CharClass(caseIndependent(c -> c == '.' || c == '/' || Character.isWhitespace(c)), "[\\s/.]"));

	private static final Regex REGEX_USEFUL_CHARS = Concatenation.of(
			new Star(new CharClass(c -> !Character.isAlphabetic(c) && !Character.isDigit(c), "[^\\p{Alnum}]")),
			new CharClass(c -> Character.isAlphabetic(c) || Character.isDigit(c), "[\\p{Alnum}]"),
			new Star(CharClass.ANY));

	private static final char[] SPEPARATOR_CHARS = new char[] { '.', ',', '(', ')', '<', '>', '/', '[', ']' };

	private TrieSearchEngineUtils() {
	}

	static boolean isSeparator(char c) {
		for (int i = 0; i < SPEPARATOR_CHARS.length; i++)
			if (c == SPEPARATOR_CHARS[i])
				return true;
		return false;
	}

	static boolean isSeparator(String s) {
		return s.length() == 1 && isSeparator(s.charAt(0));
	}

	static boolean isUseful(CharSequence cs) {
		return !cs.isEmpty() && (cs.length() > 2 || REGEX_USEFUL_CHARS.matches(cs));
	}

	static Regex generateRegexFromQuery(String query) {
		if (query.indexOf(SEGMENT_DIVIDER_CHAR) >= 0)
			throw new IllegalArgumentException("query must not contain the segment divider char");
		var cleanQuery = preProcessEntry(query.strip());
		// TODO: keep?
		if (cleanQuery.toString().endsWith("^^"))
			cleanQuery = cleanQuery.subSequence(0, cleanQuery.length() - 2).toString().toUpperCase(Locale.ROOT);
		return generateRegex(cleanQuery);
	}

	static CharSequence preProcessEntry(CharSequence query) {
		var queryWithSpaces = QUERY_WHITESPACE.matcher(query).replaceAll(" ");
		return QUERY_UNECESSARY_SPACE.matcher(queryWithSpaces).replaceAll("");
	}

	static Regex generateRegex(CharSequence cleanQuery) {
		if (isInsufficientQuery(cleanQuery))
			return new Concatenation();
		return Concatenation.of(Stream.of(QUERY_SPLIT.split(cleanQuery)).map(String::strip).map(part -> {
			// preserve "significant" whitespace between words
			if (part.isEmpty())
				return REGEX_WHITESPACE_WITH_ANY_LOWER;
			// keep separators as they are
			if (SEPARATORS.matcher(part).matches())
				return Concatenation.of(REGEX_ANY_LOWER_CASE_AND_OPTIONAL_DIVIDER, new Literal(part));
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

	static boolean isInsufficientQuery(CharSequence cleanQuery) {
		return cleanQuery.isEmpty() || cleanQuery.length() == 1
				&& (isSeparator(cleanQuery.charAt(0)) || SKIP_CHAR.charAt(0) == cleanQuery.charAt(0));
	}

	static CharSequence generateTrieName(SearchableEntity se) {
		var nameWithSegmentDividers = se.qualifiedNameSegments().<String>mapMulti((segment, sink) -> {
			if (isSeparator(segment))
				sink.accept(SEGMENT_DIVIDER_STRING);
			sink.accept(segment);
		}).collect(Collectors.joining());
		return preProcessEntry(nameWithSegmentDividers);
	}
}
