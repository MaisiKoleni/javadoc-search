package net.maisikoleni.javadoc.util.regex;

import java.util.Objects;
import java.util.regex.Pattern;

public record Literal(CharSequence chars) implements Regex {

	public Literal {
		Objects.requireNonNull(chars);
	}

	@Override
	public int matches(CharSequence s, int start) {
		int length = s.length();
		int pos = start;
		for (int i = 0; i < chars.length(); i++, pos++) {
			if (length == pos || s.charAt(pos) != chars.charAt(i))
				return -1;
		}
		return pos;
	}

	@Override
	public String toString() {
		return Pattern.quote(chars.toString());
	}
}