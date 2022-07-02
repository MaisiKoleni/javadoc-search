package net.maisikoleni.javadoc.parser;

sealed interface JsonSearchablePackagedEntity extends JsonSearchableEntity permits JsonType, JsonMember {

	String m();

	String p();
}
