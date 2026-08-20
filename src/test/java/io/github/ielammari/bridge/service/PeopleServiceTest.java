package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.CandidateDossierDto;
import io.github.ielammari.bridge.dto.EducationRequest;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Who may open a person, and what they find when they do. */
@SpringBootTest
@Transactional
class PeopleServiceTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private PeopleService peopleService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	private Integer candidate(String email) {
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Ada", "Lovelace",
				"0612345678", LocalDate.of(1995, 5, 20), null, "Lyon", "France")).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.addEducation(id, new EducationRequest(
				"Master informatique", "INSA Lyon", "Genie logiciel", (short) 2018, (short) 2020));
		profileService.storeCv(id, new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()), null);
		return id;
	}

	private Integer publishedOffer() {
		OfferDto offer = offerService.create(hrId(), new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		return offer.id();
	}

	/** Carries an application as far as the expert's exam. */
	private Integer examinedBy(Integer candidateId, LocalDate slot) {
		Integer app = applicationService.apply(candidateId, publishedOffer(), null).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, "Bon dossier");
		appointmentService.schedule(hrId(), app, slot, LocalTime.of(10, 0), expertId());
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.VALIDEE, "Solide", List.of(new Score(aTrait().getId(), (short) 9))));
		return app;
	}

	@Test
	void theDossierGathersEverythingScatteredAcrossTheirApplications() {
		Integer id = candidate("p1@example.fr");
		applicationService.apply(id, publishedOffer(), null);

		CandidateDossierDto dossier = peopleService.dossier(hrId(), Role.RH, id);

		assertThat(dossier.firstName()).isEqualTo("Ada");
		assertThat(dossier.phone()).isEqualTo("0612345678");
		assertThat(dossier.birthDate()).isEqualTo(LocalDate.of(1995, 5, 20));
		assertThat(dossier.city()).isEqualTo("Lyon");
		assertThat(dossier.degree()).isEqualTo(Degree.BAC_5);
		assertThat(dossier.hasCv()).isTrue();
		assertThat(dossier.education()).singleElement()
				.satisfies(e -> assertThat(e.title()).isEqualTo("Master informatique"));
		assertThat(dossier.traits()).hasSize(1);
		assertThat(dossier.applications()).hasSize(1);
	}

	@Test
	void aCandidateOpensThemselvesAndNobodyElse() {
		Integer mine = candidate("p2@example.fr");
		Integer theirs = candidate("p3@example.fr");

		assertThat(peopleService.dossier(mine, Role.CANDIDAT, mine).id()).isEqualTo(mine);

		assertThatThrownBy(() -> peopleService.dossier(mine, Role.CANDIDAT, theirs))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	/** An exam is the only reason an expert holds anyone's details. */
	@Test
	void anExpertOpensOnlyTheCandidatesTheyHaveAssessed() {
		Integer assessed = candidate("p4@example.fr");
		Integer stranger = candidate("p5@example.fr");
		examinedBy(assessed, LocalDate.of(2099, 7, 1));

		assertThat(peopleService.dossier(expertId(), Role.EXPERT, assessed).id()).isEqualTo(assessed);

		assertThatThrownBy(() -> peopleService.dossier(expertId(), Role.EXPERT, stranger))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	/**
	 * The exam entitles the expert to that application, not to whatever else the
	 * candidate has running. A recruiter, who owns the funnel, sees both.
	 */
	@Test
	void anExpertSeesOnlyTheApplicationsTheyAssessed() {
		Integer id = candidate("p8@example.fr");
		Integer examined = examinedBy(id, LocalDate.of(2099, 7, 2));
		applicationService.apply(id, publishedOffer(), null);

		assertThat(peopleService.dossier(expertId(), Role.EXPERT, id).applications())
				.singleElement()
				.satisfies(a -> assertThat(a.id()).isEqualTo(examined));

		assertThat(peopleService.dossier(hrId(), Role.RH, id).applications()).hasSize(2);
	}

	/**
	 * Refused rather than reported as forbidden: a distinct answer would let the
	 * address be used to find out who exists.
	 */
	@Test
	void anAccountThatIsNotACandidateIsNotFound() {
		assertThatThrownBy(() -> peopleService.dossier(hrId(), Role.RH, expertId()))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void theCvFollowsTheSameEntitlementAsTheDossier() {
		Integer id = candidate("p6@example.fr");
		Integer other = candidate("p7@example.fr");
		applicationService.apply(id, publishedOffer(), null);

		assertThat(peopleService.cv(hrId(), Role.RH, id)).isNotNull();

		assertThatThrownBy(() -> peopleService.cv(other, Role.CANDIDAT, id))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
