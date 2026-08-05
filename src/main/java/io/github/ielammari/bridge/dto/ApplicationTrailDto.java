package io.github.ielammari.bridge.dto;

import java.util.List;

/**
 * Everything the funnel recorded about one application, for the actors who ran
 * it. Never served to the candidate: the evaluations and the interview notes
 * are assessments of them.
 */
public record ApplicationTrailDto(
		HrApplicationDto application,
		List<AppointmentDto> appointments,
		List<EvaluationDto> evaluations,
		HRInterviewDto interview,
		HiringDto hiring) {
}
