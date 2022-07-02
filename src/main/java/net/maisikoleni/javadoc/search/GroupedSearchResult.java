package net.maisikoleni.javadoc.search;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;

public record GroupedSearchResult(Stream<Module> modules, Stream<Package> packages, Stream<Type> types,
		Stream<Member> members, Stream<Tag> tags) {

	public static GroupedSearchResult empty() {
		return new GroupedSearchResult(Stream.of(), Stream.of(), Stream.of(), Stream.of(), Stream.of());
	}

	public GroupedSearchResult ifEmptyTry(Supplier<GroupedSearchResult> other) {
		var modulesIt = modules.iterator();
		var packagesIt = packages.iterator();
		var typesIt = types.iterator();
		var membersIt = members.iterator();
		var tagsIt = tags.iterator();
		if (modulesIt.hasNext() || packagesIt.hasNext() || typesIt.hasNext() || membersIt.hasNext() || tagsIt.hasNext())
			return new GroupedSearchResult(iteratorToStream(modulesIt), iteratorToStream(packagesIt),
					iteratorToStream(typesIt), iteratorToStream(membersIt), iteratorToStream(tagsIt));
		return other.get();
	}

	static <T> Stream<T> iteratorToStream(Iterator<T> it) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,
				Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
	}

	static <T> Stream<T> ifEmptyTry(Stream<T> firstChoice, Supplier<Stream<T>> secondChoice) {
		var it = firstChoice.iterator();
		if (it.hasNext())
			return iteratorToStream(it);
		return secondChoice.get();
	}
}
