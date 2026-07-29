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

	Optional<Message> findByIdAndRecipientId(Integer id, Integer recipientId);

	@Transactional
	@Modifying
	@Query("UPDATE Message m SET m.read = true WHERE m.recipient.id = :recipientId AND m.read = false")
	void markAllRead(Integer recipientId);

}
