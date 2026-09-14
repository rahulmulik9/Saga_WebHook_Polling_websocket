package com.rahul.orderservice.controller;

import com.rahul.orderservice.dto.OrderResponse;
import com.rahul.orderservice.dto.PlaceOrderRequest;
import com.rahul.orderservice.entity.Order;
import com.rahul.orderservice.entity.OrderStatus;
import com.rahul.orderservice.service.OrderService;
import com.rahul.orderservice.service.OrderStatusWaiters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.context.request.async.DeferredResult;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final long LONG_POLL_TIMEOUT_MS = 10_000;

    private final OrderService orderService;
    private final OrderStatusWaiters orderStatusWaiters;

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


    @GetMapping("/{id}/poll-long")
    public DeferredResult<OrderResponse> pollLongOrderStatus(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);

        DeferredResult<OrderResponse> deferredResult = new DeferredResult<>(LONG_POLL_TIMEOUT_MS, response);
        // ^ if nothing completes it within 10s, Spring auto-completes it
        //   with this same "response" (current status at request time) as the fallback

        if (response.getStatus() != OrderStatus.PENDING) {
            deferredResult.setResult(response);
        } else {
            orderStatusWaiters.register(id, deferredResult);
        }

        return deferredResult;
    }
}