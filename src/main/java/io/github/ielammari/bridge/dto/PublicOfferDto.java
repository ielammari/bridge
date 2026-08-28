package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.RemoteMode;

/**
 * One open position as a visitor reads it. {@code domains} names the trait
 * categories the offer looks for, which is what the public listing filters on.
 */
public record PublicOfferDto(
		Integer id,
		String title,
		String company,
		String location,
		ContractType contractType,
		RemoteMode remoteMode,
		BigDecimal salaryMin,
		BigDecimal salaryMax,
		LocalDate publicationDate,
		List<String> domains) {
}
