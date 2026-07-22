package com.augustine.gplfantasyleaague.domain.subscription.controller;

import com.augustine.gplfantasyleaague.domain.subscription.dto.InitializePaymentResponse;
import com.augustine.gplfantasyleaague.domain.subscription.dto.SubscriptionStatusResponse;
import com.augustine.gplfantasyleaague.domain.subscription.service.PaymentService;
import com.augustine.gplfantasyleaague.domain.subscription.service.PaystackService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaystackService paystackService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService, PaystackService paystackService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.paystackService = paystackService;
        this.objectMapper = objectMapper;
    }

    // Starts a premium purchase for the logged-in user. Returns Paystack's
    // hosted checkout URL - the frontend opens it in a WebView, it never
    // handles card details itself.
    @PostMapping("/initialize")
    public ResponseEntity<InitializePaymentResponse> initialize() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(paymentService.initializePremiumPurchase(email));
    }

    // Called by the frontend right after its WebView sees the Paystack
    // callback redirect fire, to confirm (server-to-server) that the
    // payment actually succeeded before activating anything.
    @PostMapping("/verify/{reference}")
    public ResponseEntity<SubscriptionStatusResponse> verify(@PathVariable String reference) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(paymentService.verifyAndActivate(reference, email));
    }

    // Paystack calls this directly (see SecurityConfig - must stay
    // permitAll, Paystack has no way to send our JWT). Trust comes from the
    // HMAC signature check below, not authentication.
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature
    ) {
        if (!paystackService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Rejected Paystack webhook with invalid/missing signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            if ("charge.success".equals(event)) {
                String reference = root.path("data").path("reference").asText();
                paymentService.handleSuccessfulWebhook(reference);
            }
        } catch (Exception e) {
            // Log and still return 200 - a malformed/unhandled event isn't
            // something retrying will fix, and Paystack retries on non-2xx.
            log.warn("Failed to process Paystack webhook body", e);
        }

        return ResponseEntity.ok().build();
    }
}
