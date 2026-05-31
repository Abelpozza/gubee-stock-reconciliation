package com.gubee.stockreconciliation

import com.gubee.stockreconciliation.adapter.inbound.rest.EventRequest
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockEventRepository
import com.gubee.stockreconciliation.adapter.outbound.persistence.StockBalanceRepository
import com.gubee.stockreconciliation.application.usecase.ProcessEventUseCase
import com.gubee.stockreconciliation.domain.model.EventStatus
import com.gubee.stockreconciliation.domain.model.EventType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
open class StockReconciliationApplicationTests {

    @Autowired
    lateinit var processEventUseCase: ProcessEventUseCase

    @Autowired
    lateinit var stockEventRepository: StockEventRepository

    @Autowired
    lateinit var stockBalanceRepository: StockBalanceRepository

    @BeforeEach
    fun cleanup() {
        stockEventRepository.deleteAll()
        stockBalanceRepository.deleteAll()
    }

    // Cenário 1 — Ajuste de estoque simples
    @Test
    open fun `cenario 1 - ajuste de estoque define saldo corretamente`() {
        val response = processEventUseCase.execute(req(
            eventId = "c1-evt-001",
            type = EventType.STOCK_ADJUSTED,
            available = 10
        ))
        assertEquals(EventStatus.PROCESSED, response.status)
        assertEquals(10, response.currentStock)
    }

    // Cenário 2 — Venda reduz estoque
    @Test
    open fun `cenario 2 - venda reduz estoque corretamente`() {
        processEventUseCase.execute(req("c2-evt-001", EventType.STOCK_ADJUSTED, available = 10))

        val response = processEventUseCase.execute(req(
            eventId = "c2-evt-002",
            type = EventType.ORDER_CREATED,
            quantity = 3,
            externalOrderId = "ML-C2"
        ))
        assertEquals(EventStatus.PROCESSED, response.status)
        assertEquals(7, response.currentStock)
    }

    // Cenário 3 — Cancelamento restaura estoque
    @Test
    open fun `cenario 3 - cancelamento restaura estoque`() {
        processEventUseCase.execute(req("c3-evt-001", EventType.STOCK_ADJUSTED, available = 10))
        processEventUseCase.execute(req("c3-evt-002", EventType.ORDER_CREATED, quantity = 3, externalOrderId = "ML-C3"))

        val response = processEventUseCase.execute(req(
            eventId = "c3-evt-003",
            type = EventType.ORDER_CANCELLED,
            quantity = 3,
            externalOrderId = "ML-C3"
        ))
        assertEquals(EventStatus.PROCESSED, response.status)
        assertEquals(10, response.currentStock)
    }

    // Cenário 4 — Idempotência: mesmo eventId ignorado
    @Test
    open fun `cenario 4 - evento duplicado e ignorado por idempotencia`() {
        val request = req("c4-evt-001", EventType.STOCK_ADJUSTED, available = 10)
        processEventUseCase.execute(request)

        val response = processEventUseCase.execute(request)
        assertEquals(EventStatus.IGNORED, response.status)
    }

    // Cenário 5 — Estoque insuficiente marca INCONSISTENT
    @Test
    open fun `cenario 5 - venda com estoque insuficiente marca INCONSISTENT`() {
        processEventUseCase.execute(req("c5-evt-001", EventType.STOCK_ADJUSTED, available = 2))

        val response = processEventUseCase.execute(req(
            eventId = "c5-evt-002",
            type = EventType.ORDER_CREATED,
            quantity = 5,
            externalOrderId = "ML-C5"
        ))
        assertEquals(EventStatus.INCONSISTENT, response.status)
    }

