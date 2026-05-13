package me.pixka.pos.backup.api

data class BackupExportResponse(
    val fileName: String,
    val filePath: String,
    val bytes: Long,
    val message: String,
)
