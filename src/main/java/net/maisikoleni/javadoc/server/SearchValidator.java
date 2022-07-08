package net.maisikoleni.javadoc.server;

import static net.maisikoleni.javadoc.Configuration.QUERY_LENGHT_LIMIT_DEFAULT;
import static net.maisikoleni.javadoc.Configuration.QUERY_LENGHT_LIMIT_KEY;
import static net.maisikoleni.javadoc.Configuration.SUGGESTION_COUNT_LIMIT_DEFAULT;
import static net.maisikoleni.javadoc.Configuration.SUGGESTION_COUNT_LIMIT_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public final class SearchValidator {

	@Inject
	@ConfigProperty(name = QUERY_LENGHT_LIMIT_KEY, defaultValue = QUERY_LENGHT_LIMIT_DEFAULT)
	int queryCharLimit;

	@Inject
	@ConfigProperty(name = SUGGESTION_COUNT_LIMIT_KEY, defaultValue = SUGGESTION_COUNT_LIMIT_DEFAULT)
	int suggestionCountLimit;

	public void validateSuggestionCount(int suggestionCount) {
		if (suggestionCount <= 0)
			throw new BadRequestException("suggestion count must be a positive number");
		if (suggestionCount > suggestionCountLimit)
			throw new BadRequestException("suggestion count must not exceed " + suggestionCountLimit);
	}

	public void validateQuery(String query) {
		if (query == null)
			throw new BadRequestException("query must not be null");
		if (query.length() > queryCharLimit)
			throw new BadRequestException("query length must not exceed " + queryCharLimit);
	}
}
