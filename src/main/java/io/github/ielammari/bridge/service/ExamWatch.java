package io.github.ielammari.bridge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.NotificationType;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.MessageRepository;

/**
 * Watches for exams nobody recorded. An expert holds an exam alone, so an
 * unrecorded one stalls the application unnoticed: three hours past its hour
 * the offer's recruiter is asked to replan it, once per exam.
 */
@Service
public class ExamWatch {

	/** How long after the booked hour an exam counts as unattended. */
	private static final int GRACE_HOURS = 3;

	private final AppointmentRepository appointments;
	private final MessageRepository messages;
	private final NotificationService notifications;

	public ExamWatch(AppointmentRepository appointments, MessageRepository messages,
			NotificationService notifications) {
		this.appointments = appointments;
		this.messages = messages;
		this.notifications = notifications;
	}

	@Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
	@Transactional
	public void reportUnattendedExams() {
		LocalDateTime cutoff = LocalDateTime.now().minusHours(GRACE_HOURS);

		for (Appointment exam : appointments.findOverdue(AppointmentType.TECHNIQUE,
				cutoff.toLocalDate(), cutoff.toLocalTime())) {
			Integer applicationId = exam.getApplication().getId();
			if (messages.existsByApplicationIdAndType(applicationId, NotificationType.EXAM_OVERDUE)) {
				continue;
			}
			notifications.examOverdue(exam.getApplication(), exam.getDate(), exam.getTime());
		}
	}

	/** Whether a booked exam has gone unattended by the given moment. Pure, so it is testable. */
	static boolean isUnattended(LocalDate date, java.time.LocalTime time, LocalDateTime now) {
		return LocalDateTime.of(date, time).plusHours(GRACE_HOURS).isBefore(now);
	}

}
