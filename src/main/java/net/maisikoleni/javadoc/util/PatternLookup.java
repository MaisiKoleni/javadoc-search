package net.maisikoleni.javadoc.util;

import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public interface PatternLookup<T> {

	Stream<T> search(CharSequence cs);

	Stream<T> search(GradingLongStepMatcher matcher);

	void insert(CharSequence cs, T value);
}
