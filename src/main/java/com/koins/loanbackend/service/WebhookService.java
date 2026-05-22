package com.koins.loanbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.WebhookEvent;
import com.koins.loanbackend.domain.enums.TransactionType;
import com.koins.loanbackend.domain.enums.WebhookEventStatus;
import com.koins.loanbackend.dto.webhook.PaystackWebhookPayload;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.UserRepository;
import com.koins.loanbackend.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final BigDecimal KOBO_DIVISOR = new BigDecimal("100");

    private final WebhookEventRepository webhookEventRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @Value("${app.paystack.secret-key}")
    private String paystackSecretKey;

    public WebhookService(WebhookEventRepository webhookEventRepository,
                          UserRepository userRepository,
                          WalletService walletService,
                          TransactionService transactionService,
                          ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
    }

    public boolean verifyPaystackSignature(String rawBody, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equals(signature);
        } catch (Exception e) {
            log.error("Paystack signature verification failed", e);
            return false;
        }
    }

    @Transactional
    public void processPaystackEvent(String rawBody) {
        PaystackWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);
        } catch (Exception e) {
            log.error("Failed to parse Paystack payload", e);
            return;
        }

        if (!"charge.success".equals(payload.getEvent())) {
            return;
        }

        PaystackWebhookPayload.Data data = payload.getData();
        String reference = data.getReference();

        if (webhookEventRepository.existsByEventReference(reference)) {
            log.info("Paystack event already processed: {}", reference);
            return;
        }

        try {
            String email = data.getCustomer().getEmail();
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found for email: " + email));

            BigDecimal amountNaira = BigDecimal.valueOf(data.getAmount()).divide(KOBO_DIVISOR);
            Wallet wallet = walletService.getWalletByUserId(user.getId());

            transactionService.credit(
                wallet.getId(), user, amountNaira,
                TransactionType.Credit,
                "Paystack payment — ref " + reference,
                "paystack:" + reference
            );

            saveEvent(reference, payload.getEvent(), WebhookEventStatus.PROCESSED, null);
            log.info("Processed Paystack charge.success: ref={} amount={}", reference, amountNaira);

        } catch (Exception e) {
            log.error("Error processing Paystack event ref={}: {}", reference, e.getMessage(), e);
        }
    }

    private void saveEvent(String reference, String eventType,
                           WebhookEventStatus status, String failureReason) {
        WebhookEvent event = new WebhookEvent();
        event.setEventReference(reference);
        event.setEventType(eventType);
        event.setStatus(status);
        event.setFailureReason(failureReason);
        webhookEventRepository.save(event);
    }
}