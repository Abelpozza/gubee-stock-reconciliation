package com.gubee.stockreconciliation.application.usecase

import com.gubee.stockreconciliation.adapter.inbound.rest.EventRequest
import com.gubee.stockreconciliation.adapter.inbound.rest.EventResponse
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockBalanceRepository
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockEventRepository
import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import com.gubee.stockreconciliation.domain.model.StockBalance
import com.gubee.stockreconciliation.domain.model.StockBalanceId
import com.gubee.stockreconciliation.domain.model.StockEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ProcessEventUseCase(
    private val stockEventRepository: StockEventRepository,
    private val stockBalanceRepository: StockBalanceRepository
) {

    private companion object {
        val RESTORING_TYPES = listOf(
            EventType.ORDER_CANCELLED,
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
                message = "Evento já recebido — ignorado por idempotência (status real: ${existing.status})"
            )
        }

        val event = existing ?: request.toEntity()
        return route(event)
    }

    @Transactional
    fun reprocess(event: StockEvent): EventResponse = route(event)

    private fun route(event: StockEvent): EventResponse = when (event.type) {
        EventType.STOCK_ADJUSTED -> applyAdjustment(event)
        EventType.ORDER_CREATED -> applySale(event)
        EventType.ORDER_CANCELLED -> applyRestore(event)
        EventType.MARKETPLACE_STOCK_RESTORED -> applyRestore(event)
        EventType.STOCK_SYNC_SENT -> applySyncSent(event)
    }

    private fun applyAdjustment(event: StockEvent): EventResponse {
        val target = event.available
            ?: return finalize(event, EventStatus. INCONSISTENT, "STOCK_ADJUSTED sem 'available'")
        if (target < 0)
            return finalize(event, EventStatus.INCONSISTENT, "Ajuste com 'available' negativo")

        val balance = lockOrCreate(event.accountId, event.sku)
        balance.quantity = target
        balance.updatedAt = Instant.now()
        stockBalanceRepository.save(balance)
        return finalize(event, EventStatus.PROCESSED, null, balance.quantity)
    }

    private fun applySale(event: StockEvent): EventResponse {
        val qty = event.quantity
            ?: return finalize(event, EventStatus.INCONSISTENT, "ORDER_CREATED sem 'quantity'")

        val orderId = event.externalOrderId
        if (orderId != null &&
            stockEventRepository.existsProcessedForOrder(
                event.accountId, event.sku, orderId, ORDER_CREATED_ONLY
            )
        ) {
            return finalize(
                event, EventStatus.INCONSISTENT,
                "Duplicidade lógica: venda já processada para o pedido $orderId"
            )
        }

        val balance = lockOrCreate(event.accountId, event.sku)
        val resulting = balance.quantity - qty
        if (resulting < 0)
            return finalize(
                event, EventStatus.INCONSISTENT,
                "Estoque insuficiente: atual=${balance.quantity}, solicitado=$qty"
            )

        balance.quantity = resulting
        balance.updatedAt = Instant.now()
        stockBalanceRepository.save(balance)
        return finalize(event, EventStatus.PROCESSED, null, balance.quantity)
    }

    private fun applyRestore(event: StockEvent): EventResponse {
        val qty = event.quantity
            ?: return finalize(event, EventStatus.INCONSISTENT, "${event.type} sem 'quantity'")
        val orderId = event.externalOrderId
            ?: return finalize(event, EventStatus.INCONSISTENT, "${event.type} sem 'externalOrderId'")

        if (stockEventRepository.existsProcessedForOrder(
                event.accountId, event.sku, orderId, RESTORING_TYPES
            )
        ) {
            return finalize(
                event, EventStatus.INCONSISTENT,
                "Recomposição já aplicada para o pedido $orderId — ignorando duplicata"
            )
        }

        if (!stockEventRepository.existsProcessedForOrder(
                event.accountId, event.sku, orderId, ORDER_CREATED_ONLY
            )
        ) {
            return finalize(
                event, EventStatus.PENDING,
                "Aguardando ORDER_CREATED do pedido $orderId (evento fora de ordem)"
            )
        }

        val balance = lockOrCreate(event.accountId, event.sku)
        balance.quantity += qty
        balance.updatedAt = Instant.now()
        stockBalanceRepository.save(balance)
        return finalize(event, EventStatus.PROCESSED, null, balance.quantity)
    }

    private fun applySyncSent(event: StockEvent): EventResponse {
        return finalize(event, EventStatus.PROCESSED, null, currentStock(event.accountId, event.sku))
    }

    private fun lockOrCreate(accountId: String, sku: String): StockBalance =
        stockBalanceRepository.findByIdWithLock(accountId, sku)
            ?: StockBalance(StockBalanceId(accountId, sku), quantity = 0)

    private fun currentStock(accountId: String, sku: String): Int? =
        stockBalanceRepository.findByAccountIdAndSku(accountId, sku)?.quantity

    private fun finalize(
        event: StockEvent,
        status: EventStatus,
        reason: String?,
        stock: Int? = null
    ): EventResponse {
        event.status = status
        event.ignoreReason = reason
        val saved = stockEventRepository.save(event)
        return EventResponse(
            eventId = saved.eventId,
            status = saved.status,
            currentStock = stock,
            message = reason
        )
    }

    private fun EventRequest.toEntity() = StockEvent(
        eventId = eventId,
        type = type,
        accountId = accountId,
        sku = sku,
        marketplace = marketplace,
        externalOrderId = externalOrderId,
        quantity = quantity,
        available = available,
        quantitySent = quantitySent,
        reason = reason,
        occurredAt = occurredAt
    )
}