package com.telcox.springmicroservices.subscription.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Yeni abonelik oluşturma isteği.
 * Order Service tarafından Saga akışı içinde çağrılır.
 */
public record CreateSubscriptionRequest(

                @NotNull(message = "Müşteri ID boş olamaz") UUID customerId,

                @NotNull(message = "Tarife ID boş olamaz") UUID tariffId,

                @NotBlank(message = "Tarife kodu boş olamaz") String tariffCode,

                UUID orderId) {
}
