package me.pixka.pos.backup.api

data class BackupExportResponse(
    val fileName: String,
    val filePath: String,
    val bytes: Long,
    val message: String,
    /** Rows included in snapshot `orders` array (zones/foods/etc. are always complete). */
    val ordersExported: Int,
    val ordersFromDate: String? = null,
    val ordersToDate: String? = null,
)
