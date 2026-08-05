package io.github.ielammari.bridge.dto;

/** One trait as an expert scored it, in half star units from 0 to 10. */
public record TraitScoreDto(Integer traitId, String label, short note) {
}
