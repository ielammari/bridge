package io.github.ielammari.bridge.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.service.HealthService;

/**
 * Reports application and database availability, plus the size of the traits
 * catalogue, for deployment and monitoring checks.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping("/health")
	public Map<String, Object> health() {
		return Map.of(
				"application", "bridge",
				"database", "up",
				"traitCount", healthService.countTraits());
	}

}
