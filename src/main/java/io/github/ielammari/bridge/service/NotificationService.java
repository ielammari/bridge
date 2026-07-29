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
import io.github.ielammari.bridge.repository.UserRepository;

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
	private final UserRepository users;

	public NotificationService(MessageRepository messages, UserRepository users) {
		this.messages = messages;
		this.users = users;
	}

	/** A candidate has applied: tell the HR who published the offer. */
	public void applicationReceived(Application application) {
		HRManager hr = application.getOffer().getPublisher();
		messages.save(Message.notification(hr,
				"Nouvelle candidature de " + candidateName(application)
						+ " pour l'offre « " + application.getOffer().getTitle() + " ».",
				NotificationType.APPLICATION_RECEIVED, application));
	}

	/** An evaluation was approved: tell HR the next interview needs scheduling. */
	public void scheduleNeeded(Application application, AppointmentType next) {
		HRManager hr = application.getOffer().getPublisher();
		String what = next == AppointmentType.TECHNIQUE ? "l'examen technique" : "l'entretien RH";
		messages.save(Message.notification(hr,
				"Planifiez " + what + " pour " + candidateName(application)
						+ " (offre « " + application.getOffer().getTitle() + " »).",
				NotificationType.SCHEDULE_NEEDED, application));
	}

	/** An interview was booked: tell the candidate, and the experts for an exam. */
	public void interviewScheduled(Application application, AppointmentType type, LocalDate date, LocalTime time) {
		String when = DAY.format(date) + " à " + HOUR.format(time);
		String what = type == AppointmentType.TECHNIQUE ? "Votre examen technique" : "Votre entretien RH";
		messages.save(Message.notification(application.getCandidate(),
				what + " est fixé au " + when + ".", NotificationType.INTERVIEW_SCHEDULED, application));

		if (type == AppointmentType.TECHNIQUE) {
			for (User expert : users.findAllExperts()) {
				messages.save(Message.notification(expert,
						"Examen technique à faire passer à " + candidateName(application) + " le " + when + ".",
						NotificationType.INTERVIEW_SCHEDULED, application));
			}
		}
	}

	/** The application was closed with a refusal: tell the candidate. */
	public void rejected(Application application) {
		messages.save(Message.notification(application.getCandidate(),
				"Votre candidature pour l'offre « " + application.getOffer().getTitle()
						+ " » n'a pas été retenue.",
				NotificationType.REJECTED, application));
	}

	/** The candidate was hired: tell them. */
	public void hired(Application application) {
		messages.save(Message.notification(application.getCandidate(),
				"Félicitations, vous êtes embauché(e) pour l'offre « "
						+ application.getOffer().getTitle() + " ».",
				NotificationType.HIRED, application));
	}

	private String candidateName(Application application) {
		return application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName();
	}

}
