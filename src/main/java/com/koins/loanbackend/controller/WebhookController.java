package com.koins.loanbackend.controller;

import com.koins.loanbackend.exception.WebhookSignatureException;
import com.koins.loanbackend.service.WebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Webhooks", description = "Payment provider webhook receivers (Paystack)")
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/paystack")
    public ResponseEntity<Void> handlePaystack(
            @RequestBody String rawBody,
            @RequestHeader("x-paystack-signature") String signature) {

        if (!webhookService.verifyPaystackSignature(rawBody, signature)) {
            throw new WebhookSignatureException("Invalid Paystack webhook signature");
        }

        webhookService.processPaystackEvent(rawBody);
        return ResponseEntity.ok().build();
    }


}