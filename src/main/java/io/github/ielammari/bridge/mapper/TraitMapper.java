package io.github.ielammari.bridge.mapper;

import java.util.ArrayList;
import java.util.List;

import io.github.ielammari.bridge.dto.CandidateTraitDto;
import io.github.ielammari.bridge.dto.TraitCategoryDto;
import io.github.ielammari.bridge.dto.TraitDto;
import io.github.ielammari.bridge.model.CandidateTrait;
import io.github.ielammari.bridge.model.Trait;

/** Pure transformations between trait entities and their DTOs. */
public final class TraitMapper {

	private TraitMapper() {
	}

	/** Groups a flat, category ordered trait list into category buckets. */
	public static List<TraitCategoryDto> toCatalogue(List<Trait> traitsOrderedByCategory) {
		List<TraitCategoryDto> catalogue = new ArrayList<>();
		Integer currentCategoryId = null;
		List<TraitDto> current = null;

		for (Trait trait : traitsOrderedByCategory) {
			Integer categoryId = trait.getCategory().getId();
			if (!categoryId.equals(currentCategoryId)) {
				current = new ArrayList<>();
				catalogue.add(new TraitCategoryDto(categoryId, trait.getCategory().getLabel(), current));
				currentCategoryId = categoryId;
			}
			current.add(new TraitDto(trait.getId(), trait.getLabel()));
		}

		return catalogue;
	}

	public static CandidateTraitDto toDto(CandidateTrait candidateTrait) {
		Trait trait = candidateTrait.getTrait();
		return new CandidateTraitDto(
				trait.getId(),
				trait.getLabel(),
				trait.getCategory().getLabel(),
				candidateTrait.getLevel());
	}

}
