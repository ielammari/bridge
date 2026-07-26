package io.github.ielammari.bridge.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the executive (cadre) status, conceptually a boolean, in the
 * VARCHAR statut_cadre column as a readable label.
 */
@Converter
public class ExecutiveStatusConverter implements AttributeConverter<Boolean, String> {

	private static final String EXECUTIVE = "CADRE";
	private static final String NON_EXECUTIVE = "NON_CADRE";

	@Override
	public String convertToDatabaseColumn(Boolean executive) {
		if (executive == null) {
			return null;
		}
		return executive ? EXECUTIVE : NON_EXECUTIVE;
	}

	@Override
	public Boolean convertToEntityAttribute(String stored) {
		if (stored == null) {
			return null;
		}
		return EXECUTIVE.equals(stored);
	}

}
