package com.rahul.orderservice.service;

import com.rahul.orderservice.dto.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

// Holds open SSE streams that are waiting for an order's status to change.
// Same single-instance limitation as OrderStatusWaiters (Phase 6) - a
// stream open on instance A won't be pushed to by a listener firing on
// instance B. Fine for teaching/single-instance; would need a pub/sub
// backplane (Kafka/Redis) to fix in a real multi-instance setup.
@Component
public class OrderStatusEmitters {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(Long orderId, SseEmitter emitter) {
        emitters.put(orderId, emitter);

        // Clean up the map whichever way the stream ends - completed,
        // timed out, or client disconnected - so we don't leak entries.
        emitter.onCompletion(() -> emitters.remove(orderId, emitter));
        emitter.onTimeout(() -> emitters.remove(orderId, emitter));
        emitter.onError(e -> emitters.remove(orderId, emitter));
    }

    public void pushStatusChanged(Long orderId, OrderResponse response) {
        SseEmitter emitter = emitters.remove(orderId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(response);
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}