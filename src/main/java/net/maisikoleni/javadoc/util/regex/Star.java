package net.maisikoleni.javadoc.util.regex;

import java.util.Objects;

public record Star(Regex regex) implements Regex {

	public Star {
		Objects.requireNonNull(regex);
	}

	@Override
	public int matches(CharSequence s, int start) {
		int pos = start;
		int newPos;
		while ((newPos = regex.matches(s, pos)) >= 0) {
			pos = newPos;
		}
		return pos;
	}

	@Override
	public String toString() {
		return "(" + regex + ")*+";
	}
}