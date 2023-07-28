package net.maisikoleni.javadoc.server;

import java.util.UUID;

import net.maisikoleni.javadoc.Constants;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class HttpCacheFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final String SERVER_RUN_ID = UUID.randomUUID().toString();

	@Override
	@SuppressWarnings("resource")
	public void filter(ContainerRequestContext context) {
		if (!HttpMethod.GET.equals(context.getMethod()))
			return;
		if (!SERVER_RUN_ID.equals(context.getHeaderString(HttpHeaders.IF_NONE_MATCH))) {
			return;
		}
		var cache = new CacheControl();
		cache.setMaxAge(Constants.HTTP_CACHE_MAX_AGE);
		context.abortWith(Response.notModified().cacheControl(cache).build());
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
		responseContext.getHeaders().add(HttpHeaders.ETAG, SERVER_RUN_ID);
	}
}