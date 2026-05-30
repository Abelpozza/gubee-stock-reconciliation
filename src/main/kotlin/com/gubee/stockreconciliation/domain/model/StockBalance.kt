package com.gubee.stockreconciliation.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stock_balance")
class StockBalance(

    @EmbeddedId
    val id: StockBalanceId,

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

@Embeddable
data class StockBalanceId(
    @Column(name = "account_id")
    val accountId: String,
    @Column(name = "sku")
    val sku: String) : java.io.Serializable
