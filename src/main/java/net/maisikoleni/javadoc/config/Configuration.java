package net.maisikoleni.javadoc.config;

import static net.maisikoleni.javadoc.Constants.DB_PATH_DEFAULT;
import static net.maisikoleni.javadoc.Constants.LOG_SEARCH_THRESHOLD_NANOS_DEFAULT;
import static net.maisikoleni.javadoc.Constants.PACKAGE;
import static net.maisikoleni.javadoc.Constants.QUERY_LENGHT_LIMIT_DEFAULT;
import static net.maisikoleni.javadoc.Constants.SUGGESTION_COUNT_DEFAULT;
import static net.maisikoleni.javadoc.Constants.SUGGESTION_COUNT_LIMIT_DEFAULT;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = PACKAGE)
public interface Configuration {

	DatabaseConfig db();

	public interface DatabaseConfig {

		@WithDefault(DB_PATH_DEFAULT)
		Path path();
	}

	ServerConfig server();

	public interface ServerConfig {

		@Positive
		@WithDefault(QUERY_LENGHT_LIMIT_DEFAULT)
		int queryCharLimit();

		@Positive
		@WithDefault(SUGGESTION_COUNT_LIMIT_DEFAULT)
		int suggestionCountLimit();

		@PositiveOrZero
		@WithDefault(LOG_SEARCH_THRESHOLD_NANOS_DEFAULT)
		int logSearchThresholdNanos();

		HtmlConfig html();

		interface HtmlConfig {

			@Positive
			@WithDefault(SUGGESTION_COUNT_DEFAULT)
			int suggestionCount();
		}
	}

	@ExactlyOneDefault
	Map<@Pattern(regexp = "[\\p{Alnum}_-]{2,50}") String, @NotNull LibraryConfigValue> libraries();

	public interface LibraryConfigValue {

		@NotBlank
		String name();

		@NotNull
		URI baseUrl();

		@WithName("default")
		@WithDefault("false")
		boolean isDefault();
	}
}
