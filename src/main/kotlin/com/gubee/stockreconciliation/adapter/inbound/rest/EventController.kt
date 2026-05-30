package com.gubee.stockreconciliation.adapter.inbound.rest

import com.gubee.stockreconciliation.application.usecase.ProcessEventUseCase
import com.gubee.stockreconciliation.application.usecase.QueryStockUseCase
import com.gubee.stockreconciliation.domain.model.EventStatus
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventController(
    private val processEventUseCase: ProcessEventUseCase,
    private val queryStockUseCase: QueryStockUseCase
    ) {
    @PostMapping
    fun receive(@Valid @RequestBody request: EventRequest): ResponseEntity<EventResponse> =
        ResponseEntity.ok(processEventUseCase.execute(request))

    @GetMapping
    fun listByStatus(@RequestParam status: EventStatus): List<StockHistoryResponse> =
        queryStockUseCase.listByStatus(status)
}