package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
import io.github.ielammari.bridge.repository.NotificationPreferenceRepository;
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
	@Autowired private NotificationPreferenceRepository preferences;

	/**
	 * These accounts are shared with the development database, where a
	 * notification may have been silenced by hand. Delivery is what is under
	 * test, and the surrounding transaction rolls the clearing back.
	 */
	@BeforeEach
	void deliverEverything() {
		preferences.deleteByUserId(hrId());
		preferences.deleteByUserId(expertId());
		preferences.flush();
	}

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
		Integer id = authService.register(new RegisterRequest(email, "Motdepasse1!x", "Notif", "Test", null, LocalDate.of(1995, 5, 20), null, null, null)).user().id();
		profileService.update(id, new UpdateProfileRequest(Degree.BAC_5, null,
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

	/** Scoped to one application: the shared inboxes also hold older notifications. */
	private long unreadOfType(Integer userId, Integer applicationId, NotificationType type) {
		return messageService.inbox(userId).stream()
				.filter(m -> m.type() == type && !m.read() && applicationId.equals(m.applicationId()))
				.count();
	}

	/** Screening the application discharges the prompt that announced it. */
	@Test
	void anAnsweredApplicationNoticeStopsCountingAsUnread() {
		Integer candidate = applyingCandidate("n6@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();

		assertThat(unreadOfType(hrId(), app, NotificationType.APPLICATION_RECEIVED)).isGreaterThan(0);

		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		assertThat(unreadOfType(hrId(), app, NotificationType.APPLICATION_RECEIVED)).isZero();
	}

	/** Booking the interview discharges the prompt asking for it. */
	@Test
	void aScheduledInterviewSettlesTheSchedulingPrompt() {
		Integer candidate = applyingCandidate("n7@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);

		assertThat(unreadOfType(hrId(), app, NotificationType.SCHEDULE_NEEDED)).isGreaterThan(0);

		appointmentService.schedule(app, LocalDate.of(2099, 5, 4), LocalTime.of(11, 0));

		assertThat(unreadOfType(hrId(), app, NotificationType.SCHEDULE_NEEDED)).isZero();
	}

	/** Recording the exam discharges the expert's prompt to run it. */
	@Test
	void aCompletedExamSettlesTheExpertsPrompt() {
		Integer candidate = applyingCandidate("n8@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		appointmentService.schedule(app, LocalDate.of(2099, 5, 5), LocalTime.of(11, 0));

		assertThat(unreadOfType(expertId(), app, NotificationType.INTERVIEW_SCHEDULED)).isGreaterThan(0);

		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.REFUSEE, "ko", List.of(new Score(aTrait().getId(), (short) 2))));

		assertThat(unreadOfType(expertId(), app, NotificationType.INTERVIEW_SCHEDULED)).isZero();
	}

	/** News is not a task: a refusal stays unread until the candidate reads it. */
	@Test
	void aRefusalNoticeIsNeverSettledAutomatically() {
		Integer candidate = applyingCandidate("n9@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		assertThat(unreadOfType(candidate, app, NotificationType.REJECTED)).isEqualTo(1);
		// Reading the inbox again must not quietly clear it.
		assertThat(unreadOfType(candidate, app, NotificationType.REJECTED)).isEqualTo(1);
	}

	/** An interview date is information for the candidate, so it survives too. */
	@Test
	void theCandidatesInterviewNoticeIsNotSettledByTheExamBeingDone() {
		Integer candidate = applyingCandidate("n10@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.VALIDEE, null);
		appointmentService.schedule(app, LocalDate.of(2099, 5, 6), LocalTime.of(11, 0));
		evaluationService.evaluateTechnical(expertId(), app, new TechnicalEvaluationRequest(
				Decision.REFUSEE, "ko", List.of(new Score(aTrait().getId(), (short) 2))));

		assertThat(unreadOfType(candidate, app, NotificationType.INTERVIEW_SCHEDULED)).isEqualTo(1);
	}

	/**
	 * The badge reads this number, so settling has to show up here and not only
	 * in the inbox listing.
	 */
	@Test
	void theUnreadCountItselfDropsWhenTheTaskIsDone() {
		Integer candidate = applyingCandidate("n11@example.fr");
		long before = messageService.unreadCount(hrId());

		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		assertThat(messageService.unreadCount(hrId())).isEqualTo(before + 1);

		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		assertThat(messageService.unreadCount(hrId())).isEqualTo(before);
	}

	/** Opening an application answers every notice the reader had about it. */
	@Test
	void openingAnApplicationClearsItsNotices() {
		Integer candidate = applyingCandidate("n12@example.fr");
		long before = messageService.unreadCount(hrId());
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		assertThat(messageService.unreadCount(hrId())).isEqualTo(before + 1);

		messageService.markReadForApplication(hrId(), app);

		assertThat(messageService.unreadCount(hrId())).isEqualTo(before);
	}

	/** One reader opening an application says nothing about anyone else's inbox. */
	@Test
	void openingAnApplicationLeavesOtherInboxesAlone() {
		Integer candidate = applyingCandidate("n13@example.fr");
		Integer app = applicationService.apply(candidate, publishedOffer()).id();
		evaluationService.preselect(hrId(), app, Decision.REFUSEE, null);

		messageService.markReadForApplication(hrId(), app);

		assertThat(unreadOfType(candidate, app, NotificationType.REJECTED)).isEqualTo(1);
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
