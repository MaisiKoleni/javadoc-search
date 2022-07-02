package net.maisikoleni.javadoc.util.regex;

import java.util.Objects;

public record CharClass(CharPredicate predicate, String pattern) implements Regex {

	public static final CharClass ANY = new CharClass(c -> true, ".");

	public CharClass {
		Objects.requireNonNull(predicate);
	}

	@Override
	public int matches(CharSequence s, int start) {
		if (start < s.length() && predicate.test(s.charAt(start)))
			return start + 1;
		return -1;
	}

	@Override
	public String toString() {
		return pattern;
	}

	@Override
	public <R> R accept(RegexVisitor<R> visitor) {
		return visitor.visit(this);
	}
}