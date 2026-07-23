package io.github.ielammari.bridge.dto;

import java.time.Instant;

import io.github.ielammari.bridge.model.ApplicationStatus;

/** An application as HR sees it in the list for one offer. */
public record HrApplicationDto(
		Integer id,
		Integer candidateId,
		String candidateFirstName,
		String candidateLastName,
		String candidateEmail,
		Instant applicationDate,
		ApplicationStatus status) {
}
