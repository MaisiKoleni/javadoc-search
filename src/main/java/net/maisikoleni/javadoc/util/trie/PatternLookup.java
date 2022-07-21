package net.maisikoleni.javadoc.util.trie;

import java.util.stream.Stream;

import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public interface PatternLookup<T> {

	Stream<T> search(CharSequence key);

	Stream<T> search(GradingLongStepMatcher matcher);

	void insert(CharSequence key, T value);
}
