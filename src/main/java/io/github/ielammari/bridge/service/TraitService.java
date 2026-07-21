package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.TraitCategoryDto;
import io.github.ielammari.bridge.mapper.TraitMapper;
import io.github.ielammari.bridge.repository.TraitRepository;

@Service
public class TraitService {

	private final TraitRepository traits;

	public TraitService(TraitRepository traits) {
		this.traits = traits;
	}

	/** The full catalogue, grouped by category, for the profile and offer pickers. */
	@Transactional(readOnly = true)
	public List<TraitCategoryDto> catalogue() {
		return TraitMapper.toCatalogue(traits.findAllWithCategory());
	}

}
