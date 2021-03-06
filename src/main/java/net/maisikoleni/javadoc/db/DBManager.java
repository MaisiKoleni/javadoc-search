package net.maisikoleni.javadoc.db;

import static net.maisikoleni.javadoc.Configuration.DB_PATH_DEFAULT;
import static net.maisikoleni.javadoc.Configuration.DB_PATH_KEY;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import one.microstream.persistence.types.PersistenceLegacyTypeMappingResultor;
import one.microstream.persistence.types.Storer;
import one.microstream.reflect.ClassLoaderProvider;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@Singleton
public class DBManager {

	private static final Logger LOG = LoggerFactory.getLogger(DBManager.class);

	private static final AtomicBoolean active = new AtomicBoolean();

	private PersistedJavadocIndexes javadocIndexes = new PersistedJavadocIndexes();

	private EmbeddedStorageManager storageManager;
	private Storer lazyStorer;
	private Storer eagerStorer;

	@Inject
	public DBManager(@ConfigProperty(name = DB_PATH_KEY, defaultValue = DB_PATH_DEFAULT) Path databasePath) {
		if (!active.getAndSet(true)) {
			LOG.info("Initialize Database");
			var foundation = EmbeddedStorage.Foundation(databasePath.resolve("javadoc-indexes"));
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