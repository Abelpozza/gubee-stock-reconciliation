package com.gubee.stockreconciliation.application.usecase

import com.gubee.stockreconciliation.adapter.inbound.rest.EventResponse
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockBalanceRepository
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockEventRepository
import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import com.sun.jdi.request.EventRequest
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class ProcessEventUseCase(
    private val stockEventRepository: StockEventRepository,
    private val stockBalanceRepository: StockBalanceRepository
) {
    private companion object {
        val RESTORING_TYPES = listOf(
            EventType.ORDER_CANCELED,
            EventType.MARKETPLACE_STOCK_RESTORED
        )
        val ORDER_CREATED_ONLY = listOf(EventType.ORDER_CREATED)
    }
    @Transactional
    fun execute(request: EventRequest): EventResponse {
        val existing = stockEventRepository.findByEventId(request.eventId)

        if (existing != null && existing.status != EventStatus.PENDING) {
            return EventResponse(
                eventId = existing.eventId,
                status = EventStatus.IGNORED,
                currentStock = currentStock(existing.accountId, existing.sku),
                message = "Evento já recebido - ignorado por idempotência (status real: ${existing.status})"
            )
        }
    }

}