package net.maisikoleni.javadoc.util.regex;

/**
 * Simple regular expression structures, everything is possessive.
 *
 * @author Christian Femers
 */
public sealed interface Regex permits Concatenation, Literal, CharClass, Star {

	default boolean matches(CharSequence s) {
		return matches(s, 0) == s.length();
	}

	default boolean matches(CharSequence s, int start, int end) {
		return matches(s, start) == end;
	}

	int matches(CharSequence s, int start);

	<R> R accept(RegexVisitor<R> visitor);
}