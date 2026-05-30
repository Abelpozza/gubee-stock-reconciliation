package com.gubee.stockreconciliation.adapter.inbound.rest

import com.gubee.stockreconciliation.application.usecase.QueryStockUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stocks")
class StockController(
    private val queryStockUseCase: QueryStockUseCase
) {
    @GetMapping("/{accountId}/{sku}")
    fun getStock(
        @PathVariable accountId: String,
        @PathVariable sku: String
    ): ResponseEntity<StockResponse> {
        val stock = queryStockUseCase.getStock(accountId, sku)
        return if (stock != null) ResponseEntity.ok(stock)
        else ResponseEntity.notFound().build()
    }
    @GetMapping("/{accountId}/{sku}/history")
    fun getHistory(
        @PathVariable accountId: String,
        @PathVariable sku: String
    ): List<StockHistoryResponse> = queryStockUseCase.getHistory(accountId,sku)

}