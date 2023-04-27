package net.maisikoleni.javadoc.server;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.maisikoleni.javadoc.config.Configuration;

@Singleton
public final class SearchReporter {

	private static final Logger LOG = LoggerFactory.getLogger(SearchReporter.class);

	private static final double NANOS_IN_MILLISECOND = TimeUnit.MILLISECONDS.toNanos(1);

	private final int logSearchThresholdNanos;

	@Inject
	public SearchReporter(Configuration config) {
		this.logSearchThresholdNanos = config.server().logSearchThresholdNanos();
	}

	public void logSearchTime(String operation, String query, long x) {
		long dt = System.nanoTime() - x;
		if (dt >= logSearchThresholdNanos && LOG.isInfoEnabled()) {
			var formattedDuration = "%.1f".formatted(dt / NANOS_IN_MILLISECOND);
			LOG.info("Search for '{}' took {} ms. (operation: {})", query, formattedDuration, operation);
		}
	}
}
