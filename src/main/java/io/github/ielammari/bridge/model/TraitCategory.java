package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A grouping of traits, such as technical skills or languages. */
@Entity
@Table(name = "categorie_trait")
public class TraitCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_categorie")
	private Integer id;

	@Column(name = "libelle", nullable = false, length = 80)
	private String label;

	/** Position in a picker, independent of creation order. */
	@Column(name = "ordre", nullable = false)
	private short displayOrder;

	protected TraitCategory() {
	}

	public Integer getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public short getDisplayOrder() {
		return displayOrder;
	}

}
