package net.maisikoleni.javadoc.util.regex;

/**
 * A matcher for character sequences that operates in steps and stores all its
 * state in a single <code>long</code> value that is transparent to the user.
 * <p>
 * The matcher instance itself must be stateless and should be immutable.
 *
 * @author Christian Femers
 */
public interface LongStepMatcher {

	/**
	 * Returns the start state for this long step matcher.
	 *
	 * @return a constant long value that must be used as start state
	 */
	long getStartState();

	/**
	 * Returns whether the given {@link CharSequence} matches.
	 *
	 * @param cs the {@link CharSequence} to match
	 * @return <code>true</code> if <code>cs</code> matches, <code>false</code>
	 *         otherwise
	 */
	boolean matches(CharSequence cs);

	/**
	 * Returns whether the given state is a matching state.
	 *
	 * @param state the state to check. Must originate from this
	 *              {@link LongStepMatcher} instance.
	 * @return <code>true</code> if the given state is a final one and thereby
	 *         resulting in a successful matching, <code>false</code> otherwise
	 */
	boolean isMatch(long state);

	/**
	 * Returns whether the given state is OK and not a definite matching failure.
	 * <p>
	 * This is different to {@link #isMatch(long)} as intermediate states are also
	 * OK because they can still transition a match and lead to a state for which
	 * {@link #isMatch(long)} returns <code>true</code>.
	 *
	 * @param state the state to check. Must originate from this
	 *              {@link LongStepMatcher} instance.
	 * @return <code>true</code> if the given state <b>could</b> transition into a
	 *         state indicating a successful matching, <code>false</code> otherwise
	 */
	boolean isOk(long state);

	/**
	 * Advances the step matcher by one step, consuming the given character.
	 *
	 * @param c     the character to match
	 * @param state the current state
	 * @return the new state after the character got matched (or couldn't get
	 *         matched)
	 */
	long step(char c, long state);

	/**
	 * Advances the step matcher by <code>end-start</code> steps, consuming the
	 * characters of the given {@link CharSequence} in the interval
	 * <code>[start,end)</code>.
	 *
	 * @param cs    the {@link CharSequence} to use as a character source
	 * @param state the current state
	 * @param start the start index in <code>cs</code>, inclusive
	 * @param end   the end index in <code>cs</code>, exclusive
	 * @return the new state after the characters got matched (or couldn't get
	 *         matched)
	 */
	long stepThrough(CharSequence cs, long state, int start, int end);

	/**
	 * Starts the step matcher and advances it by the number of steps equivalent to
	 * the length of the given {@link CharSequence}, consuming all of its
	 * characters.
	 * <p>
	 * This is equivalent to
	 * <code>stepThrough(cs, getStartState(), 0, cs.length())</code>.
	 *
	 * @param cs the {@link CharSequence} to use as a character source
	 * @return the state after all the characters got matched (or couldn't get
	 *         matched), starting from the {@linkplain #getStartState() start
	 *         state}.
	 * @see #stepThrough(CharSequence, long, int, int)
	 */
	default long stepThrough(CharSequence cs) {
		return stepThrough(cs, getStartState(), 0, cs.length());
	}

	/**
	 * Advances the step matcher by the number of steps equivalent to the length of
	 * the given {@link CharSequence}, consuming all of its characters.
	 * <p>
	 * This is equivalent to <code>stepThrough(cs, state, 0, cs.length())</code>.
	 *
	 * @param cs    the {@link CharSequence} to use as a character source
	 * @param state the current state
	 * @return the new state after all the characters got matched (or couldn't get
	 *         matched)
	 * @see #stepThrough(CharSequence, long, int, int)
	 */
	default long stepThrough(CharSequence cs, long state) {
		return stepThrough(cs, state, 0, cs.length());
	}
}
