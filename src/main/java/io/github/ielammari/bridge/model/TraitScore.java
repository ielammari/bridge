package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A trait's score within a technical evaluation, on a 0 to 30 scale. */
@Entity
@Table(name = "noter")
public class TraitScore {

	@EmbeddedId
	private TraitScoreId id;

	@MapsId("evaluationId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_evaluation", nullable = false)
	private Evaluation evaluation;

	@MapsId("traitId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_trait", nullable = false)
	private Trait trait;

	@Column(name = "note", nullable = false)
	private short note;

	protected TraitScore() {
	}

	public TraitScore(Evaluation evaluation, Trait trait, short note) {
		this.id = new TraitScoreId(evaluation.getId(), trait.getId());
		this.evaluation = evaluation;
		this.trait = trait;
		this.note = note;
	}

	public TraitScoreId getId() {
		return id;
	}

	public Trait getTrait() {
		return trait;
	}

	public short getNote() {
		return note;
	}

}
