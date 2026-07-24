package io.github.ielammari.bridge.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite key for a per trait score within an evaluation. */
@Embeddable
public class TraitScoreId implements Serializable {

	@Column(name = "id_evaluation")
	private Integer evaluationId;

	@Column(name = "id_trait")
	private Integer traitId;

	protected TraitScoreId() {
	}

	public TraitScoreId(Integer evaluationId, Integer traitId) {
		this.evaluationId = evaluationId;
		this.traitId = traitId;
	}

	public Integer getEvaluationId() {
		return evaluationId;
	}

	public Integer getTraitId() {
		return traitId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TraitScoreId that)) {
			return false;
		}
		return Objects.equals(evaluationId, that.evaluationId) && Objects.equals(traitId, that.traitId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(evaluationId, traitId);
	}

}
