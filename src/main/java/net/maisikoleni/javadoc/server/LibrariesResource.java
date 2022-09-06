package net.maisikoleni.javadoc.server;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
