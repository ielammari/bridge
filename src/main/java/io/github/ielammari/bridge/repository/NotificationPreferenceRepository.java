package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.NotificationPreference;
import io.github.ielammari.bridge.model.NotificationPreferenceId;
import io.github.ielammari.bridge.model.NotificationType;

public interface NotificationPreferenceRepository
		extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

	List<NotificationPreference> findByUserId(Integer userId);

	boolean existsByUserIdAndType(Integer userId, NotificationType type);

	void deleteByUserId(Integer userId);

}
