package me.pixka.pos.backup.api

data class BackupImportResponse(
    val message: String,
    val zonesRestored: Int,
    val printersRestored: Int,
    val foodCategoriesRestored: Int,
    val kitchensRestored: Int,
    val tablesRestored: Int,
    val foodsRestored: Int,
    val ordersRestored: Int,
)
