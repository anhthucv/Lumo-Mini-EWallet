package com.chethu.paymentledgerservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.service.TopUpFinalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/topups/webhook")
@Tag(name = "Top-up Webhook", description = "Public verified payOS webhook endpoint")
public class TopUpWebhookController {
    private final PaymentProvider paymentProvider;
    private final TopUpFinalizationService topUpFinalizationService;

    public TopUpWebhookController(PaymentProvider paymentProvider,
            TopUpFinalizationService topUpFinalizationService) {
        this.paymentProvider = paymentProvider;
        this.topUpFinalizationService = topUpFinalizationService;
    }

    @PostMapping
    @Operation(summary = "Receive a payOS top-up webhook")
    public ResponseEntity<Map<String, Boolean>> receive(@RequestBody String rawPayload) {
        topUpFinalizationService.finalizeVerifiedWebhook(paymentProvider.verifyWebhook(rawPayload));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
