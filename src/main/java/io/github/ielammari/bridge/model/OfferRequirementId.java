package io.github.ielammari.bridge.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite key for the offer to trait requirement. */
@Embeddable
public class OfferRequirementId implements Serializable {

	@Column(name = "id_offre")
	private Integer offerId;

	@Column(name = "id_trait")
	private Integer traitId;

	protected OfferRequirementId() {
	}

	public OfferRequirementId(Integer offerId, Integer traitId) {
		this.offerId = offerId;
		this.traitId = traitId;
	}

	public Integer getOfferId() {
		return offerId;
	}

	public Integer getTraitId() {
		return traitId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OfferRequirementId that)) {
			return false;
		}
		return Objects.equals(offerId, that.offerId) && Objects.equals(traitId, that.traitId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(offerId, traitId);
	}

}
