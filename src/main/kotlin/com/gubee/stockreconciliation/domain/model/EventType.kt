package com.gubee.stockreconciliation.domain.model

enum class EventType {
    ORDER_CREATED,
    ORDER_CANCELLED,
    STOCK_ADJUSTED,
    STOCK_SYNC_SENT,
    MARKETPLACE_STOCK_RESTORED
}