package net.maisikoleni.javadoc.util.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Concatenation(List<Regex> parts) implements Regex {

	public Concatenation(Regex... parts) {
		this(Arrays.asList(parts));
	}

	public Concatenation(List<Regex> parts) {
		this.parts = List.copyOf(parts);
	}

	@Override
	public int matches(CharSequence s, int start) {
		int pos = start;
		for (Regex regex : parts) {
			pos = regex.matches(s, pos);
			if (pos == -1)
				return -1;
		}
		return pos;
	}

	@Override
	public String toString() {
		return parts.stream().map(Regex::toString).collect(Collectors.joining());
	}

	@Override
	public <R> R accept(RegexVisitor<R> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Stream<Regex> stream() {
		return parts.stream().flatMap(Regex::stream);
	}

	public static Concatenation of(Regex... parts) {
		return new Concatenation(parts);
	}

	public static <R extends Regex> Collector<R, List<Regex>, Concatenation> joining(Regex delimiter) {
		Objects.requireNonNull(delimiter);
		return collector((prev, next) -> delimiter, (prev, next) -> true);
	}

	public static <R extends Regex> Collector<R, List<Regex>, Concatenation> joining(Regex delimiter,
			BiPredicate<R, R> condition) {
		Objects.requireNonNull(delimiter);
		return collector((prev, next) -> delimiter, Objects.requireNonNull(condition));
	}

	public static <R extends Regex> Collector<R, List<Regex>, Concatenation> joining(
			BiFunction<R, R, Regex> delimiter) {
		Objects.requireNonNull(delimiter);
		return collector(delimiter, null);
	}

	public static <R extends Regex> Collector<R, List<Regex>, Concatenation> joining() {
		return collector(null, null);
	}

	private static <R extends Regex> Collector<R, List<Regex>, Concatenation> collector(
			BiFunction<R, R, Regex> delimiter, BiPredicate<R, R> condition) {
		return new Collector<>() {

			@Override
			public Supplier<List<Regex>> supplier() {
				return ArrayList::new;
			}

			@Override
			public Function<List<Regex>, Concatenation> finisher() {
				return Concatenation::new;
			}

			@Override
			public BinaryOperator<List<Regex>> combiner() {
				return (a, b) -> {
					if (!b.isEmpty())
						addDelimiterIfNecessary(a, getFirst(b));
					a.addAll(b);
					return a;
				};
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Set.of();
			}

			@Override
			public BiConsumer<List<Regex>, R> accumulator() {
				return (list, regex) -> {
					addDelimiterIfNecessary(list, regex);
					list.add(regex);
				};
			}

			@SuppressWarnings("unchecked")
			private R getFirst(List<Regex> list) {
				// first is never the delimiter
				return (R) list.get(0);
			}

			@SuppressWarnings("unchecked")
			private R getLast(List<Regex> list) {
				// last is never the delimiter
				return (R) list.get(list.size() - 1);
			}

			private void addDelimiterIfNecessary(List<Regex> list, R next) {
				if (delimiter != null && !list.isEmpty()
						&& (condition == null || condition.test(getLast(list), next))) {
					var result = delimiter.apply(getLast(list), next);
					if (result != null)
						list.add(result);
				}
			}
		};
	}
}