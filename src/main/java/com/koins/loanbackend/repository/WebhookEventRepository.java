package com.koins.loanbackend.repository;

import com.koins.loanbackend.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    boolean existsByEventReference(String eventReference);
    Optional<WebhookEvent> findByEventReference(String eventReference);
}