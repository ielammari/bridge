package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.RemoteMode;

/** What the final HR interview recorded, kept whatever the outcome. */
public record HRInterviewDto(
		BigDecimal expectedSalary,
		LocalDate availabilityDate,
		ContractType envisagedContract,
		String noticePeriod,
		String scheduleFlexibility,
		RemoteMode remoteExpectation,
		String cultureFit) {
}
