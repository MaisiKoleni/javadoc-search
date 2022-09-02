package net.maisikoleni.javadoc.server.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class JavadocSearchPageFastTest {

	private static final Path CUSTOM_CSS_PATH = Path
			.of("src/main/resources/META-INF/resources/javadoc-search-style.css");
	private static final Path TEMPLATE_BASE_PATH = Path.of("src/main/resources/templates/base.qute.html");

	@Test
	void testCssIntegrityEquality() throws IOException, NoSuchAlgorithmException {
		var customCssBytes = Files.readAllBytes(CUSTOM_CSS_PATH);
		var templateBase = Files.readString(TEMPLATE_BASE_PATH, StandardCharsets.UTF_8);

		var hashInTemplatePattern = Pattern.compile("""
				link\\s++\
				rel="stylesheet"\\s++\
				href="/javadoc-search-style.css"\\s++\
				integrity="sha256-([A-Za-z0-9+/=]++)"\
				""");
		var matcher = hashInTemplatePattern.matcher(templateBase);
		assertThat(matcher.find()).isTrue();
		var hashInTemplate = matcher.group(1);
		var hashOfFile = Base64.getEncoder()
				.encodeToString(MessageDigest.getInstance("SHA-256").digest(customCssBytes));

		assertThat(hashInTemplate).isEqualTo(hashOfFile);
	}
}
