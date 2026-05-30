package com.gubee.stockreconciliation.adapter.inbound.rest

import java.time.Instant

data class StockResponse(
    val accountId: String,
    val sku: String,
    val quantity: Int,
    val updatedAt: Instant
)