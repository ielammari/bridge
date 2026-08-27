package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.CalendarDto;
import io.github.ielammari.bridge.dto.CalendarEntryDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.CalendarScope;
import io.github.ielammari.bridge.model.Evaluator;
import io.github.ielammari.bridge.model.HRManager;
import io.github.ielammari.bridge.model.OrganisationSettings;
import io.github.ielammari.bridge.model.User;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.OrganisationSettingsRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * A month of interviews, read by whoever they belong to. An evaluator opens the
 * calendar they run; a recruiter also opens the exams they arranged on their own
 * offers and, while booking one, the calendar of the expert they are handing it
 * to. Anything else answers as though it did not exist.
 */
@Service
public class CalendarService {

	/** The widest range one request may ask for, a month view needing 42 days. */
	private static final int RANGE_LIMIT = 92;

	private final AppointmentRepository appointments;
	private final OrganisationSettingsRepository organisation;
	private final UserRepository users;

	public CalendarService(AppointmentRepository appointments,
			OrganisationSettingsRepository organisation, UserRepository users) {
		this.appointments = appointments;
		this.organisation = organisation;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public CalendarDto read(Integer callerId, CalendarScope scope, Integer evaluatorId,
			LocalDate from, LocalDate to) {
		if (to.isBefore(from) || from.plusDays(RANGE_LIMIT).isBefore(to)) {
			throw ApiException.badRequest("RANGE_INVALID",
					"La période demandée est invalide.");
		}
		User caller = users.findById(callerId)
				.orElseThrow(() -> ApiException.notFound("Ce compte est introuvable."));

		List<Appointment> found = switch (scope) {
			case MINE -> appointments.findForEvaluatorBetween(callerId, from, to);
			case PLANNED -> appointments.findByTypeAndPublisherBetween(
					AppointmentType.TECHNIQUE, requireRecruiter(caller).getId(), from, to);
			case EVALUATOR -> appointments.findForEvaluatorBetween(
					readable(caller, evaluatorId), from, to);
		};

		return new CalendarDto(from, to, capacity(), found.stream().map(CalendarService::entry).toList());
	}

	/** How many hours a day holds, as the organisation has configured them. */
	private int capacity() {
		OrganisationSettings grid = organisation.findById(OrganisationSettings.SINGLETON_ID)
				.orElseThrow(() -> ApiException.internal("SETTINGS_MISSING",
						"Les paramètres de l'organisation sont introuvables."));
		return grid.getLastHour() - grid.getFirstHour() + 1;
	}

	/**
	 * Whose calendar the caller is allowed to open. Their own always; anyone
	 * else's only for a recruiter, who books the hours that belong to experts.
	 */
	private Integer readable(User caller, Integer evaluatorId) {
		if (evaluatorId == null || evaluatorId.equals(caller.getId())) {
			return caller.getId();
		}
		requireRecruiter(caller);
		return users.findById(evaluatorId)
				.filter(Evaluator.class::isInstance)
				.map(User::getId)
				.orElseThrow(() -> ApiException.notFound("Cet évaluateur est introuvable."));
	}

	private HRManager requireRecruiter(User caller) {
		if (caller instanceof HRManager recruiter) {
			return recruiter;
		}
		throw ApiException.notFound("Ce calendrier est introuvable.");
	}

	private static CalendarEntryDto entry(Appointment appointment) {
		Application application = appointment.getApplication();
		Evaluator evaluator = appointment.getEvaluator();
		return new CalendarEntryDto(
				application.getId(),
				application.getCandidate().getId(),
				application.getCandidate().getFirstName(),
				application.getCandidate().getLastName(),
				application.getOffer().getId(),
				application.getOffer().getTitle(),
				appointment.getDate(),
				appointment.getTime(),
				appointment.getType(),
				application.getStatus(),
				evaluator.getId(),
				evaluator.getFirstName() + " " + evaluator.getLastName(),
				isRecorded(appointment.getType(), application.getStatus()));
	}

	/** Whether the interview this reports has already been assessed. */
	private static boolean isRecorded(AppointmentType type, ApplicationStatus status) {
		return switch (type) {
			case TECHNIQUE -> status != ApplicationStatus.EXAMEN_TECHNIQUE;
			case RH -> status == ApplicationStatus.REFUSEE || status == ApplicationStatus.EMBAUCHEE;
		};
	}

}
