package io.github.ielammari.bridge.dto;

import java.util.List;

import io.github.ielammari.bridge.model.NotificationType;

/**
 * Which notifications reach this user, scoped to their role: `silenceable` are
 * the ones they may turn off, `always` the ones they receive regardless, and
 * `silenced` the subset of `silenceable` currently turned off. A type this role
 * never receives appears in none of the three.
 */
public record NotificationPreferencesDto(
		List<NotificationType> silenceable,
		List<NotificationType> always,
		List<NotificationType> silenced) {
}