    // Cenário 6 — Evento fora de ordem fica PENDING e é reprocessado
    @Test
    open fun `cenario 6 - cancelamento antes da venda fica PENDING e reprocessa`() {
        processEventUseCase.execute(req("c6-evt-001", EventType.STOCK_ADJUSTED, available = 10))

        val pendingResponse = processEventUseCase.execute(req(
            eventId = "c6-evt-003",
            type = EventType.ORDER_CANCELLED,
            quantity = 3,
            externalOrderId = "ML-C6"
        ))
        assertEquals(EventStatus.PENDING, pendingResponse.status)

        processEventUseCase.execute(req(
            eventId = "c6-evt-002",
            type = EventType.ORDER_CREATED,
            quantity = 3,
            externalOrderId = "ML-C6"
        ))

        val pendingEvent = stockEventRepository.findByEventId("c6-evt-003")!!
        val reprocessed = processEventUseCase.reprocess(pendingEvent)
        assertEquals(EventStatus.PROCESSED, reprocessed.status)
        assertEquals(10, reprocessed.currentStock)
    }

    // Cenário 7 — Duplicidade lógica: duas vendas pro mesmo pedido
    @Test
    open fun `cenario 7 - duplicidade logica bloqueia segunda venda do mesmo pedido`() {
        processEventUseCase.execute(req("c7-evt-001", EventType.STOCK_ADJUSTED, available = 10))
        processEventUseCase.execute(req("c7-evt-002", EventType.ORDER_CREATED, quantity = 3, externalOrderId = "ML-C7"))

        val response = processEventUseCase.execute(req(
            eventId = "c7-evt-003",
            type = EventType.ORDER_CREATED,
            quantity = 3,
            externalOrderId = "ML-C7"
        ))
        assertEquals(EventStatus.INCONSISTENT, response.status)
    }

    // Cenário 7b — Concorrência: dois pedidos simultâneos não geram saldo negativo
    @Test
    open fun `cenario 7b - concorrencia dois pedidos simultaneos nao geram saldo negativo`() {
        processEventUseCase.execute(req("c7b-evt-001", EventType.STOCK_ADJUSTED, available = 5))

        val threads = (1..10).map { i ->
            Thread {
                try {
                    processEventUseCase.execute(req(
                        eventId = "c7b-evt-${i + 1}",
                        type = EventType.ORDER_CREATED,
                        quantity = 1,
                        externalOrderId = "ML-C7B-$i"
                    ))
                } catch (e: Exception) {
                    // algumas threads podem falhar por lock — esperado
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val balance = stockBalanceRepository.findByAccountIdAndSku("account-test", "SKU-TEST")
        val finalStock = balance?.quantity ?: 0
        assertTrue(finalStock >= 0, "Estoque não pode ser negativo: $finalStock")
    }

    // Cenário 8 — MARKETPLACE_STOCK_RESTORED recompõe estoque
    @Test
    open fun `cenario 8 - marketplace stock restored recompoe estoque apos cancelamento`() {
        processEventUseCase.execute(req("c8-evt-001", EventType.STOCK_ADJUSTED, available = 10))
        processEventUseCase.execute(req("c8-evt-002", EventType.ORDER_CREATED, quantity = 4, externalOrderId = "ML-C8"))
        processEventUseCase.execute(req("c8-evt-003", EventType.ORDER_CANCELLED, quantity = 4, externalOrderId = "ML-C8"))

        val response = processEventUseCase.execute(req(
            eventId = "c8-evt-004",
            type = EventType.MARKETPLACE_STOCK_RESTORED,
            quantity = 4,
            externalOrderId = "ML-C8"
        ))
        assertEquals(EventStatus.INCONSISTENT, response.status)

        val balance = stockBalanceRepository.findByAccountIdAndSku("account-test", "SKU-TEST")
        assertEquals(10, balance?.quantity)
    }

    // --- helper ---
    private fun req(
        eventId: String,
        type: EventType,
        accountId: String = "account-test",
        sku: String = "SKU-TEST",
        available: Int? = null,
        quantity: Int? = null,
        externalOrderId: String? = null
    ) = EventRequest(
        eventId = eventId,
        type = type,
        occurredAt = Instant.now(),
        accountId = accountId,
        sku = sku,
        marketplace = if (type != EventType.STOCK_ADJUSTED) "MERCADO_LIVRE" else null,
        externalOrderId = externalOrderId,
        quantity = quantity,
        available = available,
        quantitySent = null,
        reason = null
    )
}