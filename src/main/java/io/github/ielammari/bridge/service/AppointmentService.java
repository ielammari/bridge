package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.DayScheduleDto;
import io.github.ielammari.bridge.dto.DaySlotDto;
import io.github.ielammari.bridge.dto.HrApplicationDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.ApplicationMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.OrganisationSettings;
import io.github.ielammari.bridge.dto.ExpertSummaryDto;
import io.github.ielammari.bridge.model.Evaluator;
import io.github.ielammari.bridge.model.TechnicalExpert;
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.OrganisationSettingsRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * HR schedules interviews by hand on an hourly grid. Every interview names the
 * evaluator who runs it, and the calendar belongs to that evaluator: two may
 * hold the same hour, neither may hold two, and a candidate is never expected
 * in two places at once.
 */
@Service
public class AppointmentService {

	private final AppointmentRepository appointments;
	private final ApplicationRepository applications;
	private final NotificationService notifications;
	private final OrganisationSettingsRepository organisation;
	private final UserRepository users;

	public AppointmentService(AppointmentRepository appointments, ApplicationRepository applications,
			NotificationService notifications, OrganisationSettingsRepository organisation,
			UserRepository users) {
		this.appointments = appointments;
		this.applications = applications;
		this.notifications = notifications;
		this.organisation = organisation;
		this.users = users;
	}

	/** The bookable hours, as configured by HR. */
	private OrganisationSettings grid() {
		return organisation.findById(OrganisationSettings.SINGLETON_ID)
				.orElseThrow(() -> ApiException.internal("SETTINGS_MISSING",
						"Les paramètres de l'organisation sont introuvables."));
	}

	/** One evaluator's day: what they already hold, and what is left. */
	@Transactional(readOnly = true)
	public DayScheduleDto day(LocalDate date, Integer evaluatorId) {
		Map<LocalTime, Appointment> byTime = appointments
				.findByDateAndEvaluatorWithCandidate(date, evaluatorId).stream()
				.collect(Collectors.toMap(Appointment::getTime, Function.identity()));

		OrganisationSettings grid = grid();
		List<DaySlotDto> slots = new ArrayList<>();
		for (int hour = grid.getFirstHour(); hour <= grid.getLastHour(); hour++) {
			LocalTime time = LocalTime.of(hour, 0);
			Appointment appointment = byTime.get(time);
			if (appointment == null) {
				slots.add(new DaySlotDto(time.toString(), false, null, null, null, null));
			} else {
				Application booked = appointment.getApplication();
				String candidate = booked.getCandidate().getFirstName() + " "
						+ booked.getCandidate().getLastName();
				slots.add(new DaySlotDto(time.toString(), true, booked.getId(), candidate,
						booked.getOffer().getTitle(), appointment.getType()));
			}
		}
		return new DayScheduleDto(date, slots);
	}

