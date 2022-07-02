package net.maisikoleni.javadoc.util;

import java.util.Objects;

public final class LightString implements CharSequence, Comparable<CharSequence> {

	private static final Cache<String> CACHE = Cache.newConcurrent();
	private static final Cache<String> CACHE_LS = Cache.newConcurrent();

	private static final CharSequence EMPTY = LightString.of("");

	private final String content;
	private final int start;
	private final int end;
	private final int hash;

	private LightString(String content, int start, int end) {
		this.content = content; // CACHE.getOrCache(content);
		this.start = start;
		this.end = end;
		this.hash = calcHash();
	}

	private LightString(String content) {
		this(content, 0, content.length());
	}

	@Override
	public boolean isEmpty() {
		return start == end;
	}

	@Override
	public int length() {
		return end - start;
	}

	@Override
	public char charAt(int index) {
		return content.charAt(start + index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if (this.start == start && this.end == end)
			return this;
		return LightString.of(content, this.start + start, this.start + end);
	}

	@Override
	public int hashCode() {
		int localHash = hash;
//		if (!hashed) {
//			localHash = hash = calcHash();
//			hashed = true;
//		}
		return localHash;
	}

	private int calcHash() {
		int hash = 1;
		for (int i = start; i < end; i++) {
			hash = hash * 31 + content.charAt(i);
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		return obj instanceof LightString ls && hash == ls.hash
				&& content.regionMatches(false, ls.start, ls.content, start, length());
	}

	@Override
	public int compareTo(CharSequence o) {
		if (o instanceof LightString ls) {
			int minEnd = Math.min(end, start + ls.length());
			for (int i1 = start, i2 = ls.start; i1 < minEnd; i1++, i2++) {
				char c1 = content.charAt(i1);
				char c2 = ls.content.charAt(i2);
				if (c1 != c2)
					return Character.compare(c1, c2);
			}
			return length() - ls.length();
		}
		return CharSequence.compare(this, o);
	}

	private static CharSequence of(String content, int start, int end) {
		if (start == end)
			return EMPTY;
		return new LightString(content, start, end);
	}

	public static CharSequence of(String content) {
		return of(Objects.requireNonNull(content), 0, content.length());
	}

	public static CharSequence of(CharSequence content) {
		return of(content.toString());
	}

	public static void clearCache() {
		CACHE.clear();
		CACHE_LS.clear();
	}

	@Override
	public String toString() {
		return content.substring(start, end);
	}
}
