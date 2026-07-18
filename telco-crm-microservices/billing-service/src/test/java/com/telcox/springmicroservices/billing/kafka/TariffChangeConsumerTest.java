package com.telcox.springmicroservices.billing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telcox.springmicroservices.billing.dto.TariffChangeRequestedEvent;
import com.telcox.springmicroservices.billing.entity.PendingCharge;
import com.telcox.springmicroservices.billing.repository.PendingChargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TariffChangeConsumerTest {

    private PendingChargeRepository pendingChargeRepository;
    private ObjectMapper objectMapper;
    private TariffChangeConsumer consumer;

    @BeforeEach
    void setUp() {
        pendingChargeRepository = mock(PendingChargeRepository.class);
        objectMapper = new ObjectMapper();
        consumer = new TariffChangeConsumer(pendingChargeRepository, objectMapper);
    }

    @Test
    void testConsume_ImmediateTariffChange() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        TariffChangeRequestedEvent event = new TariffChangeRequestedEvent();
        event.setOrderId(orderId);
        event.setSubscriptionId(subscriptionId);
        event.setCustomerId(customerId);
        event.setOldTariffCode("OLD_CODE");
        event.setNewTariffCode("NEW_CODE");
        event.setPriceDiff(new BigDecimal("15.50"));
        event.setEffectiveBillCycle("IMMEDIATE");

        String payload = objectMapper.writeValueAsString(event);

        when(pendingChargeRepository.existsByOrderId(orderId)).thenReturn(false);

        consumer.consume(payload, "TariffChangeRequested");

        ArgumentCaptor<PendingCharge> captor = ArgumentCaptor.forClass(PendingCharge.class);
        verify(pendingChargeRepository, times(1)).save(captor.capture());

        PendingCharge saved = captor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertEquals(subscriptionId, saved.getSubscriptionId());
        assertEquals(customerId, saved.getCustomerId());
        assertEquals("Tariff Change: OLD_CODE -> NEW_CODE", saved.getDescription());
        assertTrue(new BigDecimal("15.50").compareTo(saved.getAmount()) == 0);
        assertEquals("IMMEDIATE", saved.getEffectiveBillCycle());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void testConsume_NextCycleTariffChange() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        TariffChangeRequestedEvent event = new TariffChangeRequestedEvent();
        event.setOrderId(orderId);
        event.setSubscriptionId(subscriptionId);
        event.setCustomerId(customerId);
        event.setOldTariffCode("OLD_CODE");
        event.setNewTariffCode("NEW_CODE");
        event.setPriceDiff(new BigDecimal("-10.00"));
        event.setEffectiveBillCycle("NEXT_CYCLE");

        String payload = objectMapper.writeValueAsString(event);

        when(pendingChargeRepository.existsByOrderId(orderId)).thenReturn(false);

        consumer.consume(payload, "TariffChangeRequested");

        ArgumentCaptor<PendingCharge> captor = ArgumentCaptor.forClass(PendingCharge.class);
        verify(pendingChargeRepository, times(1)).save(captor.capture());

        PendingCharge saved = captor.getValue();
        assertEquals(orderId, saved.getOrderId());
        assertTrue(new BigDecimal("-10.00").compareTo(saved.getAmount()) == 0);
        assertEquals("NEXT_CYCLE", saved.getEffectiveBillCycle());
        assertEquals("PENDING_NEXT", saved.getStatus());
    }

    @Test
    void testConsume_IdempotentCheck() throws Exception {
        UUID orderId = UUID.randomUUID();

        TariffChangeRequestedEvent event = new TariffChangeRequestedEvent();
        event.setOrderId(orderId);

        String payload = objectMapper.writeValueAsString(event);

        when(pendingChargeRepository.existsByOrderId(orderId)).thenReturn(true);

        consumer.consume(payload, "TariffChangeRequested");

        verify(pendingChargeRepository, never()).save(any());
    }
}
