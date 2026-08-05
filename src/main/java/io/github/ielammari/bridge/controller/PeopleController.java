package io.github.ielammari.bridge.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.CandidateDossierDto;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.service.PeopleService;

@RestController
@RequestMapping("/api/v1/people")
public class PeopleController {

	private final PeopleService peopleService;

	public PeopleController(PeopleService peopleService) {
		this.peopleService = peopleService;
	}

	@GetMapping("/{id}")
	public CandidateDossierDto dossier(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return peopleService.dossier(currentUserId(jwt), currentRole(jwt), id);
	}

	@GetMapping("/{id}/cv")
	public ResponseEntity<Resource> cv(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		Resource cv = peopleService.cv(currentUserId(jwt), currentRole(jwt), id);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
				.body(cv);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

	private Role currentRole(Jwt jwt) {
		return Role.valueOf(jwt.getClaimAsString("role"));
	}

}
