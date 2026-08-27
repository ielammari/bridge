package io.github.ielammari.bridge.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.CalendarDto;
import io.github.ielammari.bridge.model.CalendarScope;
import io.github.ielammari.bridge.service.CalendarService;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

	private final CalendarService calendarService;

	public CalendarController(CalendarService calendarService) {
		this.calendarService = calendarService;
	}

	/** The interviews over a range, scoped to what the caller may read. */
	@GetMapping
	public CalendarDto read(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "MINE") CalendarScope scope,
			@RequestParam(required = false) Integer evaluatorId,
			@AuthenticationPrincipal Jwt jwt) {
		return calendarService.read(Integer.valueOf(jwt.getSubject()), scope, evaluatorId, from, to);
	}

}
