package com.rahul.orderservice.dto;

import com.rahul.orderservice.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private Long id;

    private OrderStatus status;

    private LocalDateTime createdAt;
}