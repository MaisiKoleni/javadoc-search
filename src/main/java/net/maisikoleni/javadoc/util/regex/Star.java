package net.maisikoleni.javadoc.util.regex;

import java.util.Objects;

public record Star(Regex regex) implements Regex {

	public Star {
		Objects.requireNonNull(regex);
	}

	@Override
	@SuppressWarnings("all")
	public int matches(CharSequence s, int start) {
		int newStart;
		while ((newStart = regex.matches(s, start)) >= 0) {
			start = newStart;
		}
		return start;
	}

	@Override
	public String toString() {
		return "(?:" + regex + ")*+";
	}

	@Override
	public <R> R accept(RegexVisitor<R> visitor) {
		return visitor.visit(this);
	}
}