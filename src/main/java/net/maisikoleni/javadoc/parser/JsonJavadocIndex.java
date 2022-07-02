package net.maisikoleni.javadoc.parser;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.entities.Member;
import net.maisikoleni.javadoc.entities.Module;
import net.maisikoleni.javadoc.entities.Package;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.entities.Tag;
import net.maisikoleni.javadoc.entities.Type;

public record JsonJavadocIndex(List<JsonModule> modules, List<JsonPackage> packages, List<JsonType> types,
		List<JsonMember> members, List<JsonTag> tags) {

	public Stream<JsonSearchableEntity> stream() {
		return Stream.of(modules, packages, types, members, tags).flatMap(List::stream);
	}

	public JavadocIndex toJavadocIndex() {
		var modules = modules().stream().map(m -> new Module(m.l())).toList();
		var packages = packages().stream().map(p -> new Package(findByName(modules, p.m()), p.l(), p.u())).toList();
		var types = types().stream().map(t -> new Type(findByName(packages, t.p()), t.l(), t.u())).toList();
		var members = members().stream().map(m -> new Member(findType(types, m.p(), m.c()), m.l(), m.u())).toList();
		var tags = tags().stream().map(t -> new Tag(t.h(), t.l(), t.d(), t.u())).toList();
		return new JavadocIndex(modules, packages, types, members, tags);
	}

	private static <T extends SearchableEntity> T findByName(Collection<T> options, String name) {
		return options.stream().filter(se -> Objects.equals(name, se.name())).findFirst().orElse(null);
	}

	private static Type findType(Collection<Type> options, String packageName, String typeName) {
		return options.stream()
				.filter(se -> Objects.equals(typeName, se.name()) && Objects.equals(packageName, se.container().name()))
				.findFirst().orElse(null);
	}
}
