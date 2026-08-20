package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.PendingTechnicalDto;
import io.github.ielammari.bridge.dto.TechnicalContextDto;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.service.EvaluationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/evaluations/technical")
public class EvaluationController {

	private final EvaluationService evaluationService;

	public EvaluationController(EvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	/** Expert: applications awaiting the technical exam. */
	@GetMapping("/pending")
	public List<PendingTechnicalDto> pending(@AuthenticationPrincipal Jwt jwt) {
		return evaluationService.pendingTechnical(currentUserId(jwt));
	}

	/** Expert: the scoring grid for one application. */
	@GetMapping("/{applicationId}")
	public TechnicalContextDto context(@AuthenticationPrincipal Jwt jwt,
			@PathVariable Integer applicationId) {
		return evaluationService.technicalContext(currentUserId(jwt), applicationId);
	}

	/** Expert: the CV attached to the application they are examining. */
	@GetMapping("/{applicationId}/cv")
	public ResponseEntity<Resource> cv(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer applicationId) {
		Resource cv = evaluationService.loadCv(currentUserId(jwt), applicationId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
				.body(cv);
	}

	/** Expert: record the technical evaluation. */
	@PostMapping("/{applicationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void evaluate(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer applicationId,
			@Valid @RequestBody TechnicalEvaluationRequest request) {
		evaluationService.evaluateTechnical(currentUserId(jwt), applicationId, request);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
