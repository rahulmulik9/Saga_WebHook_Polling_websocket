package com.rahul.orderservice.service;

import com.rahul.orderservice.dto.OrderResponse;
import com.rahul.orderservice.dto.PlaceOrderRequest;
import com.rahul.orderservice.dto.sagaDto.KafkaTopics;
import com.rahul.orderservice.dto.sagaDto.OrderCreated;
import com.rahul.orderservice.dto.sagaDto.OrderItemEvent;
import com.rahul.orderservice.entity.Order;
import com.rahul.orderservice.entity.OrderItem;
import com.rahul.orderservice.entity.OrderStatus;
import com.rahul.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        return new OrderResponse(order.getId(), order.getStatus(), order.getCreatedAt());
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }


    // No stock check, no payment call here anymore - orderservice no longer
    // decides the order's outcome. It just records intent as PENDING and
    // hands control to the saga orchestrator (wired in from Step 3 onward).
    @Transactional
    public Order placeOrder(PlaceOrderRequest request) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setCallbackUrl(request.getCallbackUrl());

        request.getItems().forEach(line -> {
            OrderItem item = new OrderItem();
            item.setProductId(line.getProductId());
            item.setQuantity(line.getQuantity());
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order savedOrder = orderRepository.save(order);

        List<OrderItemEvent> items = savedOrder.getItems().stream()
                .map(i -> new OrderItemEvent(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());

        OrderCreated event = new OrderCreated(savedOrder.getId(), items);
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event);

        return savedOrder;
    }
}