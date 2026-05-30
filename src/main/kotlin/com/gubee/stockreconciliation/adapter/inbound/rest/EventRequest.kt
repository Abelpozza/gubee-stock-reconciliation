package com.gubee.stockreconciliation.adapter.inbound.rest

import com.gubee.stockreconciliation.domain.model.EventType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class EventRequest(
    @field:NotBlank
    val eventId: String,

    @field:NotNull
    val type: EventType,

    @field:NotNull
    val occurredAt: Instant,

    @field:NotBlank
    val accountId: String,

    @field:NotBlank
    val sku: String,

    val marketplacec: String? = null,
    val externalOrderId: String? = null,
    val quantity: Int? = null,
    val available: Int? = null,
    val quantitySent: Int? = null,
    val reason: String? = null
    )