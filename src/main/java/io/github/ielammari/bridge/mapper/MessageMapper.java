package io.github.ielammari.bridge.mapper;

import io.github.ielammari.bridge.dto.MessageDto;
import io.github.ielammari.bridge.model.Message;

public final class MessageMapper {

	private MessageMapper() {
	}

	public static MessageDto toDto(Message message) {
		return new MessageDto(
				message.getId(),
				message.getContent(),
				message.getSentAt(),
				message.isRead(),
				message.getType(),
				message.getApplication() == null ? null : message.getApplication().getId());
	}

}
