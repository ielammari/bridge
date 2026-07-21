package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A trait a candidate holds, with an optional self declared level. */
@Entity
@Table(name = "posseder")
public class CandidateTrait {

	@EmbeddedId
	private CandidateTraitId id;

	@MapsId("traitId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_trait", nullable = false)
	private Trait trait;

	@Column(name = "niveau", length = 30)
	private String level;

	protected CandidateTrait() {
	}

	public CandidateTrait(Integer candidateId, Trait trait, String level) {
		this.id = new CandidateTraitId(candidateId, trait.getId());
		this.trait = trait;
		this.level = level;
	}

	public CandidateTraitId getId() {
		return id;
	}

	public Trait getTrait() {
		return trait;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

}
