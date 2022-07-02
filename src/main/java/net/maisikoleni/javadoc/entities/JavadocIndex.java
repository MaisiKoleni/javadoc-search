package net.maisikoleni.javadoc.entities;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import net.maisikoleni.javadoc.parser.JavaScriptIndexParser;

public record JavadocIndex(List<Module> modules, List<Package> packages, List<Type> types, List<Member> members,
		List<Tag> tags) {

	static final String MODULE_SEARCH_INDEX = "module-search-index.js";
	static final String PACKAGE_SEARCH_INDEX = "package-search-index.js";
	static final String TYPE_SEARCH_INDEX = "type-search-index.js";
	static final String MEMBER_SEARCH_INDEX = "member-search-index.js";
	static final String TAG_SEARCH_INDEX = "tag-search-index.js";

	@SuppressWarnings("unchecked")
	public Stream<SearchableEntity> stream() {
		return (Stream<SearchableEntity>) Stream.of(modules, packages, types, members, tags).map(List::stream)
				.reduce(Stream::concat).get();
	}

	public static JavadocIndex loadFromUrl(URI javadocLocation) {
		return loadFromInputStreams(indexName -> javadocLocation.resolve(indexName).toURL().openStream());
	}

	public static JavadocIndex loadAsResources(Class<?> resourceLocation) {
		return loadFromInputStreams(resourceLocation::getResourceAsStream);
	}

	public static JavadocIndex loadFromInputStreams(IndexResourceResolver resolver) {
		ExecutorService executor = Executors.newFixedThreadPool(5);
		var moduleSearchIndex = executor.submit(() -> getIndexAsString(resolver, MODULE_SEARCH_INDEX));
		var packageSearchIndex = executor.submit(() -> getIndexAsString(resolver, PACKAGE_SEARCH_INDEX));
		var typeSearchIndex = executor.submit(() -> getIndexAsString(resolver, TYPE_SEARCH_INDEX));
		var memberSearchIndex = executor.submit(() -> getIndexAsString(resolver, MEMBER_SEARCH_INDEX));
		var tagSearchIndex = executor.submit(() -> getIndexAsString(resolver, TAG_SEARCH_INDEX));
		try {
			executor.shutdown();
			if (!executor.awaitTermination(10, TimeUnit.SECONDS))
				throw new UncheckedIOException(new IOException("serach index loading/reading timed out"));
			var jsonIndex = JavaScriptIndexParser.parseJavaScriptIndexes(moduleSearchIndex.get(),
					packageSearchIndex.get(), typeSearchIndex.get(), memberSearchIndex.get(), tagSearchIndex.get());
			return jsonIndex.toJavadocIndex();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	@FunctionalInterface
	interface IndexResourceResolver {
		InputStream resolve(String indexName) throws IOException;
	}

	static String getIndexAsString(IndexResourceResolver resolver, String name) {
		try (var resourceStream = resolver.resolve(name)) {
			return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
