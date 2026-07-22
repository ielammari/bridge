package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.model.RemoteMode;

/** An offer with its requirements, used for the HR views and the candidate feed. */
public record OfferDto(
		Integer id,
		String title,
		String description,
		Degree requiredDegree,
		ContractType contractType,
		String location,
		RemoteMode remoteMode,
		BigDecimal salaryMin,
		BigDecimal salaryMax,
		LocalDate publicationDate,
		OfferStatus status,
		List<OfferRequirementDto> requirements) {
}
