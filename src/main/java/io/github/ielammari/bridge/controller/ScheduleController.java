package io.github.ielammari.bridge.controller;

import java.time.LocalDate;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.DayScheduleDto;
import io.github.ielammari.bridge.dto.ExpertSummaryDto;
import io.github.ielammari.bridge.service.AppointmentService;

@RestController
@RequestMapping("/api/v1/schedule")
public class ScheduleController {

	private final AppointmentService appointmentService;

	public ScheduleController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	/** HR: one evaluator's hourly grid for a day, showing which slots they hold. */
	@GetMapping("/day")
	public DayScheduleDto day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Integer evaluatorId,
			@AuthenticationPrincipal Jwt jwt) {
		return appointmentService.day(date,
				evaluatorId == null ? Integer.valueOf(jwt.getSubject()) : evaluatorId);
	}

	/** HR: the experts an exam can be handed to, with their load that week. */
	@GetMapping("/experts")
	public List<ExpertSummaryDto> experts(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return appointmentService.experts(date);
	}

}