	/** Books or moves the interview an application is waiting on to the chosen slot. */
	@Transactional
	public HrApplicationDto schedule(Integer hrId, Integer applicationId, LocalDate date, LocalTime time,
			Integer expertId) {
		Application application = Ownership.requireOwnApplication(
				applications.findByIdWithOfferAndCandidate(applicationId)
						.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable.")),
				hrId);

		AppointmentType type = Stages.appointmentTypeFor(application.getStatus());
		if (type == null) {
			throw ApiException.badRequest("NOTHING_TO_SCHEDULE",
					"Cette candidature n'attend pas de rendez-vous à planifier.");
		}
		validateSlot(date, time);

		// An exam is handed to the expert HR named; the final interview is run
		// by the recruiter who owns the offer, so it needs nobody named.
		Evaluator evaluator = type == AppointmentType.TECHNIQUE
				? requireExpert(expertId)
				: requireEvaluator(hrId);

		Optional<Appointment> existing = appointments.findByApplicationIdAndType(applicationId, type);
		Integer candidateId = application.getCandidate().getId();
		boolean taken = existing
				.map(a -> appointments.existsByEvaluatorIdAndDateAndTimeAndIdNot(
						evaluator.getId(), date, time, a.getId()))
				.orElseGet(() -> appointments.existsByEvaluatorIdAndDateAndTime(evaluator.getId(), date, time));
		if (taken) {
			throw ApiException.badRequest("SLOT_TAKEN",
					"Cet évaluateur a déjà un entretien sur ce créneau.");
		}
		boolean candidateBusy = existing
				.map(a -> appointments.existsByApplicationCandidateIdAndDateAndTimeAndIdNot(
						candidateId, date, time, a.getId()))
				.orElseGet(() -> appointments.existsByApplicationCandidateIdAndDateAndTime(
						candidateId, date, time));
		if (candidateBusy) {
			throw ApiException.badRequest("CANDIDATE_BUSY",
					"Ce candidat est déjà attendu à un autre entretien sur ce créneau.");
		}

		Evaluator previous = existing.map(Appointment::getEvaluator).orElse(null);
		Appointment appointment = existing
				.map(a -> {
					a.reschedule(date, time);
					a.assignTo(evaluator);
					return a;
				})
				.orElseGet(() -> appointments.save(
						new Appointment(application, type, date, time, evaluator)));

		if (previous != null && !previous.getId().equals(evaluator.getId())) {
			notifications.examUnassigned(application, previous);
		}
		notifications.interviewScheduled(application, type, date, time, evaluator);
		return ApplicationMapper.toHrView(application, appointment);
	}

	/** The experts an exam can be handed to, with what they already hold that week. */
	@Transactional(readOnly = true)
	public List<ExpertSummaryDto> experts(LocalDate week) {
		LocalDate from = week.with(java.time.DayOfWeek.MONDAY);
		Map<Integer, Long> load = appointments.countByEvaluatorBetween(from, from.plusDays(6)).stream()
				.collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

		return users.findAllExperts().stream()
				.map(expert -> new ExpertSummaryDto(expert.getId(), expert.getFirstName(),
						expert.getLastName(), expert.getEmail(),
						load.getOrDefault(expert.getId(), 0L).intValue()))
				.toList();
	}

	private Evaluator requireExpert(Integer expertId) {
		if (expertId == null) {
			throw ApiException.badRequest("EXPERT_REQUIRED",
					"Choisissez l'expert technique qui fera passer l'examen.");
		}
		return users.findById(expertId)
				.filter(TechnicalExpert.class::isInstance)
				.map(Evaluator.class::cast)
				.orElseThrow(() -> ApiException.badRequest("UNKNOWN_EXPERT",
						"Cet expert technique est introuvable."));
	}

	private Evaluator requireEvaluator(Integer id) {
		return users.findById(id)
				.filter(Evaluator.class::isInstance)
				.map(Evaluator.class::cast)
				.orElseThrow(() -> ApiException.notFound("Cet évaluateur est introuvable."));
	}

	private void validateSlot(LocalDate date, LocalTime time) {
		OrganisationSettings grid = grid();
		if (time.getMinute() != 0 || time.getHour() < grid.getFirstHour() || time.getHour() > grid.getLastHour()) {
			throw ApiException.badRequest("SLOT_INVALID",
					"Choisissez une heure pleine entre " + grid.getFirstHour() + "h et "
							+ grid.getLastHour() + "h.");
		}
		if (isPast(date, time, LocalDate.now(), LocalTime.now())) {
			throw ApiException.badRequest("SLOT_PAST", "Ce créneau est déjà passé.");
		}
	}

	/**
	 * Whether a slot has already gone by. The date alone does not settle it: 9h
	 * today is past by the afternoon. Pure, so it is testable at any hour.
	 */
	static boolean isPast(LocalDate date, LocalTime time, LocalDate today, LocalTime now) {
		if (date.isBefore(today)) {
			return true;
		}
		return date.isEqual(today) && !time.isAfter(now);
	}

}
