package com.rahul.notificationservice.dto.sagaDto;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String ORDER_CONFIRM = "order.confirm.command";
    public static final String ORDER_FAILED = "order.failed.command";
}