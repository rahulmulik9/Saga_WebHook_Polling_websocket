package com.rahul.orderservice.service;

import com.rahul.orderservice.dto.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ConcurrentHashMap;

// Holds requests that are waiting for an order's status to change.
// Single-instance only - if you run multiple order-service replicas,
// a request waiting on instance A won't be woken by a listener firing
// on instance B. Fine for teaching/single-instance; would need a
// pub/sub backplane (Kafka/Redis) to fix in a real multi-instance setup.
@Component
public class OrderStatusWaiters {

    private final ConcurrentHashMap<Long, DeferredResult<OrderResponse>> waiters = new ConcurrentHashMap<>();

    public void register(Long orderId, DeferredResult<OrderResponse> deferredResult) {
        waiters.put(orderId, deferredResult);

        // Whatever the reason the DeferredResult finishes (timeout, or we
        // complete it below) - remove it from the map so we don't leak entries.
        deferredResult.onCompletion(() -> waiters.remove(orderId, deferredResult));
    }

    public void notifyStatusChanged(Long orderId, OrderResponse response) {
        DeferredResult<OrderResponse> waiting = waiters.remove(orderId);
        if (waiting != null) {
            waiting.setResult(response);
        }
    }
}