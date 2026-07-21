package io.github.ielammari.bridge.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite key for the candidate to trait association. */
@Embeddable
public class CandidateTraitId implements Serializable {

	@Column(name = "id_candidat")
	private Integer candidateId;

	@Column(name = "id_trait")
	private Integer traitId;

	protected CandidateTraitId() {
	}

	public CandidateTraitId(Integer candidateId, Integer traitId) {
		this.candidateId = candidateId;
		this.traitId = traitId;
	}

	public Integer getCandidateId() {
		return candidateId;
	}

	public Integer getTraitId() {
		return traitId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CandidateTraitId that)) {
			return false;
		}
		return Objects.equals(candidateId, that.candidateId) && Objects.equals(traitId, that.traitId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(candidateId, traitId);
	}

}
