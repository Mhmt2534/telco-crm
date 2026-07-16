import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Should emit PaymentCompleted event when payment is successful")
    label("trigger_payment_completed")

    // Defines the method that should be called to trigger the message publication
    input {
        triggeredBy("triggerPaymentCompleted()")
    }

    // Defines the message that will be sent
    outputMessage {
        // The destination (Kafka topic)
        sentTo("telcox.Payment.events")

        // The expected payload
        body(
                paymentId: $(regex(uuid())),
                orderId: "00000000-0000-0000-0000-000000000001",
                invoiceId: null,
                customerId: "00000000-0000-0000-0000-000000012345",
                amount: 100.00,
                occurredAt: $(consumer('2026-01-01T10:00:00Z'), producer(regex('.*')))
        )

        // The expected headers
        headers {
            header('eventType', 'PaymentCompleted')
        }
    }
}
