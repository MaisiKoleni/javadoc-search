package net.maisikoleni.javadoc.util.regex;

import java.util.function.Predicate;

@FunctionalInterface
public interface CharPredicate extends Predicate<Character> {

	boolean test(char c);

	@Override
	default boolean test(Character c) {
		return test(c);
	}

	default CharPredicate ignoreCase() {
		return c -> test(Character.toLowerCase(c)) || test(Character.toUpperCase(c));
	}

	static CharPredicate caseIndependent(CaseIndependentCharPredicate caseIndependentCharPredicate) {
		return caseIndependentCharPredicate;
	}

	static CharPredicate caseDependent(CharPredicate regular, CaseIndependentCharPredicate ignoreCase) {
		return new CharPredicate() {
			@Override
			public boolean test(char c) {
				return regular.test(c);
			}

			@Override
			public CharPredicate ignoreCase() {
				return ignoreCase;
			}
		};
	}

	@FunctionalInterface
	interface CaseIndependentCharPredicate extends CharPredicate {

		@Override
		default CharPredicate ignoreCase() {
			return this;
		}
	}
}