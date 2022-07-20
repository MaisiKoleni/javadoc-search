package net.maisikoleni.javadoc.util.regex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import net.maisikoleni.javadoc.db.TestJavadocIndexes;
import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.jdk18.Jdk18Javadoc;

class RegexTest {

	private static final int LOOPS = 10;

	private static JavadocIndex index;
	private static String[] names;

	private Regex regex;
	private CompiledRegex compiledRegex;
	private Pattern jdkPattern;

	@BeforeAll
	static void initializeIndex() {
		index = new Jdk18Javadoc(new TestJavadocIndexes()).index();
		names = index.stream().map(SearchableEntity::qualifiedName).toArray(String[]::new);
	}

	@BeforeEach
	void createRegexesAndPatterns() {
		var lowerCaseChar = new CharClass(Character::isLowerCase, "\\p{javaLowerCase}");
		var nonDivider = new CharClass(c -> c != '.' && c != '/', "[^./]");
		var anyLower = new Star(lowerCaseChar);
		regex = new Concatenation(new Literal("jav"), anyLower, new Literal(".base"), anyLower, new Literal("/j"),
				anyLower, new Literal("."), anyLower, new Literal(".Set"), new Star(nonDivider));
		compiledRegex = CompiledRegex.compile(regex, false);
		jdkPattern = Pattern.compile(regex.toString());
	}

	@Test
	void testRegexToStringIsPattern() {
		assertThat(regex).hasToString("""
				\\Qjav\\E(\\p{javaLowerCase})*+\
				\\Q.base\\E(\\p{javaLowerCase})*+\
				\\Q/j\\E(\\p{javaLowerCase})*+\
				\\Q.\\E(\\p{javaLowerCase})*+\
				\\Q.Set\\E([^./])*+\
				""");
	}

	@Test
	void testRegexMatchingEquivalence() {
		for (var name : names) {
			// Custom regex
			boolean resultRegex = regex.matches(name);
			// JDK Pattern
			var matcher = jdkPattern.matcher(name);
			boolean resultJdkPattern = matcher.matches();
			// Custom compiled regex
			long finalState = compiledRegex.stepThrough(name);
			boolean resultCompiledRegex = compiledRegex.isMatch(finalState);

			assertThat(resultJdkPattern).as("Name '%s' matches are equal", name).isEqualTo(resultRegex)
					.isEqualTo(resultCompiledRegex);

			if (resultCompiledRegex) {
				var compiledRegexEmptyStars = compiledRegex.getEmptyStarCount(finalState);
				var jdkMatcherEmptyStars = IntStream.rangeClosed(1, matcher.groupCount())
						.filter(g -> matcher.start(g) == matcher.end(g)).count();
				assertThat(compiledRegexEmptyStars).as("Empty star count for '%s' match is equal", name)
						.isEqualTo(jdkMatcherEmptyStars);
			}
		}
	}

	@Test
	@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true", //
			disabledReason = "GitHub Actions cannot do this reliably due to weaker and non-dedicated system")
	void testRegexMatchingPerformance(TestReporter reporter) {
		long timeRegex = 0;
		long timeJdkPattern = 0;
		long timeCompiledRegex = 0;

		for (int i = 0; i < LOOPS; i++) {
			for (var name : names) {
				// Custom regex
				timeRegex -= System.nanoTime();
				regex.matches(name);
				timeRegex += System.nanoTime();
				// JDK Pattern
				timeJdkPattern -= System.nanoTime();
				jdkPattern.matcher(name).matches();
				timeJdkPattern += System.nanoTime();
				// Custom compiled regex
				timeCompiledRegex -= System.nanoTime();
				compiledRegex.isMatch(compiledRegex.stepThrough(name));
				timeCompiledRegex += System.nanoTime();
			}
		}

		reporter.publishEntry(Map.of( //
				"regex.time.custom", String.valueOf(timeRegex), //
				"regex.time.jdkPattern", String.valueOf(timeJdkPattern), //
				"regex.time.compiledRegex", String.valueOf(timeCompiledRegex)));

		assertThat(timeRegex).isLessThan(timeJdkPattern);
		assertThat(timeCompiledRegex).isLessThan(timeRegex);
	}

}
