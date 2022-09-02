package net.maisikoleni.javadoc.service;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.spi.InternalServerErrorException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import net.maisikoleni.javadoc.Constants;

@Singleton
public class SearchServiceProvider {

	private final JavadocSearchEngines javadocSearchEngines;

	@Inject
	SearchServiceProvider(JavadocSearchEngines javadocSearchEngines) {
		this.javadocSearchEngines = javadocSearchEngines;
	}

	@Produces
	@Default
	@RequestScoped
	public SearchService newRequestDependent() {
		UriInfo uriInfo = ResteasyProviderFactory.getInstance().getContextData(UriInfo.class);
		var id = uriInfo.getPathParameters().getFirst(Constants.LIBRARY_ID_PARAMETER);
		if (id == null)
			throw new InternalServerErrorException(
					"No path parameter with name '" + Constants.LIBRARY_ID_PARAMETER + "' found.");
		return new SearchService(javadocSearchEngines, id);
	}

	@Produces
	@FixLibraryId("")
	public SearchService newTargetDependent(InjectionPoint injectionPoint) {
		var annotation = injectionPoint.getAnnotated().getAnnotation(FixLibraryId.class);
		Objects.requireNonNull(annotation, "Target dependent SearchService must be qualified with FixLibraryId");
		var id = annotation.value();
		if (FixLibraryId.DEFAULT.equals(id))
			id = javadocSearchEngines.getDefaultLibraryId();
		return new SearchService(javadocSearchEngines, id);
	}

	@Qualifier
	@Retention(RUNTIME)
	@Documented
	public @interface FixLibraryId {

		String DEFAULT = "";

		@Nonbinding
		String value() default DEFAULT;
	}
}
