package net.maisikoleni.javadoc.db;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.JavadocIndex;
import one.microstream.persistence.types.Persister;
import one.microstream.reference.Lazy;

public final class PersistedJavadocIndexes implements JavadocIndexes {

	private static final Logger LOG = LoggerFactory.getLogger(PersistedJavadocIndexes.class);

	private Map<URI, JavadocIndexEntry> indexesByUri = Map.of();

	private transient Persister persister; // $NOSONAR$ - needed for MicroStream

	void setPersister(Persister persister) {
		this.persister = Objects.requireNonNull(persister);
	}

	private record JavadocIndexEntry(Lazy<JavadocIndex> lazyIndex) {

		JavadocIndex index() {
			return lazyIndex.get();
		}

		static JavadocIndexEntry of(JavadocIndex index) {
			Objects.requireNonNull(index);
			return new JavadocIndexEntry(Lazy.Reference(index));
		}
	}

	@Override
	public JavadocIndex getIndexByBaseUrl(URI baseUrl, Supplier<JavadocIndex> alternativeSource) {
		Objects.requireNonNull(baseUrl);
		Objects.requireNonNull(alternativeSource);

		var cachedIndex = indexesByUri.get(baseUrl);
		if (cachedIndex != null)
			return cachedIndex.index();
		synchronized (this) {
			cachedIndex = indexesByUri.get(baseUrl);
			if (cachedIndex != null)
				return cachedIndex.index();
			var newIndex = JavadocIndexEntry.of(alternativeSource.get());
			var newMap = new HashMap<>(indexesByUri);
			newMap.put(baseUrl, newIndex);
			indexesByUri = Collections.unmodifiableMap(newMap);
			LOG.info("Storing javadoc index for {}", baseUrl);
			persister.store(this);
			return newIndex.index();
		}
	}
}
