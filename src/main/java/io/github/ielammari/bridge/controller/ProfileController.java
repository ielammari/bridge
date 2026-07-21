package io.github.ielammari.bridge.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.ielammari.bridge.dto.CandidateProfileDto;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.service.ProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping
	public CandidateProfileDto read(@AuthenticationPrincipal Jwt jwt) {
		return profileService.read(currentUserId(jwt));
	}

	@PutMapping
	public CandidateProfileDto update(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return profileService.update(currentUserId(jwt), request);
	}

	@PostMapping("/cv")
	public CandidateProfileDto uploadCv(@AuthenticationPrincipal Jwt jwt,
			@RequestParam("file") MultipartFile file) {
		Integer candidateId = currentUserId(jwt);
		profileService.storeCv(candidateId, file);
		return profileService.read(candidateId);
	}

	@GetMapping("/cv")
	public ResponseEntity<Resource> downloadCv(@AuthenticationPrincipal Jwt jwt) {
		Resource cv = profileService.loadCv(currentUserId(jwt));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv.pdf\"")
				.body(cv);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
