package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A single matchable attribute (a skill, a language, an experience level, a plus). */
@Entity
@Table(name = "trait")
public class Trait {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_trait")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_categorie", nullable = false)
	private TraitCategory category;

	@Column(name = "libelle", nullable = false, length = 120)
	private String label;

	protected Trait() {
	}

	public Integer getId() {
		return id;
	}

	public TraitCategory getCategory() {
		return category;
	}

	public String getLabel() {
		return label;
	}

}
