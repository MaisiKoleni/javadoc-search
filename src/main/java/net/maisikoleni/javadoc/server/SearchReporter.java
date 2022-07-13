package net.maisikoleni.javadoc.server;

import static net.maisikoleni.javadoc.Configuration.LOG_SEARCH_THRESHOLD_NANOS_DEFAULT;
import static net.maisikoleni.javadoc.Configuration.LOG_SEARCH_THRESHOLD_NANOS_KEY;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class SearchReporter {

	private static final Logger LOG = LoggerFactory.getLogger(SearchReporter.class);

	private static final double NANOS_IN_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);

	@Inject
	@ConfigProperty(name = LOG_SEARCH_THRESHOLD_NANOS_KEY, defaultValue = LOG_SEARCH_THRESHOLD_NANOS_DEFAULT)
	int logSearchThresholdNanos;

	public void logSearchTime(String operation, String query, long x) {
		long dt = System.nanoTime() - x;
		if (dt >= logSearchThresholdNanos && LOG.isInfoEnabled()) {
			var formattedDuration = "%.1f".formatted(dt / NANOS_IN_MILLISECOND);
			LOG.info("Search for '{}' took {} ms. (operation: {})", query, formattedDuration, operation);
		}
	}
}
