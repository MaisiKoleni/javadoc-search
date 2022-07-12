/*
 * Based on the JavaScript implementation in JDK 18. Therefore, this code is
 * licensed under GNU General Public License v2.0 with Classpath exception, see
 * https://spdx.org/licenses/GPL-2.0-with-classpath-exception.html
 *
 * SPDX: GPL-2.0-with-classpath-exception
 */
package net.maisikoleni.javadoc.parser;

import java.util.Objects;

public final class JdkUrlGeneration {

	private static final char SLASH = '/';
	private static final char DOT = '.';
	private static final char HASHTAG = '#';
	private static final String HTML_SUFFIX = ".html";
	private static final String UNNAMED = "<Unnamed>";

	private JdkUrlGeneration() {
	}

	private static String getURLPrefix(JsonSearchableEntity se, Iterable<JsonPackage> packageIndex) {
		var urlPrefix = "";
		if (se instanceof JsonModule m) {
			return m.l() + SLASH;
		} else if (se instanceof JsonPackage p && p.m() != null) {
			return p.m() + SLASH;
		} else if (se instanceof JsonSearchablePackagedEntity t) {
			if (t.m() != null) {
				urlPrefix = t.m() + SLASH;
			} else {
				for (var p : packageIndex) {
					if (p.m() != null && Objects.equals(t.p(), p.l())) {
						urlPrefix = p.m() + SLASH;
					}
				}
			}
		}
		return urlPrefix;
	}

	public static String getUrl(String pathRoot, JsonSearchableEntity se, Iterable<JsonPackage> packageIndex) {
		var url = getURLPrefix(se, packageIndex);
		if (se instanceof JsonModule m) {
			url += "module-summary.html";
		} else if (se instanceof JsonPackage p) {
			if (p.u() != null) {
				url = p.u();
			} else {
				url += p.l().replace(DOT, SLASH) + SLASH + "package-summary.html";
			}
		} else if (se instanceof JsonType t) {
			if (t.u() != null) {
				url = t.u();
			} else if (UNNAMED.equals(t.p())) {
				url += t.l() + HTML_SUFFIX;
			} else {
				url += t.p().replace(DOT, SLASH) + SLASH + t.l() + HTML_SUFFIX;
			}
		} else if (se instanceof JsonMember m) {
			if (UNNAMED.equals(m.p())) {
				url += m.c() + HTML_SUFFIX + HASHTAG;
			} else {
				url += m.p().replace(DOT, SLASH) + SLASH + m.c() + HTML_SUFFIX + HASHTAG;
			}
			if (m.u() != null) {
				url += m.u();
			} else {
				url += m.l();
			}
		} else if (se instanceof JsonTag t) {
			url += t.u();
		}
		return pathRoot + url;
	}
}
