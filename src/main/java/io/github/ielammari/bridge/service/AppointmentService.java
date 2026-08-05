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
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.OrganisationSettingsRepository;

/**
 * HR schedules interviews by hand. There is one company wide calendar with an
 * hourly grid; a date and time holds at most one interview.
 */
@Service
public class AppointmentService {

	private final AppointmentRepository appointments;
	private final ApplicationRepository applications;
	private final NotificationService notifications;
	private final OrganisationSettingsRepository organisation;

	public AppointmentService(AppointmentRepository appointments, ApplicationRepository applications,
			NotificationService notifications, OrganisationSettingsRepository organisation) {
		this.appointments = appointments;
		this.applications = applications;
		this.notifications = notifications;
		this.organisation = organisation;
	}

	/** The bookable hours, as configured by HR. */
	private OrganisationSettings grid() {
		return organisation.findById(OrganisationSettings.SINGLETON_ID)
				.orElseThrow(() -> ApiException.internal("SETTINGS_MISSING",
						"Les paramètres de l'organisation sont introuvables."));
	}

	@Transactional(readOnly = true)
	public DayScheduleDto day(LocalDate date) {
		Map<LocalTime, Appointment> byTime = appointments.findByDateWithCandidate(date).stream()
				.collect(Collectors.toMap(Appointment::getTime, Function.identity()));

		OrganisationSettings grid = grid();
		List<DaySlotDto> slots = new ArrayList<>();
		for (int hour = grid.getFirstHour(); hour <= grid.getLastHour(); hour++) {
			LocalTime time = LocalTime.of(hour, 0);
			Appointment appointment = byTime.get(time);
			if (appointment == null) {
				slots.add(new DaySlotDto(time.toString(), false, null, null, null));
			} else {
				String candidate = appointment.getApplication().getCandidate().getFirstName() + " "
						+ appointment.getApplication().getCandidate().getLastName();
				slots.add(new DaySlotDto(time.toString(), true,
						appointment.getApplication().getId(), candidate, appointment.getType()));
			}
		}
		return new DayScheduleDto(date, slots);
	}

	/** Books or moves the interview an application is waiting on to the chosen slot. */
	@Transactional
	public HrApplicationDto schedule(Integer applicationId, LocalDate date, LocalTime time) {
		Application application = applications.findByIdWithOfferAndCandidate(applicationId)
				.orElseThrow(() -> ApiException.notFound("Cette candidature est introuvable."));

		AppointmentType type = Stages.appointmentTypeFor(application.getStatus());
		if (type == null) {
			throw ApiException.badRequest("NOTHING_TO_SCHEDULE",
					"Cette candidature n'attend pas de rendez-vous à planifier.");
		}
		validateSlot(date, time);

		Optional<Appointment> existing = appointments.findByApplicationIdAndType(applicationId, type);
		boolean taken = existing
				.map(a -> appointments.existsByDateAndTimeAndIdNot(date, time, a.getId()))
				.orElseGet(() -> appointments.existsByDateAndTime(date, time));
		if (taken) {
			throw ApiException.badRequest("SLOT_TAKEN", "Ce créneau est déjà occupé par un autre entretien.");
		}

		Appointment appointment = existing
				.map(a -> {
					a.reschedule(date, time);
					return a;
				})
				.orElseGet(() -> appointments.save(new Appointment(application, type, date, time)));

		notifications.interviewScheduled(application, type, date, time);
		return ApplicationMapper.toHrView(application, appointment);
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
