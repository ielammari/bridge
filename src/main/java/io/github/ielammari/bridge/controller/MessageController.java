package io.github.ielammari.bridge.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ielammari.bridge.dto.MessageDto;
import io.github.ielammari.bridge.dto.UnreadCountDto;
import io.github.ielammari.bridge.service.MessageService;

/** The inbox, available to every authenticated actor for their own messages. */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@GetMapping
	public List<MessageDto> inbox(@AuthenticationPrincipal Jwt jwt) {
		return messageService.inbox(currentUserId(jwt));
	}

	@GetMapping("/unread-count")
	public UnreadCountDto unreadCount(@AuthenticationPrincipal Jwt jwt) {
		return new UnreadCountDto(messageService.unreadCount(currentUserId(jwt)));
	}

	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
		messageService.markRead(currentUserId(jwt), id);
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
		messageService.markAllRead(currentUserId(jwt));
	}

	/** Signalled when the recipient opens an application the notices point at. */
	@PostMapping("/application/{applicationId}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markReadForApplication(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer applicationId) {
		messageService.markReadForApplication(currentUserId(jwt), applicationId);
	}

	private Integer currentUserId(Jwt jwt) {
		return Integer.valueOf(jwt.getSubject());
	}

}
