package com.gubee.stockreconciliation.adapter.inbound.rest

import com.gubee.stockreconciliation.domain.model.EventStatus

data class EventResponse(
    val eventId: String,
    val status: EventStatus,
    val currentStock: Int? = null,
    val message: String? = null
)