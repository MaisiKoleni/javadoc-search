package net.maisikoleni.javadoc.server;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import net.maisikoleni.javadoc.config.Configuration;

@Singleton
public final class SearchValidator {

	private final int suggestionCountLimit;
	private final int queryCharLimit;

	@Inject
	public SearchValidator(Configuration config) {
		this.suggestionCountLimit = config.server().suggestionCountLimit();
		this.queryCharLimit = config.server().queryCharLimit();
	}

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
		if (!query.chars().allMatch(Character::isDefined))
			throw new BadRequestException("query length must not contain undefined characters");
	}
}
