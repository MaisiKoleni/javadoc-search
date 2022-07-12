package net.maisikoleni.javadoc.search;

import java.util.List;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;
import net.maisikoleni.javadoc.util.ConcurrentTrie;
import net.maisikoleni.javadoc.util.Trie;
import net.maisikoleni.javadoc.util.regex.CompiledRegex;
import net.maisikoleni.javadoc.util.regex.GradingLongStepMatcher;

public final class TrieSearchEngine extends IndexBasedSearchEngine {

	private final Trie<SearchableEntity> all;
	private final Trie<Module> modules;
	private final Trie<Package> packages;
	private final Trie<Type> types;
	private final Trie<Member> members;
	private final Trie<Tag> tags;

	public TrieSearchEngine(JavadocIndex index) {
		super(index);
		var generator = new ConcurrentTrieGenerator();
		all = generator.generateTrie(index.stream());
		modules = generator.generateTrie(index.modules());
		packages = generator.generateTrie(index.packages());
		types = generator.generateTrie(index.types());
		members = generator.generateTrie(index.members());
		tags = generator.generateTrie(index.tags());
	}

	static final class ConcurrentTrieGenerator extends TrieGenerator {

		ConcurrentTrieGenerator() {
			super(true);
		}

		<S extends SearchableEntity> Trie<S> generateTrie(List<S> index) {
			return generateTrie(index.stream());
		}

		<S extends SearchableEntity> Trie<S> generateTrie(Stream<S> index) {
			return super.generateTrie(index.parallel(), ConcurrentTrie::new, SubdividedEntityFunction.identity());
		}
	}

	@Override
	public Stream<SearchableEntity> search(String query) {
		var precessedQuery = compileMatcher(query);
		return search(all, precessedQuery);
	}

	@Override
	public GroupedSearchResult searchGroupedByType(String query) {
		var precessedQuery = compileMatcher(query);
		return new GroupedSearchResult(search(modules, precessedQuery), search(packages, precessedQuery),
				search(types, precessedQuery), search(members, precessedQuery), search(tags, precessedQuery));
	}

	private static GradingLongStepMatcher compileMatcher(String query) {
		var simpleRegex = TrieSearchEngineUtils.generateRegexFromQuery(query);
		return CompiledRegex.compile(simpleRegex);
	}

	private static <T extends SearchableEntity> Stream<T> search(Trie<T> trie, GradingLongStepMatcher matcher) {
		return trie.search(matcher);
	}
}
