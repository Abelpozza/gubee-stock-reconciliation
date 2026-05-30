package com.gubee.stockreconciliation.adapter.inbound.rest

import java.time.Instant

data class StockResponse(
    val accounId: String,
    val sku: String,
    val quantity: Int,
    val updatedAt: Instant
)