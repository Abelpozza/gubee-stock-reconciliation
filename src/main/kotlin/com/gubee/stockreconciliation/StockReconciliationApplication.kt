package com.gubee.stockreconciliation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class StockReconciliationApplication

fun main(args: Array<String>) {
	runApplication<StockReconciliationApplication>(*args)
}
