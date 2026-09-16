package com.rahul.notificationservice.listener;

import com.rahul.notificationservice.dto.sagaDto.ConfirmOrderCommand;
import com.rahul.notificationservice.dto.sagaDto.KafkaTopics;
import com.rahul.notificationservice.dto.sagaDto.OrderFailedCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Teaching stand-in: a real system would send an email/push/SMS here.
// This just logs, to prove the notification path is fully decoupled
// from order-service and from whether any client is watching.
@Component
@Slf4j
public class NotificationListener {

    @KafkaListener(topics = KafkaTopics.ORDER_CONFIRM, containerFactory = "confirmOrderContainerFactory")
    public void handleConfirmed(ConfirmOrderCommand command) {
        log.info("[NOTIFY] Order {} completed successfully - would notify user now", command.getOrderId());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_FAILED, containerFactory = "orderFailedContainerFactory")
    public void handleFailed(OrderFailedCommand command) {
        log.info("[NOTIFY] Order {} failed (reason: {}) - would notify user now", command.getOrderId(), command.getReason());
    }
}