package net.maisikoleni.javadoc.config;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidLibraryIdValidator implements ConstraintValidator<ValidLibraryId, Map<String, ?>> {

	static final Pattern LIBRARY_ID_PATTERN = Pattern.compile(ValidLibraryId.LIBRARY_ID_PATTERN_STRING);

	@Override
	public boolean isValid(Map<String, ?> map, ConstraintValidatorContext context) {
		return map.keySet().stream().map(LIBRARY_ID_PATTERN::matcher).allMatch(Matcher::matches);
	}
}