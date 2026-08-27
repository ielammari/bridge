package io.github.ielammari.bridge.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.OfferDetailDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferMatchDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.service.MatchingService;
import io.github.ielammari.bridge.service.OfferService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

	/** The scope that keeps the offers the candidate does not qualify for. */
	private static final String ALL_SCOPE = "all";

	private final OfferService offerService;
	private final MatchingService matchingService;

	public OfferController(OfferService offerService, MatchingService matchingService) {
		this.offerService = offerService;
		this.matchingService = matchingService;
	}

	/**
	 * Candidate feed. The default scope is the offers they qualify for; scope
	 * `all` adds the rest of what is published, marked with what they lack.
	 */
	@GetMapping("/feed")
	public List<OfferMatchDto> feed(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "compatible") String scope) {
		return matchingService.feed(currentUserId(jwt), !ALL_SCOPE.equalsIgnoreCase(scope));
	}

	/** HR: the offers this recruiter published. */
	@GetMapping
	public List<OfferDto> list(@AuthenticationPrincipal Jwt jwt) {
		return offerService.listFor(currentUserId(jwt));
	}

	@GetMapping("/{id}")
	public OfferDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return offerService.get(currentUserId(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OfferDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfferRequest request) {
		return offerService.create(currentUserId(jwt), request);
	}

	@PutMapping("/{id}")
	public OfferDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id,
			@Valid @RequestBody OfferRequest request) {
		return offerService.update(currentUserId(jwt), id, request);
	}

	@PostMapping("/{id}/publish")
	public OfferDto publish(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return offerService.publish(currentUserId(jwt), id);
	}

	@PostMapping("/{id}/close")
	public OfferDto close(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return offerService.close(currentUserId(jwt), id);
	}

	/** One offer in full, at its own address, for any signed in reader. */
	@GetMapping("/{id}/detail")
	public OfferDetailDto detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return offerService.detail(currentUserId(jwt), currentRole(jwt), id);
	}

	/** Candidate: the offers they kept to come back to. */
	@GetMapping("/saved")
	public List<OfferMatchDto> saved(@AuthenticationPrincipal Jwt jwt) {
		return offerService.savedFor(currentUserId(jwt));
	}

	@PutMapping("/{id}/saved")
	public Map<String, Boolean> save(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return Map.of("saved", offerService.setSaved(currentUserId(jwt), id, true));
	}

	@DeleteMapping("/{id}/saved")
	public Map<String, Boolean> unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		return Map.of("saved", offerService.setSaved(currentUserId(jwt), id, false));
	}

	private Role currentRole(Jwt jwt) {
		return Role.valueOf(jwt.getClaimAsString("role"));
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
