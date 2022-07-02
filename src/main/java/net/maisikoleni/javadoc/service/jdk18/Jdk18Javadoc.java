package net.maisikoleni.javadoc.service.jdk18;

import java.net.URI;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.db.JavadocIndexes;
import net.maisikoleni.javadoc.entities.JavadocIndex;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;

@Singleton
@Default
@Jdk(Version.RELEASE_18)
public class Jdk18Javadoc implements Javadoc {

	private static final Logger LOG = LoggerFactory.getLogger(Jdk18Javadoc.class);

	private static final URI JAVADOC_BASE_URL = URI.create("https://docs.oracle.com/en/java/javase/18/docs/api/");

	private final JavadocIndex index;

	@Inject
	public Jdk18Javadoc(JavadocIndexes javadocIndexes) {
		long startTime = System.currentTimeMillis();
		index = javadocIndexes.getIndexByBaseUrl(JAVADOC_BASE_URL, Jdk18Javadoc::fetchIndex);
		long endTime = System.currentTimeMillis();
		LOG.info("Creating Jdk18Javadoc took {} ms.", endTime - startTime);
	}

	private static JavadocIndex fetchIndex() {
		try {
			return JavadocIndex.loadAsResources(Jdk18Javadoc.class);
		} catch (Exception e) {
			LOG.warn("Fetching JavadocIndex as resource failed, loading it from the website.", e);
			return JavadocIndex.loadFromUrl(JAVADOC_BASE_URL);
		}
	}

	@Override
	public JavadocIndex index() {
		return index;
	}

	@Override
	public URI baseUrl() {
		return JAVADOC_BASE_URL;
	}
}
