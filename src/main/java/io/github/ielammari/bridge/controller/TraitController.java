package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.TraitCategoryDto;
import io.github.ielammari.bridge.service.TraitService;

@RestController
@RequestMapping("/api/v1/traits")
public class TraitController {

	private final TraitService traitService;

	public TraitController(TraitService traitService) {
		this.traitService = traitService;
	}

	/** The trait catalogue grouped by category. */
	@GetMapping
	public List<TraitCategoryDto> catalogue() {
		return traitService.catalogue();
	}

}
