package io.github.ielammari.bridge.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Reads the availability signals reported by the health endpoint. */
@Service
public class HealthService {

	private final JdbcTemplate jdbcTemplate;

	public HealthService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int countTraits() {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trait", Integer.class);
		return count == null ? 0 : count;
	}

}
