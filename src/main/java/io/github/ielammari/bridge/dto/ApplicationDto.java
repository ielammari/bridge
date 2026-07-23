package io.github.ielammari.bridge.dto;

import java.time.Instant;

import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.ContractType;

/** A candidate's own application, for the tracking page. */
public record ApplicationDto(
		Integer id,
		Integer offerId,
		String offerTitle,
		ContractType contractType,
		String location,
		Instant applicationDate,
		ApplicationStatus status) {
}
