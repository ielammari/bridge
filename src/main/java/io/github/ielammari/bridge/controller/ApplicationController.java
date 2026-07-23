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
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.service.ApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

	private final ApplicationService applicationService;

	public ApplicationController(ApplicationService applicationService) {
		this.applicationService = applicationService;
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

	/** HR: the CV attached to an application. */
	@GetMapping("/{id}/cv")
	public ResponseEntity<Resource> cv(@PathVariable Integer id) {
		Resource cv = applicationService.loadCv(id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
				.body(cv);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
