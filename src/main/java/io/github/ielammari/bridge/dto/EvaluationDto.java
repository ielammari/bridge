package io.github.ielammari.bridge.dto;

import java.time.Instant;
import java.util.List;

import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.EvaluationType;

/** A recorded evaluation. Trait scores are present on technical ones only. */
public record EvaluationDto(
		Integer id,
		EvaluationType type,
		Decision decision,
		String comment,
		Instant date,
		String evaluatorName,
		List<TraitScoreDto> scores) {
}
