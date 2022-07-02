package net.maisikoleni.javadoc.server;

import java.util.List;
import java.util.OptionalInt;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@Path("/api/search")
public class SearchResource implements JavadocSearch {

	private static final Logger LOG = LoggerFactory.getLogger(SearchResource.class);

	@Inject
	@Jdk(Version.RELEASE_18)
	SearchService searchSerivce;

	@Override
	public Response searchAndRedirect(String query) {
		var x = System.currentTimeMillis();
		var destination = searchSerivce.getBestUrl(query);
		LOG.info("Search for '{}' took {} ms.", query, System.currentTimeMillis() - x);
		return Response.seeOther(destination).build();
	}

	@Override
	public List<String> suggestions(String query, OptionalInt count) {
		if (count.isPresent() && count.getAsInt() < 0)
			throw new BadRequestException("count must not be negative");
		var engine = searchSerivce.searchEngine();
		var x = System.currentTimeMillis();
		try {
			var results = engine.search(query);
			return results.limit(count.orElse(DEFAULT_COUNT)).map(SearchableEntity::qualifiedName).map(Object::toString)
					.toList();
		} finally {
			LOG.info("Search for '{}' took {} ms.", query, System.currentTimeMillis() - x);
		}
	}
}
