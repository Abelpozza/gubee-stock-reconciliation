package com.gubee.stockreconciliation.domain.model

import jakarta.persistence.*
import java.time.Instant


@Entity
@Table(name = "stock_events")
class StockEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "event_id", unique = true, nullable = false)
    val eventId: String,

    @Enumerated(EnumType.STRING)
    @Column (nullable = false)
    val type: EventType,

    @Column(name = "account_id", nullable = false)
    val accountId: String,

    @Column(nullable = false)
    val sku: String,
    val marketplace: String? = null,

    @Column(name = "external_order_id")
    val externalOrderId: String? = null,
    val quantity: Int? = null,
    val available: Int? = null,

    @Column(name = "quantity_sent")
    val quantitySent: Int? = null,
    val reason: String? = null,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,

    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PROCESSED,

    @Column(name = "ignore_reason")
    var ignoreReason: String? = null
)