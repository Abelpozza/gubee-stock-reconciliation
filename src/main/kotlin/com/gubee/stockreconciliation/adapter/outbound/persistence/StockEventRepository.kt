package com.gubee.stockreconciliation.adapter.outbound.persistence

import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import com.gubee.stockreconciliation.domain.model.StockEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StockEventRepository : JpaRepository<StockEvent, Long> {
    fun existsByEventId(eventId: String): Boolean
    fun findByStatus(status: EventStatus): List<StockEvent>
    fun findByAccountIdAndSkuOrderByOccurredAtAsc(
        accountId: String,
        sku: String ): List<StockEvent>


    @Query("""
        SELECT COUNT(e) > 0 FROM StockEvent e
        WHERE e.accountId = :accountId
        AND e.sku = :sku
        AND e.externalOrderId = :externalOrderId
        AND e.type = :type
        AND e.status = 'PROCESSED'
        """)
    fun existsProcessedEvent(
        accountId: String,
        sku: String,
        externalOrderId: String,
        type: EventType
    ): Boolean
}