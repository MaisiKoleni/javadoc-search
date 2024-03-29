package net.maisikoleni.javadoc.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.*;

@QuarkusTest
class PerformanceTest {

	private static final Logger LOG = LoggerFactory.getLogger(PerformanceTest.class);
	/**
	 * Chars for the query selected by random, many appearing multiple times
	 */
	private static final String CHARS = ". . . . . . . . . / / / / / AAABBCCCDDEEEFGHIIJJJKLMMNOOPQRSSSTTUVWXYZ"
			+ "aaaaabbbcccddeeeeeffghiiiijjjjkllmmnnoooopqrrrrsssstttuuuuvwxyz";
	private static final long SEED = 42;
	private static final int REQUESTS = 10_000;

	/**
	 * Delta times in nanoseconds per query.
	 */
	private Map<String, List<Long>> timings;
	private HttpClient client;

	@BeforeEach
	void setupHttpClient() {
		timings = new ConcurrentHashMap<>();
		client = Vertx.vertx(new VertxOptions()).createHttpClient(new HttpClientOptions() //
				.setDefaultHost("localhost") //
				.setDefaultPort(RestAssured.port) //
				.setSsl(false) //
				.setMaxRedirects(0) //
		);
	}

	@Test
	void testSearchSuggestionPerformance() {
		var queries = generateRandomQueries();

		var futureResponses = queries.stream().map(q -> {
			String requestURI = "/api/v2/libraries/jdk-latest/search/suggestions?query="
					+ URLEncoder.encode(q, StandardCharsets.UTF_8);
			return client.request(HttpMethod.GET, requestURI).compose(req -> handleRequest(req, q));
		}).toList();
		// wait for requests to complete
		Future.join(futureResponses).toCompletionStage().toCompletableFuture().join();
		// create a summary statistic
		var stats = timings.values().stream().flatMap(List::stream).mapToLong(x -> x).summaryStatistics();
		LOG.info("Performance statistics: {}", stats);

		assertThat(stats.getCount()).isEqualTo(REQUESTS);
		assertThat(stats.getAverage()).isLessThan(MILLISECONDS.toNanos(20));
		assertThat(stats.getMin()).isLessThan(MILLISECONDS.toNanos(2));
	}

	private static List<String> generateRandomQueries() {
		var random = RandomGeneratorFactory.of("L32X64MixRandom").create(SEED);
		var charsSize = CHARS.length();
		return IntStream.range(0, REQUESTS).mapToObj(n -> {
			// short length, more likely to be small number
			int length = (n % 5) * (n % 3) + (n % 2) + 1;
			var query = new StringBuilder(length);
			for (int i = 0; i < length; i++) {
				query.append(CHARS.charAt(random.nextInt(charsSize)));
			}
			return query.toString();
		}).toList();
	}

	private Future<HttpClientResponse> handleRequest(HttpClientRequest req, String query) {
		long t1 = System.nanoTime();
		return req.send().onComplete(aresRes -> {
			long dt = System.nanoTime() - t1;
			timings.computeIfAbsent(query, key -> Collections.synchronizedList(new ArrayList<>())).add(dt);
		});
	}
}
