package io.github.ielammari.bridge.dto;

/** A row of the hires register. */
public record HiringRecordDto(
		HiringDto hiring,
		Integer applicationId,
		Integer candidateId,
		String candidateName,
		String candidateEmail,
		String offerTitle) {
}
