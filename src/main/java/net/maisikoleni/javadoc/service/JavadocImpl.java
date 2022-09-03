package net.maisikoleni.javadoc.service;

import static net.maisikoleni.javadoc.Constants.JAVADOC_SEARCH_ID_PATTERN;

import java.net.URI;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.db.JavadocIndexes;
import net.maisikoleni.javadoc.entities.JavadocIndex;

public class JavadocImpl implements Javadoc {

	private static final Logger LOG = LoggerFactory.getLogger(JavadocImpl.class);

	private final String id;
	private final String name;
	private final URI baseUrl;
	private final JavadocIndex index;

	@Inject
	public JavadocImpl(String id, String name, URI baseUrl, JavadocIndexes javadocIndexes) {
		if (!JAVADOC_SEARCH_ID_PATTERN.matcher(id).matches())
			throw new IllegalArgumentException("id must match: " + JAVADOC_SEARCH_ID_PATTERN.pattern());
		if (name.isBlank())
			throw new IllegalArgumentException("name must not be empty");
		this.id = id;
		this.name = name;
		this.baseUrl = Objects.requireNonNull(baseUrl);
		long startTime = System.currentTimeMillis();
		this.index = javadocIndexes.getIndexByBaseUrl(baseUrl, this::fetchIndex);
		long endTime = System.currentTimeMillis();
		LOG.info("Creating JavadocImpl took {} ms.", endTime - startTime);
	}

	private JavadocIndex fetchIndex() {
		LOG.info("Fetching JavadocIndex for {}", id);
		try {
			return JavadocIndex
					.loadFromInputStreams(indexFile -> JavadocImpl.class.getResourceAsStream(id + "/" + indexFile));
		} catch (Exception e) {
			LOG.warn("Fetching JavadocIndex as resource failed, loading it from the website: {}", e.toString());
			return JavadocIndex.loadFromUrl(baseUrl);
		}
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public JavadocIndex index() {
		return index;
	}

	@Override
	public URI baseUrl() {
		return baseUrl;
	}
}
