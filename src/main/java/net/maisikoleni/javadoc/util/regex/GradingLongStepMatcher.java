package net.maisikoleni.javadoc.util.regex;

/**
 * A matcher for character sequences that operates in steps and stores all its
 * state in a single <code>long</code> value that is transparent to the user.
 * <p>
 * The matcher instance itself must be stateless and should be immutable.
 * <p>
 * In addition to {@link LongStepMatcher}, it provides a method to grade a
 * successful match.
 *
 * @author Christian Femers
 */
public interface GradingLongStepMatcher extends LongStepMatcher {

	/**
	 * Grades the given state.
	 * <p>
	 * The grade is a value between <code>0.0</code> and <code>1.0</code>. A higher
	 * value corresponds to a better grade.
	 *
	 * @param state the state to check. Must originate from this
	 *              {@link GradingLongStepMatcher} instance.
	 * @return a double value between <code>0.0</code> and <code>1.0</code> (both
	 *         inclusive)
	 */
	double grade(long state);
}
