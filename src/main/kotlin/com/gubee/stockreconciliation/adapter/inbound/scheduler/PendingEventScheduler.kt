package com.gubee.stockreconciliation.adapter.inbound.scheduler

import com.gubee.stockreconciliation.adapter.outbound.persistence.StockEventRepository
import com.gubee.stockreconciliation.application.usecase.ProcessEventUseCase
import com.gubee.stockreconciliation.domain.model.EventStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PendingEventScheduler(
    private val stockEventRepository: StockEventRepository,
    private val processEventUseCase: ProcessEventUseCase
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 30000) // roda 30s após o término da execução anterior
    fun reprocessPendingEvents() {
        val pending = stockEventRepository.findByStatus(EventStatus.PENDING)

        if (pending.isEmpty()) return

        logger.info("Scheduler: encontrados ${pending.size} evento(s) PENDING para reprocessar")

        pending.forEach { event ->
            try {
                processEventUseCase.reprocess(event)
                logger.info("Scheduler: evento ${event.eventId} reprocessado com sucesso")
            } catch (e: Exception) {
                logger.warn("Scheduler: falha ao reprocessar ${event.eventId} — ${e.message}")
            }
        }
    }
}