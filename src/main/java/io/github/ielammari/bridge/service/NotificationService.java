package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.Message;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.MessageRepository;
import io.github.ielammari.bridge.repository.NotificationPreferenceRepository;

/**
 * Emits the system notifications the funnel produces. Every call happens inside
 * the caller's transaction, so a message is written only if the action commits.
 */
@Service
public class NotificationService {

	private static final DateTimeFormatter DAY =
			DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
	private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH'h'mm");

	private final MessageRepository messages;
	private final NotificationPreferenceRepository preferences;

	public NotificationService(MessageRepository messages,
			NotificationPreferenceRepository preferences) {
		this.messages = messages;
		this.preferences = preferences;
	}

	/**
	 * Writes a notification unless its recipient has turned that kind off. A
	 * type that cannot be silenced never consults the preferences.
	 */
	private void deliver(User recipient, String content, NotificationType type, Application application) {
		if (type.isSilenceableBy(recipient.getRole())
				&& preferences.existsByUserIdAndType(recipient.getId(), type)) {
			return;
		}
		messages.save(Message.notification(recipient, content, type, application));
	}

	/** A candidate has applied: confirm it to them, and tell the publishing HR. */
	public void applicationReceived(Application application) {
		deliver(application.getCandidate(),
				"Votre candidature pour l'offre « " + application.getOffer().getTitle()
						+ " » a bien été enregistrée.",
				NotificationType.APPLICATION_SUBMITTED, application);

		HRManager hr = application.getOffer().getPublisher();
		deliver(hr, "Nouvelle candidature de " + candidateName(application)
				+ " pour l'offre « " + application.getOffer().getTitle() + " ».",
				NotificationType.APPLICATION_RECEIVED, application);
	}

	/** An evaluation was approved: tell HR the next interview needs scheduling. */
	public void scheduleNeeded(Application application, AppointmentType next) {
		HRManager hr = application.getOffer().getPublisher();
		String what = next == AppointmentType.TECHNIQUE ? "l'examen technique" : "l'entretien RH";
		deliver(hr, "Planifiez " + what + " pour " + candidateName(application)
				+ " (offre « " + application.getOffer().getTitle() + " »).",
				NotificationType.SCHEDULE_NEEDED, application);
	}

	/**
	 * An interview was booked: tell the candidate, and the expert it was handed
	 * to when it is an exam. An HR interview is run by the recruiter who booked
	 * it, who needs no notice of their own act.
	 */
	public void interviewScheduled(Application application, AppointmentType type, LocalDate date,
			LocalTime time, User evaluator) {
		String when = DAY.format(date) + " à " + HOUR.format(time);
		String what = type == AppointmentType.TECHNIQUE ? "Votre examen technique" : "Votre entretien RH";
		deliver(application.getCandidate(), what + " est fixé au " + when + ".",
				NotificationType.INTERVIEW_SCHEDULED, application);

		if (type == AppointmentType.TECHNIQUE) {
			deliver(evaluator, "Examen technique à faire passer à " + candidateName(application)
					+ " le " + when + " (offre « " + application.getOffer().getTitle() + " »).",
					NotificationType.INTERVIEW_SCHEDULED, application);
		}
	}

	/** The exam went to somebody else: tell the expert who was holding it. */
	public void examUnassigned(Application application, User previous) {
		deliver(previous, "L'examen technique de " + candidateName(application)
				+ " ne vous est plus attribué.",
				NotificationType.EXAM_UNASSIGNED, application);
	}

	/** The exam hour went by with nothing recorded: ask the recruiter to hand it on. */
	public void examOverdue(Application application, LocalDate date, LocalTime time) {
		HRManager hr = application.getOffer().getPublisher();
		deliver(hr, "L'examen technique de " + candidateName(application) + " du "
				+ DAY.format(date) + " à " + HOUR.format(time)
				+ " n'a pas été évalué. Replanifiez-le, avec un autre expert si besoin.",
				NotificationType.EXAM_OVERDUE, application);
	}

	/** The application was closed with a refusal: tell the candidate. */
	public void rejected(Application application) {
		deliver(application.getCandidate(),
				"Votre candidature pour l'offre « " + application.getOffer().getTitle()
						+ " » n'a pas été retenue.",
				NotificationType.REJECTED, application);
	}

	/** The candidate was hired: tell them. */
	public void hired(Application application) {
		deliver(application.getCandidate(),
				"Félicitations, vous êtes embauché(e) pour l'offre « "
						+ application.getOffer().getTitle() + " ».",
				NotificationType.HIRED, application);
	}

	private String candidateName(Application application) {
		return application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName();
	}

}
