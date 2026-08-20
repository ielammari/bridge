package io.github.ielammari.bridge.model;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** An offer a candidate kept to come back to. */
@Entity
@Table(name = "offre_enregistree")
@IdClass(SavedOffer.Key.class)
public class SavedOffer {

	@Id
	@Column(name = "id_candidat")
	private Integer candidateId;

	@Id
	@Column(name = "id_offre")
	private Integer offerId;

	@Column(name = "date_enregistrement", nullable = false)
	private Instant savedAt;

	protected SavedOffer() {
	}

	public SavedOffer(Integer candidateId, Integer offerId) {
		this.candidateId = candidateId;
		this.offerId = offerId;
		this.savedAt = Instant.now();
	}

	public Integer getCandidateId() {
		return candidateId;
	}

	public Integer getOfferId() {
		return offerId;
	}

	public Instant getSavedAt() {
		return savedAt;
	}

	/** Composite key: one row per candidate and offer. */
	public static final class Key implements Serializable {

		private Integer candidateId;
		private Integer offerId;

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Key key)) {
				return false;
			}
			return java.util.Objects.equals(candidateId, key.candidateId)
					&& java.util.Objects.equals(offerId, key.offerId);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(candidateId, offerId);
		}
	}

}
