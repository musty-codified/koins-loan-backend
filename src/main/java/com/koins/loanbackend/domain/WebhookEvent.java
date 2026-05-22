package com.koins.loanbackend.domain;

import com.koins.loanbackend.domain.enums.WebhookEventStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_reference", columnList = "event_reference", unique = true),
    @Index(name = "idx_webhook_provider",  columnList = "provider"),
    @Index(name = "idx_webhook_status",    columnList = "status")
})
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "event_reference", nullable = false, unique = true, length = 100)
    private String eventReference;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WebhookEventStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public String getEventReference() { return eventReference; }
    public void setEventReference(String eventReference) { this.eventReference = eventReference; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public WebhookEventStatus getStatus() { return status; }
    public void setStatus(WebhookEventStatus status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}