package net.maisikoleni.javadoc.service.jdk18;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

import net.maisikoleni.javadoc.search.RankedTrieSearchEngine;
import net.maisikoleni.javadoc.search.SearchEngine;
import net.maisikoleni.javadoc.service.Javadoc;
import net.maisikoleni.javadoc.service.Jdk;
import net.maisikoleni.javadoc.service.Jdk.Version;
import net.maisikoleni.javadoc.service.SearchService;

@Singleton
@Default
@Jdk(Version.RELEASE_18)
public class Jdk18SearchSerivce implements SearchService {

	private final Javadoc javadoc;
	private SearchEngine searchEngine;

	public Jdk18SearchSerivce(@Jdk(Version.RELEASE_18) Javadoc javadoc) {
		this.javadoc = javadoc;
	}

	@Override
	public String name() {
		return "JDK 18";
	}

	@Override
	public synchronized SearchEngine searchEngine() {
		if (searchEngine == null)
			searchEngine = new RankedTrieSearchEngine(javadoc.index());
		return searchEngine;
	}

	@Override
	public Javadoc javadoc() {
		return javadoc;
	}
}
