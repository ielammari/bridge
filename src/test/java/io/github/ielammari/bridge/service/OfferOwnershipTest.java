package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.EducationRequest;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.ProvisionAccountRequest;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.EvaluationType;
import io.github.ielammari.bridge.model.Evaluator;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.EvaluationRepository;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * A recruiter runs the offers they published and nothing else. Everything that
 * arrives through another recruiter's offer answers as though it did not exist.
 */
@SpringBootTest
@Transactional
class OfferOwnershipTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private HistoryService historyService;
	@Autowired private PeopleService peopleService;
	@Autowired private SettingsService settingsService;
	@Autowired private ApplicationRepository applications;
	@Autowired private EvaluationRepository evaluations;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer owner() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer expertId() {
		return users.findByEmailIgnoreCase("expert@bridge.local").orElseThrow().getId();
	}

	/** The second recruiter of the deployment, created once per test. */
	private Integer otherRecruiter() {
		return users.findByEmailIgnoreCase("second.rh@bridge.local")
				.map(account -> account.getId())
				.orElseGet(() -> settingsService.provision(new ProvisionAccountRequest(
						"second.rh@bridge.local", "Claire", "Fontaine", "Recrutement2026!x", Role.RH)).id());
	}

	private Trait aTrait() {
		return traits.findAll().get(0);
	}

	private Integer offerOf(Integer hrId) {
		return offerService.create(hrId, new OfferRequest("Poste", null, "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				false, List.of(new RequirementSelection(aTrait().getId(), true)), true)).id();
	}

	private Integer candidate(String email) {
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Ada", "Lovelace",
				"0612345678", LocalDate.of(1995, 5, 20), null, "Lyon", "France")).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.addEducation(id, new EducationRequest(
				"Master informatique", "INSA Lyon", "Genie logiciel", (short) 2018, (short) 2020));
		profileService.storeCv(id,
				new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()), null);
		return id;
	}

	@Test
	void theOfferListingHoldsOnlyWhatTheRecruiterPublished() {
		Integer mine = offerOf(owner());
		Integer theirs = offerOf(otherRecruiter());

		assertThat(offerService.listFor(owner())).extracting("id").contains(mine).doesNotContain(theirs);
	}

	@Test
	void anotherRecruitersOfferCannotBeReadOrMoved() {
		Integer theirs = offerOf(otherRecruiter());
		Integer hr = owner();

		assertThatThrownBy(() -> offerService.get(hr, theirs))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> offerService.publish(hr, theirs))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> offerService.close(hr, theirs))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> offerService.detail(hr, Role.RH, theirs))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void theApplicationsToAnotherRecruitersOfferAreOutOfReach() {
		Integer theirs = offerOf(otherRecruiter());
		Integer app = applicationService.apply(candidate("own1@example.fr"), theirs, null).id();
		Integer hr = owner();

		assertThatThrownBy(() -> applicationService.forOffer(hr, theirs))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> applicationService.hrView(hr, app))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> applicationService.loadCv(hr, app))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> evaluationService.review(hr, app))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> evaluationService.preselect(hr, app, Decision.VALIDEE, null))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
		assertThatThrownBy(() -> historyService.trail(hr, Role.RH, app))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void aCandidateWhoAppliedElsewhereIsNotFound() {
		Integer theirs = offerOf(otherRecruiter());
		Integer id = candidate("own2@example.fr");
		applicationService.apply(id, theirs, null);
		Integer hr = owner();

		assertThatThrownBy(() -> peopleService.dossier(hr, Role.RH, id))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

	@Test
	void aDossierCarriesOnlyTheApplicationsThisRecruitersOffersReceived() {
		Integer id = candidate("own3@example.fr");
		Integer mine = offerOf(owner());
		applicationService.apply(id, mine, null);
		applicationService.apply(id, offerOf(otherRecruiter()), null);

		assertThat(peopleService.dossier(owner(), Role.RH, id).applications())
				.singleElement()
				.satisfies(row -> assertThat(row.offerId()).isEqualTo(mine));
	}

	@Test
	void aClosedApplicationOnAnotherRecruitersOfferStaysOutOfTheirHistory() {
		Integer theirs = offerOf(otherRecruiter());
		Integer other = otherRecruiter();
		Integer app = applicationService.apply(candidate("own4@example.fr"), theirs, null).id();
		evaluationService.preselect(other, app, Decision.REFUSEE, "Non retenu");

		assertThat(historyService.closedApplications(owner()))
				.extracting("id").doesNotContain(app);
		assertThat(historyService.closedApplications(other))
				.extracting("id").contains(app);
	}

	/**
	 * An assessment can predate the offer changing hands, so the record is
	 * filtered on the offer as well as on the author.
	 */
	@Test
	void anEvaluationWrittenOnAnotherRecruitersOfferIsNotListedBack() {
		Integer theirs = offerOf(otherRecruiter());
		Integer app = applicationService.apply(candidate("own5@example.fr"), theirs, null).id();
		Evaluator author = (Evaluator) users.findById(owner()).orElseThrow();
		evaluations.save(new Evaluation(EvaluationType.PRESELECTION, Decision.VALIDEE, "Bon dossier",
				applications.findById(app).orElseThrow(), author));

		assertThat(historyService.authored(owner(), Role.RH))
				.extracting("applicationId").doesNotContain(app);
	}

	@Test
	void schedulingOnAnotherRecruitersApplicationIsRefused() {
		Integer theirs = offerOf(otherRecruiter());
		Integer other = otherRecruiter();
		Integer app = applicationService.apply(candidate("own6@example.fr"), theirs, null).id();
		evaluationService.preselect(other, app, Decision.VALIDEE, "Bon dossier");
		Integer hr = owner();

		assertThatThrownBy(() -> appointmentService.schedule(hr, app, LocalDate.of(2099, 7, 1),
				java.time.LocalTime.of(10, 0), expertId()))
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
