package net.maisikoleni.javadoc.entities;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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
		return (Stream<SearchableEntity>) Stream.of(modules, packages, types, members, tags).map(List::stream) // $NOSONAR$
				.reduce(Stream::concat).get(); // this is never empty, it consists of exactly 5 concatenated streams
	}

	public static JavadocIndex loadFromUrl(URI javadocLocation) {
		return loadFromInputStreams(indexName -> javadocLocation.resolve(indexName).toURL().openStream());
	}

	public static JavadocIndex loadAsResources(Class<?> resourceLocation) {
		return loadFromInputStreams(resourceLocation::getResourceAsStream);
	}

	public static JavadocIndex loadFromInputStreams(IndexResourceResolver resolver) {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var moduleSearchIndex = executor.submit(() -> getIndexAsString(resolver, MODULE_SEARCH_INDEX));
			var packageSearchIndex = executor.submit(() -> getIndexAsString(resolver, PACKAGE_SEARCH_INDEX));
			var typeSearchIndex = executor.submit(() -> getIndexAsString(resolver, TYPE_SEARCH_INDEX));
			var memberSearchIndex = executor.submit(() -> getIndexAsString(resolver, MEMBER_SEARCH_INDEX));
			var tagSearchIndex = executor.submit(() -> getIndexAsString(resolver, TAG_SEARCH_INDEX));
			executor.shutdown();
			if (!executor.awaitTermination(10, SECONDS)) {
				executor.shutdownNow();
				throw new JavadocIndexLoadException("JavadocIndex loading/reading timed out");
			}
			var jsonIndex = JavaScriptIndexParser.parseJavaScriptIndexes(moduleSearchIndex.get(),
					packageSearchIndex.get(), typeSearchIndex.get(), memberSearchIndex.get(), tagSearchIndex.get());
			return jsonIndex.toJavadocIndex();
		} catch (RuntimeException | ExecutionException e) {
			throw JavadocIndexLoadException.from(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new JavadocIndexLoadException("JavadocIndex load interrupted", e);
		}
	}

	static String getIndexAsString(IndexResourceResolver resolver, String name) {
		try (var resourceStream = resolver.resolve(name)) {
			return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new JavadocIndexLoadException(e);
		}
	}

	@FunctionalInterface
	public interface IndexResourceResolver {
		InputStream resolve(String indexName) throws IOException;
	}

	public static final class JavadocIndexLoadException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		JavadocIndexLoadException(String message, Throwable cause) {
			super(message, cause);
		}

		JavadocIndexLoadException(String message) {
			super(message);
		}

		JavadocIndexLoadException(Throwable cause) {
			super(cause);
		}

		static JavadocIndexLoadException from(Throwable cause) {
			if (cause instanceof ExecutionException ee)
				return from(ee.getCause());
			if (cause instanceof JavadocIndexLoadException jole)
				throw jole;
			throw new JavadocIndexLoadException(cause);
		}
	}
}
