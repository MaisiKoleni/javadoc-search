package net.maisikoleni.javadoc.server.html;

import java.net.URI;
import java.util.Collection;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import net.maisikoleni.javadoc.entities.SearchableEntity;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@Path("/")
public final class JavadocSearchPage {

	private static final Logger LOG = LoggerFactory.getLogger(JavadocSearchPage.class);

	@Inject
	@ConfigProperty(name = "net.maisikoleni.javadoc.search.server.html.suggestion-count", defaultValue = "10")
	int count;

	@Inject
	@Jdk(Version.RELEASE_18)
	SearchService searchService;

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public String generateMainPage() {
		return Templates.searchPage(searchService.name()).render();
	}

	@POST
	@Path("search-redirect")
	public Response searchAndRedirect(@NotNull @FormParam("query") String query) {
		var x = System.currentTimeMillis();
		try {
			return Response.seeOther(searchService.getBestUrl(query)).build();
		} finally {
			LOG.info("Search for '{}' took {} ms.", query, System.currentTimeMillis() - x);
		}
	}

	@GET
	@Path("search-suggestions")
	@Produces(MediaType.TEXT_HTML)
	public String getSearchSuggestionTable(@NotNull @QueryParam("query") String query) {
		var x = System.currentTimeMillis();
		var results = searchService.searchEngine().search(query).limit(count).toList();
		LOG.info("Suggestions for '{}' took {} ms.", query, System.currentTimeMillis() - x);
		return Templates.searchSuggestions(searchService.javadoc().baseUrl(), results).render();
	}

	@CheckedTemplate
	public static class Templates {

		public static native TemplateInstance searchPage(String javadocName);

		public static native TemplateInstance searchSuggestions(URI baseUrl, Collection<SearchableEntity> suggestions);
	}
}
