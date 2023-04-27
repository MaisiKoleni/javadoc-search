package net.maisikoleni.javadoc.config;

import java.util.Map;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import net.maisikoleni.javadoc.config.Configuration.LibraryConfigValue;

public class ExactlyOneDefaultValidator implements ConstraintValidator<ExactlyOneDefault, Map<?, LibraryConfigValue>> {

	@Override
	public boolean isValid(Map<?, LibraryConfigValue> map, ConstraintValidatorContext context) {
		var defaultLibrariesCount = map.entrySet().stream().filter(entry -> entry.getValue().isDefault()).count();
		return defaultLibrariesCount == 1;
	}
}