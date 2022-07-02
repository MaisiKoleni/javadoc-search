package net.maisikoleni.javadoc.search;

import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public interface PatternLookup<T> {

	Stream<T> search(CharSequence cs);

	Stream<T> search(GradingLongStepMatcher matcher);

	void insert(CharSequence cs, T value);
}
