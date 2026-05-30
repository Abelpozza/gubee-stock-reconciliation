package com.gubee.stockreconciliation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StockReconciliationApplication

fun main(args: Array<String>) {
	runApplication<StockReconciliationApplication>(*args)
}
