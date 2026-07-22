package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.service.MatchingService;
import io.github.ielammari.bridge.service.OfferService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

	private final OfferService offerService;
	private final MatchingService matchingService;

	public OfferController(OfferService offerService, MatchingService matchingService) {
		this.offerService = offerService;
		this.matchingService = matchingService;
	}

	/** Candidate feed: only published offers the candidate qualifies for. */
	@GetMapping("/feed")
	public List<OfferDto> feed(@AuthenticationPrincipal Jwt jwt) {
		return matchingService.feed(currentUserId(jwt));
	}

	@GetMapping
	public List<OfferDto> list() {
		return offerService.listAll();
	}

	@GetMapping("/{id}")
	public OfferDto get(@PathVariable Integer id) {
		return offerService.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OfferDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfferRequest request) {
		return offerService.create(currentUserId(jwt), request);
	}

	@PutMapping("/{id}")
	public OfferDto update(@PathVariable Integer id, @Valid @RequestBody OfferRequest request) {
		return offerService.update(id, request);
	}

	@PostMapping("/{id}/publish")
	public OfferDto publish(@PathVariable Integer id) {
		return offerService.publish(id);
	}

	@PostMapping("/{id}/close")
	public OfferDto close(@PathVariable Integer id) {
		return offerService.close(id);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
