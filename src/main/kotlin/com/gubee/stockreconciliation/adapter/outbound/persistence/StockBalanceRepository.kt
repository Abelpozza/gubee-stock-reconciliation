package com.gubee.stockreconciliation.adapter.outbound.persistence

import com.gubee.stockreconciliation.domain.model.StockBalance
import com.gubee.stockreconciliation.domain.model.StockBalanceId
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StockBalanceRepository : JpaRepository<StockBalance, StockBalanceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockBalance s WHERE s.id.accountId = accountId AND s.id.sku = :sku")
    fun findByIdWithLock(accountId: String, sku: String): StockBalance?

    @Query("SELECT s FROM StockBalance s WHERE s.id.accountId = accountID AND s.id.sku = :sku")
    fun findByAccountIdAndSku(accountId: String, sku: String): StockBalance?
}