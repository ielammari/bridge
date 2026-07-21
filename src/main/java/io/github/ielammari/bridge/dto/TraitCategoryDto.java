package io.github.ielammari.bridge.dto;

import java.util.List;

/** A category with the traits it contains, for the profile picker. */
public record TraitCategoryDto(Integer id, String label, List<TraitDto> traits) {
}
