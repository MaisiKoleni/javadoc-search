package net.maisikoleni.javadoc.server.html;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.TemplateExtension;
import net.maisikoleni.javadoc.entities.SearchableEntity;

/**
 * Allows to generate the URL of a {@link SearchableEntity} using the given base
 * URL as {@link URI} and falling back to that base URL on failure.
 */
@TemplateExtension
public final class SearchableEntityToUrl {

	private static final Logger LOG = LoggerFactory.getLogger(SearchableEntityToUrl.class);

	private SearchableEntityToUrl() {
	}

	public static String toUrlSafe(SearchableEntity searchableEntity, URI baseUrl) {
		try {
			return searchableEntity.url(baseUrl);
		} catch (IllegalArgumentException e) {
			LOG.warn("Failed to generate URL for {}, using base URL.", searchableEntity, e);
			return baseUrl.toString();
		}
	}
}
