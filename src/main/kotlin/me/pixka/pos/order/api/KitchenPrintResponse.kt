package me.pixka.pos.order.api

data class KitchenPrintResult(
    val kitchenId: Long,
    val kitchenName: String,
    val printerCode: String?,
    val printed: Boolean,
    val message: String?,
)

data class KitchenPrintResponse(
    val results: List<KitchenPrintResult>,
    val message: String? = null,
)
