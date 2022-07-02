package net.maisikoleni.javadoc.parser;

public sealed interface JsonSearchableEntity permits JsonModule, JsonPackage, JsonSearchablePackagedEntity, JsonTag {

	String l();
}
