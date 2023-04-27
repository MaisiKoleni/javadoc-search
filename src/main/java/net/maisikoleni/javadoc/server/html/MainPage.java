package net.maisikoleni.javadoc.server.html;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import net.maisikoleni.javadoc.Constants;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

@ApplicationScoped
@Path("/")
public final class MainPage {

	@Inject
	JavadocSearchEngines javadocSearchEngines;

	@GET
	@Path("")
	public Response redirectToDefaultLibrary(@QueryParam(JavadocSearchPage.QUERY_NAME) Optional<String> query) {
		var uriBulder = UriBuilder.fromResource(JavadocSearchPage.class);
		uriBulder.resolveTemplate(Constants.LIBRARY_ID_PARAMETER, javadocSearchEngines.getDefaultLibraryId());
		query.ifPresent(queryValue -> uriBulder.replaceQueryParam(JavadocSearchPage.QUERY_NAME, queryValue));
		return Response.temporaryRedirect(uriBulder.build()).build();
	}
}
