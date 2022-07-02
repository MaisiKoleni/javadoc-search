package net.maisikoleni.javadoc.search;

import net.maisikoleni.javadoc.entities.JavadocIndex;

public abstract class IndexBasedSearchEngine implements SearchEngine {

	private final JavadocIndex index;

	protected IndexBasedSearchEngine(JavadocIndex index) {
		this.index = index;
	}

	public final JavadocIndex getIndex() {
		return index;
	}
}
