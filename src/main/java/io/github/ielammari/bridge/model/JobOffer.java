package io.github.ielammari.bridge.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** A job offer configured by HR, matched to candidates through its traits. */
@Entity
@Table(name = "offre")
public class JobOffer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_offre")
	private Integer id;

	@Column(name = "titre", nullable = false, length = 150)
	private String title;

	@Column(name = "entreprise", nullable = false, length = 120)
	private String company;

	@Column(name = "description", nullable = false, columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "diplome_requis", nullable = false, length = 80)
	private Degree requiredDegree;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_contrat", nullable = false, length = 30)
	private ContractType contractType;

	@Column(name = "localisation", length = 120)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(name = "modalite_teletravail", length = 30)
	private RemoteMode remoteMode;

	@Column(name = "salaire_min", precision = 12, scale = 2)
	private BigDecimal salaryMin;

	@Column(name = "salaire_max", precision = 12, scale = 2)
	private BigDecimal salaryMax;

	@Column(name = "date_publication", nullable = false)
	private LocalDate publicationDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private OfferStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_rh", nullable = false)
	private HRManager publisher;

	@OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OfferRequirement> requirements = new ArrayList<>();

	protected JobOffer() {
	}

	public JobOffer(HRManager publisher) {
		this.publisher = publisher;
		this.status = OfferStatus.BROUILLON;
		this.publicationDate = LocalDate.now();
	}

	public void addRequirement(Trait trait, boolean mandatory) {
		requirements.add(new OfferRequirement(this, trait, mandatory));
	}

	public void clearRequirements() {
		requirements.clear();
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Degree getRequiredDegree() {
		return requiredDegree;
	}

	public void setRequiredDegree(Degree requiredDegree) {
		this.requiredDegree = requiredDegree;
	}

	public ContractType getContractType() {
		return contractType;
	}

	public void setContractType(ContractType contractType) {
		this.contractType = contractType;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public RemoteMode getRemoteMode() {
		return remoteMode;
	}

	public void setRemoteMode(RemoteMode remoteMode) {
		this.remoteMode = remoteMode;
	}

	public BigDecimal getSalaryMin() {
		return salaryMin;
	}

	public void setSalaryMin(BigDecimal salaryMin) {
		this.salaryMin = salaryMin;
	}

	public BigDecimal getSalaryMax() {
		return salaryMax;
	}

	public void setSalaryMax(BigDecimal salaryMax) {
		this.salaryMax = salaryMax;
	}

	public LocalDate getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(LocalDate publicationDate) {
		this.publicationDate = publicationDate;
	}

	public OfferStatus getStatus() {
		return status;
	}

	public void setStatus(OfferStatus status) {
		this.status = status;
	}

	public HRManager getPublisher() {
		return publisher;
	}

	public List<OfferRequirement> getRequirements() {
		return requirements;
	}

}
