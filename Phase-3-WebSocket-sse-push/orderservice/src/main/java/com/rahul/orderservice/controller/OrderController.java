package com.rahul.orderservice.controller;

import com.rahul.orderservice.dto.OrderResponse;
import com.rahul.orderservice.dto.PlaceOrderRequest;
import com.rahul.orderservice.entity.Order;
import com.rahul.orderservice.entity.OrderStatus;
import com.rahul.orderservice.service.OrderService;
import com.rahul.orderservice.service.OrderStatusEmitters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final long LONG_POLL_TIMEOUT_MS = 10_000;

    private final OrderService orderService;
    private final OrderStatusEmitters orderStatusEmitters;

    @PostMapping("/place")
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}/stream")
    public SseEmitter streamOrderStatus(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);

        SseEmitter emitter = new SseEmitter();

        if (response.getStatus() != OrderStatus.PENDING) {
            // already resolved - send immediately, nothing to wait for
            try {
                emitter.send(response);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        } else {
            // still pending - hold the stream open, a listener will push later
            orderStatusEmitters.register(id, emitter);
        }

        return emitter;
    }
}