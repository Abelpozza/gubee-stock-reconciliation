package com.gubee.stockreconciliation.application.usecase

import com.gubee.stockreconciliation.adapter.inbound.rest.StockHistoryResponse
import com.gubee.stockreconciliation.adapter.inbound.rest.StockResponse
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockBalanceRepository
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockEventRepository
import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.StockEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QueryStockUseCase(
    private val stockBalanceRepository: StockBalanceRepository,
    private val stockEventRepository: StockEventRepository
) {

    @Transactional(readOnly = true)
    fun getStock(accountId: String, sku: String): StockResponse? {
        val balance = stockBalanceRepository.findByAccountIdAndSku(accountId, sku) ?: return null
        return StockResponse(
            accountId = balance.id.accountId,
            sku = balance.id.sku,
            quantity = balance.quantity,
            updatedAt = balance.updatedAt
        )
    }


    @Transactional(readOnly = true)
    fun getHistory(accountId: String, sku: String): List<StockHistoryResponse> =
        stockEventRepository.findByAccountIdAndSkuOrderByOccurredAtAsc(accountId, sku)
            .map { it.toHistoryResponse() }

    @Transactional(readOnly = true)
    fun listByStatus(status: EventStatus): List<StockHistoryResponse> =
        stockEventRepository.findByStatus(status).map { it.toHistoryResponse() }

    private fun StockEvent.toHistoryResponse() = StockHistoryResponse(
        eventId = eventId,
        accountId = accountId,
        sku = sku,
        type = type,
        status = status,
        quantity = quantity,
        available = available,
        marketplace = marketplace,
        externalOrderId = externalOrderId,
        occurredAt = occurredAt,
        ignoreReason = ignoreReason
    )
}