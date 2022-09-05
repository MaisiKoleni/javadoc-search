package net.maisikoleni.javadoc.config;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ValidLibraryIdValidator.class)
public @interface ValidLibraryId {

	static final String LIBRARY_ID_PATTERN_STRING = "[\\p{Alnum}_-]{2,13}";

	String message() default "All library ids must match '" + LIBRARY_ID_PATTERN_STRING
			+ " (length limit of 13 is due to the OpenSearch spec).";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
