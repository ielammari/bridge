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
import io.github.ielammari.bridge.repository.ApplicationRepository;
import io.github.ielammari.bridge.repository.AppointmentRepository;

/**
 * HR schedules interviews by hand. There is one company wide calendar with an
 * hourly grid; a date and time holds at most one interview.
 */
@Service
public class AppointmentService {

	private static final int FIRST_HOUR = 9;
	private static final int LAST_HOUR = 16;

	private final AppointmentRepository appointments;
	private final ApplicationRepository applications;
	private final NotificationService notifications;

	public AppointmentService(AppointmentRepository appointments, ApplicationRepository applications,
			NotificationService notifications) {
		this.appointments = appointments;
		this.applications = applications;
		this.notifications = notifications;
	}

	@Transactional(readOnly = true)
	public DayScheduleDto day(LocalDate date) {
		Map<LocalTime, Appointment> byTime = appointments.findByDateWithCandidate(date).stream()
				.collect(Collectors.toMap(Appointment::getTime, Function.identity()));

		List<DaySlotDto> slots = new ArrayList<>();
		for (int hour = FIRST_HOUR; hour <= LAST_HOUR; hour++) {
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
		if (date.isBefore(LocalDate.now())) {
			throw ApiException.badRequest("SLOT_PAST", "La date choisie est déjà passée.");
		}
		if (time.getMinute() != 0 || time.getHour() < FIRST_HOUR || time.getHour() > LAST_HOUR) {
			throw ApiException.badRequest("SLOT_INVALID", "Choisissez une heure pleine entre 9h et 16h.");
		}
	}

}
