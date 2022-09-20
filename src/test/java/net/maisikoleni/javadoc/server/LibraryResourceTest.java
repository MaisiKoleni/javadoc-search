package net.maisikoleni.javadoc.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LibraryResourceTest {

	private static final String ROUTE_PREFIX = "/api/v2/libraries";

	@Test
	void testGetAll() {
		given().when().get(ROUTE_PREFIX).then() //
				.statusCode(Status.OK.getStatusCode()) //
				.body(is("""
						[{\
						"name":"JDK 19",\
						"id":"jdk-latest",\
						"description":"Latest JDK Release",\
						"baseUrl":"https://docs.oracle.com/en/java/javase/19/docs/api/"\
						},{\
						"name":"JUnit 5",\
						"id":"junit5-latest",\
						"description":"Current JUnit 5",\
						"baseUrl":"https://junit.org/junit5/docs/current/api/"\
						}]\
						"""));
	}
}
