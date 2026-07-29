package io.github.ielammari.bridge.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MessageDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.mapper.MessageMapper;
import io.github.ielammari.bridge.model.Message;
import io.github.ielammari.bridge.repository.MessageRepository;

/** Reading side of the inbox. The writing side is NotificationService. */
@Service
public class MessageService {

	private final MessageRepository messages;

	public MessageService(MessageRepository messages) {
		this.messages = messages;
	}

	@Transactional(readOnly = true)
	public List<MessageDto> inbox(Integer userId) {
		return messages.findInbox(userId).stream().map(MessageMapper::toDto).toList();
	}

	@Transactional(readOnly = true)
	public long unreadCount(Integer userId) {
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

}
