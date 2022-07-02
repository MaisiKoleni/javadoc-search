package net.maisikoleni.javadoc.util.regex;

public interface RegexVisitor<R> {

	R visit(Concatenation concatenation);

	R visit(Literal literal);

	R visit(Star star);

	R visit(CharClass charClass);
}