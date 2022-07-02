package net.maisikoleni.javadoc.util;

import java.util.stream.Stream;

import one.microstream.exceptions.IndexBoundsException;

public final class LightRope implements CharSequence {

	private static final int AUTO_END = -1;

	private static final LightRope EMPTY = LightRope.of("");

	private final Object content;
	private final int[] lengths;
	private final int start;
	private final int end;
	private final int hash;

	@SuppressWarnings("null")
	private LightRope(Object content, int start, int end) {
		var string = content.getClass() == String.class ? (String) content : null;
		var array = string != null ? null : (String[]) content;
		int totalLength;
		int startContent = 0;
		if (array != null) {
			this.lengths = new int[array.length];
			totalLength = 0;
			for (int i = 0; i < lengths.length; i++) {
				totalLength += array[i].length();
				if (totalLength < start)
					startContent++;
				lengths[i] = totalLength;
			}
			this.content = content;
		} else {
			this.lengths = null;
			this.content = string;
			totalLength = string.length();
		}
		this.start = start;
		this.end = end == AUTO_END ? totalLength : end;
		int hash = 1;
		if (array != null) {
			for (int i = startContent, index = 0; index < end; i++) {
				for (int j = 0; j < array[i].length() && index < end; j++, index++) {
					hash = hash * 31 + array[i].charAt(j);
				}
			}
		} else {
			for (int i = start; i < end; i++) {
				hash = hash * 31 + string.charAt(i);
			}
		}
		this.hash = hash;
	}

	private LightRope(String content) {
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

	public int totalLength() {
		if (content instanceof String s)
			return s.length();
		return lengths[lengths.length - 1];
	}

	@Override
	public char charAt(int index) {
		return (char) charAt(index, 0L);
	}

	private long charAt(int index, long previous) {
		if (lengths == null)
			return ((String) content).charAt(index + start);
		var array = (String[]) content;
		int toFind = start + index;
		int previousI = (int) (previous >> 32);
		int lastLength = previousI == 0 ? 0 : lengths[previousI - 1];
		for (int i = previousI; i < array.length; i++) {
			int currentLength = lengths[i];
			if (currentLength > toFind)
				return array[i].charAt(toFind - lastLength) | (long) i << 32;
			lastLength = currentLength;
		}
		throw new IndexBoundsException(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if (start == 0 && end == length())
			return this;
		return LightRope.of(content, totalLength(), this.start + start, this.start + end);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		return obj instanceof LightRope ls && hash == ls.hash && contentEquals(ls);
	}

	private boolean contentEquals(LightRope ls) {
		int length = length();
		if (length != ls.length())
			return false;
		if (content == ls.content)
			return start == ls.start;
		long stateA = 0;
		long stateB = 0;
		for (int i = 0; i < length; i++) {
			stateA = charAt(i, stateA);
			stateB = ls.charAt(i, stateB);
			if ((char) stateA != (char) stateB)
				return false;
		}
		return true;
	}

	private static CharSequence of(Object content, int totalLength, int start, int end) {
		if (start == end)
			return EMPTY;
		if (content instanceof String)
			return new LightRope(content, start, end);
		var array = (String[]) content;
		int startContent = 0;
		int endContent = array.length;
		int newStart = start;
		int length = 0;
		for (int i = 0; i < array.length; i++) {
			int temp = array[i].length();
			length += temp;
			if (length <= start) {
				startContent++;
				newStart -= temp;
			} else {
				break;
			}
		}
		length = totalLength;
		for (int i = endContent - 1; i > 0; i--) {
			length -= array[i].length();
			if (length >= end) {
				endContent--;
			} else {
				break;
			}
		}
		if (startContent != 0 || endContent != array.length) {
			Object newContent;
			if (startContent + 1 == endContent) {
				newContent = array[startContent];
//			else
//				newContent = Arrays.copyOfRange(array, startContent, endContent);
				return new LightRope(newContent, newStart, end - start + newStart);
			}
		}
		return new LightRope(array, start, end);

	}

	public static LightRope of(Stream<String> content) {
		return new LightRope(content.map(String::intern).toArray(String[]::new), 0, AUTO_END);
	}

	public static LightRope of(String... content) {
		return LightRope.of(Stream.of(content));
	}

	public static LightRope of(String content) {
		return new LightRope(content.intern());
	}

	public static LightRope of(CharSequence content) {
		return LightRope.of(content.toString());
	}

	@Override
	public String toString() {
		if (content instanceof String s)
			return s.substring(start, end);
		return String.join("", (String[]) content).substring(start, end);
	}

	public static void main(String[] args) {
		var x = LightRope.of("abc", "xyz", "O");

		for (int i = 0; i < 7; i++) {
			for (int j = i + 1; j <= 7; j++) {
				CharSequence subSequence = x.subSequence(i, j);
				System.out.println(subSequence);
			}
		}
	}
}
