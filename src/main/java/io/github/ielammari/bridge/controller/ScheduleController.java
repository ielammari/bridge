package io.github.ielammari.bridge.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.DayScheduleDto;
import io.github.ielammari.bridge.service.AppointmentService;

@RestController
@RequestMapping("/api/v1/schedule")
public class ScheduleController {

	private final AppointmentService appointmentService;

	public ScheduleController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	/** HR: the hourly grid for a day, showing which slots are taken. */
	@GetMapping("/day")
	public DayScheduleDto day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return appointmentService.day(date);
	}

}
