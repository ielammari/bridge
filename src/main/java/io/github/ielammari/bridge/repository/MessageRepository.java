package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.model.Message;

public interface MessageRepository extends JpaRepository<Message, Integer> {

	@Query("SELECT m FROM Message m LEFT JOIN FETCH m.application "
			+ "WHERE m.recipient.id = :recipientId ORDER BY m.sentAt DESC")
	List<Message> findInbox(Integer recipientId);

	long countByRecipientIdAndReadFalse(Integer recipientId);

	/**
	 * Unread notifications concerning an application. The inner join drops those
	 * without one, which are exactly the ones carrying no task.
	 */
	@Query("SELECT m FROM Message m JOIN FETCH m.application "
			+ "WHERE m.recipient.id = :recipientId AND m.read = false")
	List<Message> findUnreadAboutAnApplication(Integer recipientId);

	Optional<Message> findByIdAndRecipientId(Integer id, Integer recipientId);

	// Bulk updates bypass the persistence context: flush pending changes first,
	// then clear the stale copies they leave behind.
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Message m SET m.read = true WHERE m.recipient.id = :recipientId AND m.read = false")
	void markAllRead(Integer recipientId);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("UPDATE Message m SET m.read = true WHERE m.recipient.id = :recipientId "
			+ "AND m.application.id = :applicationId AND m.read = false")
	int markReadForApplication(Integer recipientId, Integer applicationId);

}
