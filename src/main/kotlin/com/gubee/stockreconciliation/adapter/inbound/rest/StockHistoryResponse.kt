package com.gubee.stockreconciliation.adapter.inbound.rest

import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import java.time.Instant

data class StockHistoryResponse(
    val eventId: String,
    val accountId: String,
    val sku: String,
    val type: EventType,
    val status: EventStatus,
    val quantity: Int? = null,
    val available: Int? = null,
    val marketplace: String? = null,
    val externalOrderId: String? = null,
    val occurredAt: Instant,
    val ignoreReason: String? = null
)