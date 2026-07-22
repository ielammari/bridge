package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A trait an offer looks for, flagged required (true) or a plus (false). */
@Entity
@Table(name = "exiger")
public class OfferRequirement {

	@EmbeddedId
	private OfferRequirementId id;

	@MapsId("offerId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_offre", nullable = false)
	private JobOffer offer;

	@MapsId("traitId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_trait", nullable = false)
	private Trait trait;

	@Column(name = "est_obligatoire", nullable = false)
	private boolean mandatory;

	protected OfferRequirement() {
	}

	public OfferRequirement(JobOffer offer, Trait trait, boolean mandatory) {
		this.id = new OfferRequirementId(offer.getId(), trait.getId());
		this.offer = offer;
		this.trait = trait;
		this.mandatory = mandatory;
	}

	public OfferRequirementId getId() {
		return id;
	}

	public JobOffer getOffer() {
		return offer;
	}

	public Trait getTrait() {
		return trait;
	}

	public boolean isMandatory() {
		return mandatory;
	}

}
