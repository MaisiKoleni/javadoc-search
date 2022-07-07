package net.maisikoleni.javadoc.util;

import static java.util.Objects.requireNonNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

public final class SingleElementSet<T> extends AbstractSet<T> {

	private final T element;

	private SingleElementSet(T element) {
		this.element = requireNonNull(element);
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<>() {

			private boolean consumed;

			@Override
			public boolean hasNext() {
				return !consumed;
			}

			@Override
			public T next() {
				if (consumed)
					throw new NoSuchElementException();
				consumed = true;
				return element;
			}
		};
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean contains(Object o) {
		return element.equals(o);
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		action.accept(element);
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof SingleElementSet<?> set)
			return element.equals(set.element);
		if (!(o instanceof Set<?> set))
			return false;
		return set.size() == 1 && element.equals(set.iterator().next());
	}

	@Override
	public String toString() {
		return "[" + element + "]";
	}

	public static <T> SingleElementSet<T> of(T element) {
		return new SingleElementSet<>(element);
	}
}
