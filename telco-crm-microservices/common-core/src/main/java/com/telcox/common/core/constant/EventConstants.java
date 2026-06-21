package com.telcox.common.core.constant;

/**
 * Canonical domain event type names. ALWAYS reference these constants when publishing or
 * consuming events so producers and consumers never drift on string literals.
 *
 * <p>Topic naming, partitioning and DLQ strategy are intentionally NOT defined here — those
 * are an architecture decision to be agreed by the team.</p>
 */
public final class EventConstants {

    // ── Customer ──
    public static final String CUSTOMER_REGISTERED = "CustomerRegistered";
    public static final String CUSTOMER_KYC_APPROVED = "CustomerKYCApproved";
    public static final String CUSTOMER_KYC_REJECTED = "CustomerKYCRejected";

    // ── Subscription / MSISDN ──
    public static final String MSISDN_ALLOCATED = "MSISDNAllocated";
    public static final String MSISDN_RELEASED = "MSISDNReleased";
    public static final String SUBSCRIPTION_ACTIVATED = "SubscriptionActivated";
    public static final String SUBSCRIPTION_SUSPENDED = "SubscriptionSuspended";
    public static final String SUBSCRIPTION_TERMINATED = "SubscriptionTerminated";

    // ── Order ──
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";

    // ── Product Catalog ──
    public static final String TARIFF_CHANGED = "TariffChanged";
    public static final String ADDON_PURCHASED = "AddonPurchased";

    // ── Usage ──
    public static final String USAGE_RECORDED = "UsageRecorded";
    public static final String QUOTA_THRESHOLD_REACHED = "QuotaThresholdReached";
    public static final String QUOTA_EXCEEDED = "QuotaExceeded";

    // ── Billing ──
    public static final String INVOICE_GENERATED = "InvoiceGenerated";
    public static final String INVOICE_PAID = "InvoicePaid";
    public static final String INVOICE_OVERDUE = "InvoiceOverdue";

    // ── Payment ──
    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";

    // ── Ticket ──
    public static final String TICKET_OPENED = "TicketOpened";
    public static final String TICKET_ASSIGNED = "TicketAssigned";
    public static final String TICKET_RESOLVED = "TicketResolved";

    // ── Notification ──
    public static final String NOTIFICATION_DISPATCHED = "NotificationDispatched";

    private EventConstants() {
    }
}
