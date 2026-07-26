package io.github.ielammari.bridge.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** The hiring record, created only when the final decision is an acceptance. */
@Entity
@Table(name = "embauche")
public class Hiring {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_embauche")
	private Integer id;

	@Column(name = "salaire_negocie", nullable = false, precision = 12, scale = 2)
	private BigDecimal negotiatedSalary;

	@Column(name = "date_prise_poste", nullable = false)
	private LocalDate startDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_contrat_final", nullable = false, length = 30)
	private ContractType finalContract;

	@Column(name = "duree_periode_essai", length = 30)
	private String trialPeriod;

	@Convert(converter = ExecutiveStatusConverter.class)
	@Column(name = "statut_cadre", length = 20)
	private Boolean executiveStatus;

	@Column(name = "avantages", columnDefinition = "text")
	private String benefits;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidature", nullable = false, unique = true)
	private Application application;

	protected Hiring() {
	}

	public Hiring(Application application, BigDecimal negotiatedSalary, LocalDate startDate,
			ContractType finalContract) {
		this.application = application;
		this.negotiatedSalary = negotiatedSalary;
		this.startDate = startDate;
		this.finalContract = finalContract;
	}

	public void setTrialPeriod(String trialPeriod) {
		this.trialPeriod = trialPeriod;
	}

	public void setExecutiveStatus(Boolean executiveStatus) {
		this.executiveStatus = executiveStatus;
	}

	public void setBenefits(String benefits) {
		this.benefits = benefits;
	}

	public Integer getId() {
		return id;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

}
