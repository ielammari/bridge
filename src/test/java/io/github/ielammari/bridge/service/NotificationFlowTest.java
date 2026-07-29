package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MessageDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest;
import io.github.ielammari.bridge.dto.TechnicalEvaluationRequest.Score;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** Confirms funnel actions drop the right notification into the right inbox. */
@SpringBootTest
@Transactional
class NotificationFlowTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private ApplicationService applicationService;
	@Autowired private EvaluationService evaluationService;
	@Autowired private AppointmentService appointmentService;
	@Autowired private MessageService messageService;
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

	private Integer applyingCandidate(String email) {
		Integer id = authService.register(new RegisterRequest(email, "motdepasse1", "Notif", "Test", null)).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null, null,
				List.of(new TraitSelection(aTrait().getId(), null))));
		profileService.storeCv(id, new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes()));
		return id;
	}

	private Integer publishedOffer() {
		OfferDto offer = offerService.create(hrId(), new OfferRequest("Poste", "d", Degree.BAC,
				ContractType.PERMANENT, "Paris", null, null, null,
				List.of(new RequirementSelection(aTrait().getId(), true)), true));
		return offer.id();
	}

	private boolean hasType(List<MessageDto> inbox, NotificationType type) {
		return inbox.stream().anyMatch(m -> m.type() == type);
	}

	@Test
	void applyingNotifiesThePublishingHr() {
		long before = messageService.inbox(hrId()).size();
		Integer candidate = applyingCandidate("n1@example.fr");
		applicationService.apply(candidate, publishedOffer());

		List<MessageDto> hrInbox = messageService.inbox(hrId());
		assertThat(hrInbox).hasSizeGreaterThan((int) before);
		assertThat(hasType(hrInbox, NotificationType.APPLICATION_RECEIVED)).isTrue();
	}

	@Test
	void preselectionRejectionNotifiesTheCandidate() {
		Integer candidate = applyingCandidate("n2@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		assertThat(hasType(messageService.inbox(candidate), NotificationType.REJECTED)).isTrue();
	}

	@Test
	void schedulingTheExamNotifiesCandidateAndExpert() {
		Integer candidate = applyingCandidate("n3@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		appointmentService.schedule(app, LocalDate.of(2099, 3, 1), LocalTime.of(10, 0));

		assertThat(hasType(messageService.inbox(candidate), NotificationType.INTERVIEW_SCHEDULED)).isTrue();
		assertThat(hasType(messageService.inbox(expertId()), NotificationType.INTERVIEW_SCHEDULED)).isTrue();
	}

	@Test
	void hiringNotifiesTheCandidate() {
		Integer candidate = applyingCandidate("n4@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.VALIDEE, "ok", List.of(new Score(aTrait().getId(), (short) 8))));
		evaluationService.finalize(hrId(), app, new io.github.ielammari.bridge.dto.FinalEvaluationRequest(
				Decision.VALIDEE, "ok",
				new io.github.ielammari.bridge.dto.FinalEvaluationRequest.InterviewData(
						null, null, null, null, null, null, null),
				new io.github.ielammari.bridge.dto.FinalEvaluationRequest.HiringTerms(
						new java.math.BigDecimal("40000"), LocalDate.of(2099, 4, 1),
						ContractType.PERMANENT, null, false, null)));

		assertThat(hasType(messageService.inbox(candidate), NotificationType.HIRED)).isTrue();
	}

	@Test
	void unreadCountAndMarkReadWork() {
		Integer candidate = applyingCandidate("n5@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		assertThat(messageService.unreadCount(candidate)).isGreaterThan(0);
		messageService.markAllRead(candidate);
		assertThat(messageService.unreadCount(candidate)).isZero();
	}

}
