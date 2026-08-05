package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Settings that belong to the company. A single row, held at id 1. */
@Entity
@Table(name = "parametre_organisation")
public class OrganisationSettings {

	public static final short SINGLETON_ID = 1;

	@Id
	@Column(name = "id")
	private Short id;

	@Column(name = "premiere_heure", nullable = false)
	private short firstHour;

	@Column(name = "derniere_heure", nullable = false)
	private short lastHour;

	protected OrganisationSettings() {
	}

	public void setHours(short firstHour, short lastHour) {
		this.firstHour = firstHour;
		this.lastHour = lastHour;
	}

	public short getFirstHour() {
		return firstHour;
	}

	public short getLastHour() {
		return lastHour;
	}

}
