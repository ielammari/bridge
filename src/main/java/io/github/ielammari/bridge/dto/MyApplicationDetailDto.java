package io.github.ielammari.bridge.dto;

import java.util.List;

/**
 * A candidate's own application in full: the facts about it, the interviews it
 * went through, and the terms if it ended in a hire. Carries no assessment.
 */
public record MyApplicationDetailDto(
		ApplicationDto application,
		List<AppointmentDto> appointments,
		HiringDto hiring) {
}
