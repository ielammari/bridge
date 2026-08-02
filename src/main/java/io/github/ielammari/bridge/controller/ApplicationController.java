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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.ApplicationDto;
import io.github.ielammari.bridge.dto.ApplyRequest;
import io.github.ielammari.bridge.dto.FinalEvaluationRequest;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.dto.PreselectionRequest;
import io.github.ielammari.bridge.dto.ScheduleRequest;
import io.github.ielammari.bridge.service.ApplicationService;
import io.github.ielammari.bridge.service.AppointmentService;
import io.github.ielammari.bridge.service.EvaluationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

	private final ApplicationService applicationService;
	private final EvaluationService evaluationService;
	private final AppointmentService appointmentService;

	public ApplicationController(ApplicationService applicationService,
			EvaluationService evaluationService, AppointmentService appointmentService) {
		this.applicationService = applicationService;
		this.evaluationService = evaluationService;
		this.appointmentService = appointmentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApplicationDto apply(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ApplyRequest request) {
		return applicationService.apply(currentUserId(jwt), request.offerId());
	}

	/** The candidate's own applications, for the tracking page. */
	@GetMapping("/mine")
	public List<ApplicationDto> mine(@AuthenticationPrincipal Jwt jwt) {
		return applicationService.forCandidate(currentUserId(jwt));
	}

	/** HR: the applications for one offer. */
	@GetMapping
	public List<HrApplicationDto> byOffer(@RequestParam Integer offerId) {
		return applicationService.forOffer(offerId);
	}

	/** HR: one application, so a review in progress has its own address. */
	@GetMapping("/{id}")
	public HrApplicationDto one(@PathVariable Integer id) {
		return applicationService.hrView(id);
	}

	/** HR: the CV attached to an application. */
	@GetMapping("/{id}/cv")
	public ResponseEntity<Resource> cv(@PathVariable Integer id) {
		Resource cv = applicationService.loadCv(id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
				.body(cv);
	}

	/** HR: opening an application to inspect it moves it into review. */
	@PostMapping("/{id}/review")
	public HrApplicationDto review(@PathVariable Integer id) {
		return evaluationService.review(id);
	}

	/** HR: the first screening decision. */
	@PostMapping("/{id}/preselection")
	public HrApplicationDto preselect(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody PreselectionRequest request) {
		return evaluationService.preselect(currentUserId(jwt), id, request.decision(), request.comment());
	}

	/** HR: book or move the interview this application is waiting on. */
	@PostMapping("/{id}/schedule")
	public HrApplicationDto schedule(@PathVariable Integer id, @Valid @RequestBody ScheduleRequest request) {
		return appointmentService.schedule(id, request.date(), request.time());
	}

	/** HR: record the final interview and close the application. */
	@PostMapping("/{id}/finalize")
	public HrApplicationDto finalize(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody FinalEvaluationRequest request) {
		return evaluationService.finalize(currentUserId(jwt), id, request);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
