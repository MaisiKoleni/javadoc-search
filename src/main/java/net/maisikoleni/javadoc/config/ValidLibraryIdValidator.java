package net.maisikoleni.javadoc.config;

import static net.maisikoleni.javadoc.Constants.LIBRARY_ID_PATTERN;

import java.util.Map;
import java.util.regex.Matcher;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidLibraryIdValidator implements ConstraintValidator<ValidLibraryId, Map<String, ?>> {

	@Override
	public boolean isValid(Map<String, ?> map, ConstraintValidatorContext context) {
		return map.keySet().stream().map(LIBRARY_ID_PATTERN::matcher).allMatch(Matcher::matches);
	}
}