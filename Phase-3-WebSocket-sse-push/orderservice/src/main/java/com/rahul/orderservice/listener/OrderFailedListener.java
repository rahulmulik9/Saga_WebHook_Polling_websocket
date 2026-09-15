package com.rahul.orderservice.listener;

import com.rahul.orderservice.dto.OrderResponse;
import com.rahul.orderservice.dto.sagaDto.KafkaTopics;
import com.rahul.orderservice.dto.sagaDto.OrderFailedCommand;
import com.rahul.orderservice.entity.Order;
import com.rahul.orderservice.entity.OrderStatus;
import com.rahul.orderservice.entity.ProcessedEvent;
import com.rahul.orderservice.repository.OrderRepository;
import com.rahul.orderservice.repository.ProcessedEventRepository;
import com.rahul.orderservice.service.OrderStatusWaiters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFailedListener {

    private static final String EVENT_TYPE = "ORDER_FAILED";

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderStatusWaiters orderStatusWaiters;

    @KafkaListener(topics = KafkaTopics.ORDER_FAILED, containerFactory = "orderFailedContainerFactory")
    @Transactional
    public void handle(OrderFailedCommand command) {
        if (processedEventRepository.existsByOrderIdAndEventType(command.getOrderId(), EVENT_TYPE)) {
            log.info("orderId={} already processed for {}, skipping (idempotent)", command.getOrderId(), EVENT_TYPE);
            return;
        }

        Order order = orderRepository.findById(command.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("No Order found for orderId={}, ignoring OrderFailedCommand", command.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        orderStatusWaiters.notifyStatusChanged(
                order.getId(),
                new OrderResponse(order.getId(), order.getStatus(), order.getCreatedAt()));

        processedEventRepository.save(new ProcessedEvent(null, command.getOrderId(), EVENT_TYPE, LocalDateTime.now()));

        log.info("Order {} marked FAILED, reason={}", command.getOrderId(), command.getReason());
    }
}
