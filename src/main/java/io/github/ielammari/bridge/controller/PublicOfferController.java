package io.github.ielammari.bridge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.PublicMarketDto;
import io.github.ielammari.bridge.dto.PublicOfferDetailDto;
import io.github.ielammari.bridge.service.PublicOfferService;

/** The open positions, for a reader who has no account. */
@RestController
@RequestMapping("/api/v1/public/offers")
public class PublicOfferController {

	private final PublicOfferService offers;

	public PublicOfferController(PublicOfferService offers) {
		this.offers = offers;
	}

	@GetMapping
	public PublicMarketDto market() {
		return offers.market();
	}

	@GetMapping("/{id}")
	public PublicOfferDetailDto detail(@PathVariable Integer id) {
		return offers.detail(id);
	}

}
