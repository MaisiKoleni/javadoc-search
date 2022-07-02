package net.maisikoleni.javadoc.util.regex;

import java.util.Objects;
import java.util.regex.Pattern;

public record Literal(CharSequence chars) implements Regex {

	public Literal {
		Objects.requireNonNull(chars);
	}

	@Override
	@SuppressWarnings("all")
	public int matches(CharSequence s, int start) {
		int length = s.length();
		for (int i = 0; i < chars.length(); i++, start++) {
			if (length == start || s.charAt(start) != chars.charAt(i))
				return -1;
		}
		return start;
	}

	@Override
	public String toString() {
		return Pattern.quote(chars.toString());
	}

	@Override
	public <R> R accept(RegexVisitor<R> visitor) {
		return visitor.visit(this);
	}
}