package com.rahul.orderservice.controller;

import com.rahul.orderservice.dto.OrderResponse;
import com.rahul.orderservice.dto.PlaceOrderRequest;
import com.rahul.orderservice.entity.Order;
import com.rahul.orderservice.entity.OrderStatus;
import com.rahul.orderservice.service.OrderService;
import com.rahul.orderservice.service.OrderStatusEmitters;
import com.rahul.orderservice.service.OrderStatusWaiters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private static final long LONG_POLL_TIMEOUT_MS = 10_000;

    private final OrderService orderService;
    private final OrderStatusWaiters orderStatusWaiters;
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


    @GetMapping("/{id}/poll-long")
    public DeferredResult<OrderResponse> pollLongOrderStatus(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);

        DeferredResult<OrderResponse> deferredResult = new DeferredResult<>(LONG_POLL_TIMEOUT_MS, response);
        /*When user calls this API:
             It checks the order status. If it is PENDING, it says: "Wait, I'll keep your request for up to 10 seconds."
             It stores that waiting request in the OrderStatusWaiters map.
             If the order becomes COMPLETED/FAILED, it immediately sends that status.
             (This is done in listener, where notify method is used to change the status )
             If nothing happens in 10 seconds, it sends PENDING.*/
        if (response.getStatus() != OrderStatus.PENDING) {
            deferredResult.setResult(response);
        } else {
            orderStatusWaiters.register(id, deferredResult);
        }

        return deferredResult;
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