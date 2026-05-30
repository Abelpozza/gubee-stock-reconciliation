package com.gubee.stockreconciliation.adapter.outbound.persistence

import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import com.gubee.stockreconciliation.domain.model.StockEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StockEventRepository : JpaRepository<StockEvent, Long> {

    fun findByEventId(eventId: String): StockEvent?

    fun findByStatus(status: EventStatus): List<StockEvent>

    fun findByAccountIdAndSkuOrderByOccurredAtAsc(
        accountId: String,
        sku: String
    ): List<StockEvent>

    @Query("""
        SELECT COUNT(e) > 0 FROM StockEvent e
        WHERE e.accountId = :accountId
          AND e.sku = :sku
          AND e.externalOrderId = :externalOrderId
          AND e.type IN :types
          AND e.status = com.gubee.stockreconciliation.domain.model.EventStatus.PROCESSED
    """)
    fun existsProcessedForOrder(
        accountId: String,
        sku: String,
        externalOrderId: String,
        types: Collection<EventType>
    ): Boolean
}