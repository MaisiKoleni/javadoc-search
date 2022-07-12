package net.maisikoleni.javadoc.search;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;

public final class RegexSearchEngine extends IndexBasedSearchEngine {

	private static final Logger LOG = LoggerFactory.getLogger(RegexSearchEngine.class);

	private static final String SEPARATOR_CHARS = ".,()<>/\\[\\]";
	private static final String INBETWEEN_CHARS = "[^\\p{javaUpperCase}\\p{Space}_" + SEPARATOR_CHARS + "]*";
	private static final Pattern SEPARATORS = Pattern.compile("[" + SEPARATOR_CHARS + "]");
	private static final Pattern QUERY_SPLIT = Pattern.compile("\\b|(?<=[" + SEPARATOR_CHARS + "])");
	private static final Pattern IDENTIFIER_SPLIT = Pattern.compile("(?=[\\p{javaUpperCase}_])");

	private final List<MatchableEntity<Module>> modules;
	private final List<MatchableEntity<Package>> packages;
	private final List<MatchableEntity<Type>> types;
	private final List<MatchableEntity<Member>> members;
	private final List<MatchableEntity<Tag>> tags;

	public RegexSearchEngine(JavadocIndex index) {
		super(index);
		modules = generateMatchableEntities(index.modules());
		packages = generateMatchableEntities(index.packages());
		types = generateMatchableEntities(index.types());
		members = generateMatchableEntities(index.members());
		tags = generateMatchableEntities(index.tags());
	}

	@SuppressWarnings("unchecked")
	private Stream<MatchableEntity<SearchableEntity>> streamOfAll() {
		// OK because entity attribute is read only
		return (Stream<MatchableEntity<SearchableEntity>>) Stream.of(modules, packages, types, members, tags)
				.flatMap(List::stream);
	}

	private static <T extends SearchableEntity> List<MatchableEntity<T>> generateMatchableEntities(List<T> index) {
		return index.stream().map(entity -> {
			var qualifiedName = entity.qualifiedName();
			var matchThresholdIndex = Math.max(0, qualifiedName.length() - (entity.name().length() + 1));
			return new MatchableEntity<>(qualifiedName, matchThresholdIndex, entity);
		}).toList();
	}

	record MatchableEntity<T extends SearchableEntity> (CharSequence idenifier, int matchThresholdIndex, T entity) {

		MatchableEntity {
			Objects.checkIndex(matchThresholdIndex, idenifier.length());
			Objects.requireNonNull(entity);
		}

		Optional<Match<T>> matchAndRank(Pattern queryPattern) {
			var matcher = queryPattern.matcher(idenifier);
			if (!matcher.matches())
				return Match.none();
			var groupCount = matcher.groupCount();
			assert groupCount > 0 : "at least one group is required";
			var result = matcher.toMatchResult();
			var queryMatchEnd = result.end(1);
			/*
			 * Does not comply completely, as "." now finds types (package.type), but it
			 * does not in the official Javadoc (that returns only 'All Classes and
			 * Interfaces'). But "/" will both here and in Javadoc find module/package
			 * combinations, so this is not consistent anyways.
			 */
			if (queryMatchEnd <= matchThresholdIndex)
				return Match.none();
			/*
			 * The score is the number of fully specified identifiers or parts (apparently,
			 * the start is better than the whole part at the end: "SettingControl" is a
			 * better match for "Set" than "AbstractSet")
			 */
			int score = 0;
			if (result.start(1) == 0 || SEPARATOR_CHARS.indexOf(idenifier.charAt(result.start(1) - 1)) >= 0)
				score++;
			for (int i = 2; i < groupCount; i++) {
				var isEmpty = result.start(i) == result.end(i);
				if (isEmpty)
					score++;
			}
			return Match.of(entity, score);
		}
	}

	record Match<T extends SearchableEntity> (T entity, int score) implements Comparable<Match<T>> {

		@Override
		public int compareTo(Match<T> o) {
			return -Integer.compare(score, o.score);
		}

		static <T extends SearchableEntity> Optional<Match<T>> none() {
			return Optional.empty();
		}

		static <T extends SearchableEntity> Optional<Match<T>> of(T entity, int score) {
			return Optional.of(new Match<>(entity, score));
		}
	}

	@Override
	public Stream<SearchableEntity> search(String query) {
		if (query == null || query.isBlank())
			return Stream.of();
		return searchIn(streamOfAll(), compileQueryPattern(query));
	}

	@Override
	public GroupedSearchResult searchGroupedByType(String query) {
		if (query == null || query.isBlank())
			return GroupedSearchResult.empty();
		Pattern queryPattern = compileQueryPattern(query);
		return new GroupedSearchResult(searchIn(modules.stream(), queryPattern),
				searchIn(packages.stream(), queryPattern), searchIn(types.stream(), queryPattern),
				searchIn(members.stream(), queryPattern), searchIn(tags.stream(), queryPattern));
	}

	private static <T extends SearchableEntity> Stream<T> searchIn(Stream<MatchableEntity<T>> entitiyIndex,
			Pattern queryPattern) {
		return entitiyIndex.map(x -> x.matchAndRank(queryPattern)).filter(Optional::isPresent).map(Optional::get)
				.sorted().map(Match::entity);
	}

	public static Pattern compileQueryPattern(String query) {
		String pattern = assembleQueryPatternString(query);
		return Pattern.compile(pattern);
	}

	private static String assembleQueryPatternString(String query) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("^(?:|.*(?<=[" + SEPARATOR_CHARS + "_])|.*(?=[\\p{javaUpperCase}_" + SEPARATOR_CHARS + "]))(");
		// this is too strict with whitespace splitting
		pattern.append(Stream.of(QUERY_SPLIT.split(query.strip())).map(String::strip).map(part -> {
			// preserve "significant" whitespace between words
			if (part.isEmpty())
				return "\\s";
			// keep separators as they are
			if (SEPARATORS.matcher(part).matches())
				return "(" + INBETWEEN_CHARS + "+)" + Pattern.quote(part);
			// insert patterns for partial identifier matches
			return Stream.of(IDENTIFIER_SPLIT.split(part.strip())).map(Pattern::quote)
					.collect(Collectors.joining("(" + INBETWEEN_CHARS + "+)"));
		}).collect(Collectors.joining("\\s*+")));
		pattern.append(").*+");
		String finalPattern = pattern.toString().replace("\\s*+\\s", "\\s");
		LOG.debug("pattern: {}", finalPattern);
		return finalPattern;
	}
}
