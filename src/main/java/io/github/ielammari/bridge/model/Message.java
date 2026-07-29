package io.github.ielammari.bridge.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A message delivered to a user. System notifications have no sender; the
 * nullable sender leaves room for user authored messages later.
 */
@Entity
@Table(name = "message")
public class Message {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_message")
	private Integer id;

	@Column(name = "contenu", nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "date_envoi", nullable = false)
	private Instant sentAt;

	@Column(name = "lu", nullable = false)
	private boolean read;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_expediteur")
	private User sender;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_destinataire", nullable = false)
	private User recipient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_candidature")
	private Application application;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_notification", length = 30)
	private NotificationType type;

	protected Message() {
	}

	/** A system notification: a recipient, no human sender. */
	public static Message notification(User recipient, String content, NotificationType type,
			Application application) {
		Message message = new Message();
		message.recipient = recipient;
		message.content = content;
		message.type = type;
		message.application = application;
		message.sentAt = Instant.now();
		message.read = false;
		return message;
	}

	public void markRead() {
		this.read = true;
	}

	public Integer getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public boolean isRead() {
		return read;
	}

	public User getRecipient() {
		return recipient;
	}

	public Application getApplication() {
		return application;
	}

	public NotificationType getType() {
		return type;
	}

}
