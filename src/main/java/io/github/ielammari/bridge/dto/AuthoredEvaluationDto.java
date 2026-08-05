package io.github.ielammari.bridge.dto;

/** An evaluation in the list of those one evaluator has written. */
public record AuthoredEvaluationDto(
		EvaluationDto evaluation,
		Integer applicationId,
		Integer candidateId,
		String candidateName,
		String offerTitle) {
}
