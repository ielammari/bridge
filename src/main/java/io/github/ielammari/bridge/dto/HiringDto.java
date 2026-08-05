package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.ielammari.bridge.model.ContractType;

/** The agreed terms of a hire. */
public record HiringDto(
		Integer id,
		BigDecimal negotiatedSalary,
		LocalDate startDate,
		ContractType finalContract,
		String trialPeriod,
		Boolean executiveStatus,
		String benefits) {
}
