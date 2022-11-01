package net.maisikoleni.javadoc.config;

import static net.maisikoleni.javadoc.Constants.*;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import javax.validation.constraints.*;

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
	@ValidLibraryId
	Map<String, LibraryConfigValue> libraries();

	public interface LibraryConfigValue {

		@NotBlank
		String name();

		@NotBlank
		String description();

		@NotNull
		URI baseUrl();

		@WithName("default")
		@WithDefault("false")
		boolean isDefault();
	}
}
