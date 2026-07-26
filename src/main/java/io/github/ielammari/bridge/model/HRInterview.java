package io.github.ielammari.bridge.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
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

/**
 * The data gathered at the final HR interview. Kept even when the outcome is a
 * refusal, so one exists per application once the interview has taken place.
 */
@Entity
@Table(name = "entretien_rh")
public class HRInterview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_entretien_rh")
	private Integer id;

	@Column(name = "salaire_attendu", precision = 12, scale = 2)
	private BigDecimal expectedSalary;

	@Column(name = "date_disponibilite")
	private LocalDate availabilityDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_contrat_envisage", length = 30)
	private ContractType envisagedContract;

	@Column(name = "duree_preavis", length = 30)
	private String noticePeriod;

	@Column(name = "flexibilite_horaire", length = 50)
	private String scheduleFlexibility;

	@Enumerated(EnumType.STRING)
	@Column(name = "attentes_teletravail", length = 30)
	private RemoteMode remoteExpectation;

	@Column(name = "adequation_culture", columnDefinition = "text")
	private String cultureFit;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidature", nullable = false, unique = true)
	private Application application;

	protected HRInterview() {
	}

	public HRInterview(Application application) {
		this.application = application;
	}

	public void setExpectedSalary(BigDecimal expectedSalary) {
		this.expectedSalary = expectedSalary;
	}

	public void setAvailabilityDate(LocalDate availabilityDate) {
		this.availabilityDate = availabilityDate;
	}

	public void setEnvisagedContract(ContractType envisagedContract) {
		this.envisagedContract = envisagedContract;
	}

	public void setNoticePeriod(String noticePeriod) {
		this.noticePeriod = noticePeriod;
	}

	public void setScheduleFlexibility(String scheduleFlexibility) {
		this.scheduleFlexibility = scheduleFlexibility;
	}

	public void setRemoteExpectation(RemoteMode remoteExpectation) {
		this.remoteExpectation = remoteExpectation;
	}

	public void setCultureFit(String cultureFit) {
		this.cultureFit = cultureFit;
	}

	public Integer getId() {
		return id;
	}

}
