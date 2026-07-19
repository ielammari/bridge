package io.github.ielammari.bridge.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports application and database availability, plus the size of the traits
 * catalogue, for deployment and monitoring checks.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

	private final JdbcTemplate jdbcTemplate;

	public HealthController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/health")
	public Map<String, Object> health() {
		Integer traitCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trait", Integer.class);
		return Map.of(
				"application", "bridge",
				"database", "up",
				"traitCount", traitCount == null ? 0 : traitCount);
	}

}
