package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.ApplicationTrailDto;
import io.github.ielammari.bridge.dto.AuthoredEvaluationDto;
import io.github.ielammari.bridge.dto.HiringRecordDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.dto.MyApplicationDetailDto;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.service.HistoryService;

/** Reads back the record the funnel leaves behind. */
@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

	private final HistoryService historyService;

	public HistoryController(HistoryService historyService) {
		this.historyService = historyService;
	}

	/** A candidate's own application in full. */
	@GetMapping("/mine/{applicationId}")
	public MyApplicationDetailDto mine(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer applicationId) {
		return historyService.mine(currentUserId(jwt), applicationId);
	}

	/** Every closed application on this recruiter's own offers. */
	@GetMapping("/applications")
	public List<HrApplicationDto> closed(@AuthenticationPrincipal Jwt jwt) {
		return historyService.closedApplications(currentUserId(jwt));
	}

	/** Every hire, for HR. */
	@GetMapping("/hirings")
	public List<HiringRecordDto> hirings() {
		return historyService.hires();
	}

	/** The evaluations the caller has written. */
	@GetMapping("/evaluations")
	public List<AuthoredEvaluationDto> authored(@AuthenticationPrincipal Jwt jwt) {
		return historyService.authored(currentUserId(jwt), currentRole(jwt));
	}

	/** The full trail of one application, for the actors who ran it. */
	@GetMapping("/applications/{applicationId}")
	public ApplicationTrailDto trail(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer applicationId) {
		return historyService.trail(currentUserId(jwt), currentRole(jwt), applicationId);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

	private Role currentRole(Jwt jwt) {
		return Role.valueOf(jwt.getClaimAsString("role"));
	}

}
