package com.rahul.orderservice.service;

import com.rahul.orderservice.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCallbackService {

    private final RestTemplate restTemplate;

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public void notifyCallback(String callbackUrl, OrderResponse response) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restTemplate.postForEntity(callbackUrl, response, Void.class);
                log.info("Callback delivered to {} for orderId={} (attempt {})",
                        callbackUrl, response.getId(), attempt);
                return; // success - stop retrying
            } catch (Exception e) {
                log.warn("Callback attempt {}/{} failed for orderId={} url={} - {}",
                        attempt, MAX_ATTEMPTS, response.getId(), callbackUrl, e.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        log.error("Callback permanently failed for orderId={} url={} after {} attempts",
                response.getId(), callbackUrl, MAX_ATTEMPTS);
    }
}