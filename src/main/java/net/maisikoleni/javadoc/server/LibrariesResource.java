package net.maisikoleni.javadoc.server;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.JavadocSearchEngines;

@Singleton
@Path("/api/v2/libraries")
public class LibrariesResource {

	@Inject
	JavadocSearchEngines javadocSearchEngines;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Javadoc> allAvailableLibraries() {
		return javadocSearchEngines.allJavadocs();
	}
}
