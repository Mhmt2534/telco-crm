package com.telcox.springmicroservices.orderservice.kafka.consumer;

import com.telcox.springmicroservices.orderservice.domain.entity.Order;
import com.telcox.springmicroservices.orderservice.domain.enums.OrderStatus;
import com.telcox.springmicroservices.orderservice.domain.entity.SagaState;
import com.telcox.springmicroservices.orderservice.domain.enums.SagaStatus;
import com.telcox.springmicroservices.orderservice.repository.OrderRepository;
import com.telcox.springmicroservices.orderservice.repository.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.datasource.url=jdbc:h2:mem:order_test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = { "telcox.Payment.events" })
@AutoConfigureStubRunner(
        ids = "com.telcox.springmicroservices:payment-service:+:stubs:8090",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(PaymentCompletedStubRunnerTest.MessagingConfig.class)
public class PaymentCompletedStubRunnerTest {

    @Autowired
    private StubTrigger stubTrigger;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    @TestConfiguration
    static class MessagingConfig {
        @Autowired
        private KafkaTemplate<String, Object> kafkaTemplate;

        @Bean("telcox.Payment.events")
        public MessageChannel paymentEventsChannel() {
            DirectChannel channel = new DirectChannel();
            channel.subscribe(message -> {
                Object payload = message.getPayload();
                String payloadStr = payload instanceof byte[] ? new String((byte[]) payload) : payload.toString();
                kafkaTemplate.send("telcox.Payment.events", payloadStr);
            });
            return channel;
        }
    }

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
        sagaStateRepository.deleteAll();
    }

    @Test
    public void shouldProcessPaymentCompletedEventFromStub() throws Exception {
        // 1. Prepare Order and SagaState for processing
        Order order = new Order();
        order.setPublicId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        order.setCustomerId(java.util.UUID.fromString("00000000-0000-0000-0000-000000012345"));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCurrency("TRY");
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order = orderRepository.saveAndFlush(order);

        SagaState sagaState = new SagaState();
        sagaState.setSagaId(java.util.UUID.randomUUID().toString());
        sagaState.setOrderId(order.getId());
        sagaState.setCurrentStep("PAYMENT");
        sagaState.setStatus(SagaStatus.STARTED);
        sagaStateRepository.save(sagaState);

        // 2. Trigger the message from the payment-service stub
        // This label matches the one defined in paymentCompleted.groovy
        stubTrigger.trigger("trigger_payment_completed");

        // 3. Wait for the asynchronous Kafka listener to process the message.
        // Poll the committed state instead of assuming that two seconds is enough.
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        Order updatedOrder;
        do {
            updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
            if (updatedOrder.getStatus() == OrderStatus.PAID) {
                break;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);

        // 4. Assert that Order and SagaState were updated
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());

        Optional<SagaState> updatedSagaStateOpt = sagaStateRepository.findByOrderId(order.getId());
        assertTrue(updatedSagaStateOpt.isPresent());
        SagaState updatedSagaState = updatedSagaStateOpt.get();
        assertEquals("STEP_2", updatedSagaState.getCurrentStep());
    }
}
