package net.maisikoleni.javadoc.db;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.serializer.persistence.types.PersistenceLegacyTypeMappingResultor;
import org.eclipse.serializer.persistence.types.Storer;
import org.eclipse.serializer.reflect.ClassLoaderProvider;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.config.Configuration;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;


@Singleton
public class DBManager {

	private static final Logger LOG = LoggerFactory.getLogger(DBManager.class);

	private static final AtomicBoolean active = new AtomicBoolean();

	private final PersistedJavadocIndexes javadocIndexes = new PersistedJavadocIndexes();

	private EmbeddedStorageManager storageManager;
	private Storer lazyStorer;
	private Storer eagerStorer;

	@Inject
	public DBManager(Configuration config) {
		if (!active.getAndSet(true)) {
			LOG.info("Initialize Database");
			var path = config.db().path().resolve("javadoc-indexes");
			var foundation = EmbeddedStorage.Foundation(path);
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			foundation.onConnectionFoundation(f -> {
				f.setLegacyTypeMappingResultor(PersistenceLegacyTypeMappingResultor.New());
				f.setClassLoaderProvider(ClassLoaderProvider.New(classLoader));
			});
			storageManager = foundation.start(javadocIndexes);
			javadocIndexes.setPersister(storageManager);
			storageManager.storeRoot();
			lazyStorer = Objects.requireNonNull(storageManager.createLazyStorer());
			eagerStorer = Objects.requireNonNull(storageManager.createEagerStorer());
			LOG.info("Database successfully initialized");
		} else {
			LOG.error("Cannot start DB twice");
		}
	}

	@PreDestroy
	public void shutdownStoreage() {
		storageManager.shutdown();
		active.set(false);
	}

	@Produces
	@Singleton
	public JavadocIndexes javadocIndexes() {
		return javadocIndexes;
	}

	public Storer lazyStorer() {
		return lazyStorer;
	}

	public Storer eagerStorer() {
		return eagerStorer;
	}

	public EmbeddedStorageManager getStorageManager() {
		return storageManager;
	}
}