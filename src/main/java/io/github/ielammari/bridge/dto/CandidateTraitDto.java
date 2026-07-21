package io.github.ielammari.bridge.dto;

/** A trait the candidate holds, with its optional self declared level. */
public record CandidateTraitDto(Integer traitId, String label, String categoryLabel, String level) {
}
