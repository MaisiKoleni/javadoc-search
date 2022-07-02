package net.maisikoleni.javadoc.entities;

public final class Module extends SearchableEntity {

	public Module(String name) {
		super(name);
	}

	@Override
	public String url() {
		return concatUrl(urlPrefix(), "module-summary.html");
	}
}
