package io.github.ielammari.bridge.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MessageDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.MessageMapper;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;
import io.github.ielammari.bridge.model.AppointmentType;
import io.github.ielammari.bridge.model.Message;
import io.github.ielammari.bridge.model.Role;
import io.github.ielammari.bridge.repository.AppointmentRepository;
import io.github.ielammari.bridge.repository.MessageRepository;

/** Reading side of the inbox. The writing side is NotificationService. */
@Service
public class MessageService {

	private final MessageRepository messages;
	private final AppointmentRepository appointments;

	public MessageService(MessageRepository messages, AppointmentRepository appointments) {
		this.messages = messages;
		this.appointments = appointments;
	}

	@Transactional
	public List<MessageDto> inbox(Integer userId) {
		settleFinishedTasks(userId);
		return messages.findInbox(userId).stream().map(MessageMapper::toDto).toList();
	}

	@Transactional
	public long unreadCount(Integer userId) {
		settleFinishedTasks(userId);
		return messages.countByRecipientIdAndReadFalse(userId);
	}

	@Transactional
	public void markRead(Integer userId, Integer messageId) {
		Message message = messages.findByIdAndRecipientId(messageId, userId)
				.orElseThrow(() -> ApiException.notFound("Ce message est introuvable."));
		message.markRead();
	}

	@Transactional
	public void markAllRead(Integer userId) {
		messages.markAllRead(userId);
	}

	/** Opening an application answers every notice this user had about it. */
	@Transactional
	public void markReadForApplication(Integer userId, Integer applicationId) {
		messages.markReadForApplication(userId, applicationId);
	}

	/** Marks read every unread notification whose task has since been carried out. */
	private void settleFinishedTasks(Integer userId) {
		for (Message message : messages.findUnreadAboutAnApplication(userId)) {
			if (isTaskDone(message)) {
				message.markRead();
			}
		}
	}

	/**
	 * Whether the work a notification asked for has happened. Only notifications
	 * that ask for something settle: news carries no task to discharge.
	 */
	private boolean isTaskDone(Message message) {
		Application application = message.getApplication();
		ApplicationStatus status = application.getStatus();

		return switch (message.getType()) {
			// HR was asked to screen the application.
			case APPLICATION_RECEIVED ->
				status != ApplicationStatus.NOUVELLE && status != ApplicationStatus.EN_REVUE;

			// HR was asked to book the next interview.
			case SCHEDULE_NEEDED -> nothingLeftToSchedule(application, status);

			// The expert holds the exam. The same type reaches the candidate as
			// information, which never settles.
			case INTERVIEW_SCHEDULED ->
				message.getRecipient().getRole() == Role.EXPERT
						&& status != ApplicationStatus.EXAMEN_TECHNIQUE;

			// HR was asked to replan an exam nobody sat.
			case EXAM_OVERDUE -> status != ApplicationStatus.EXAMEN_TECHNIQUE
					|| examReplanned(application);

			// A receipt, an outcome, and an exam leaving a queue are news, and
			// ask the reader for nothing.
			case APPLICATION_SUBMITTED, EXAM_UNASSIGNED, REJECTED, HIRED -> false;
		};
	}

	/** Whether the stalled exam has been given an hour that has not gone by. */
	private boolean examReplanned(Application application) {
		return appointments.findByApplicationIdAndType(application.getId(), AppointmentType.TECHNIQUE)
				.map(exam -> !ExamWatch.isUnattended(exam.getDate(), exam.getTime(), LocalDateTime.now()))
				.orElse(false);
	}

	private boolean nothingLeftToSchedule(Application application, ApplicationStatus status) {
		AppointmentType awaited = Stages.appointmentTypeFor(status);
		return awaited == null
				|| appointments.findByApplicationIdAndType(application.getId(), awaited).isPresent();
	}

}
