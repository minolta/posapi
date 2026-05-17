package me.pixka.pos.backup.api

import com.fasterxml.jackson.core.JsonProcessingException
import me.pixka.pos.backup.service.BackupService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.time.LocalDate

@RestController
@RequestMapping("/api/backup")
class BackupController(
    private val backupService: BackupService,
) {
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.CREATED)
    fun exportAllRecords(
        @RequestParam(required = false) ordersFromDate: LocalDate?,
        @RequestParam(required = false) ordersToDate: LocalDate?,
    ): BackupExportResponse {
        val result =
            backupService.exportAllRecords(
                ordersFromDate = ordersFromDate,
                ordersToDate = ordersToDate,
            )
        val zipFile = result.zipPath
        return BackupExportResponse(
            fileName = zipFile.fileName.toString(),
            filePath = zipFile.toAbsolutePath().toString(),
            bytes = Files.size(zipFile),
            message = result.message,
            ordersExported = result.ordersExported,
            ordersFromDate = result.ordersFromDate?.toString(),
            ordersToDate = result.ordersToDate?.toString(),
        )
    }

    /** Download an exported `.zip` (or legacy `.json`) from `app.backup-dir` (`fileName` from export response). */
    @GetMapping("/download")
    fun download(@RequestParam fileName: String): ResponseEntity<ByteArray> {
        val bytes = try {
            backupService.readBackupFile(fileName)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        val contentType =
            when {
                fileName.endsWith(".zip", ignoreCase = true) ->
                    MediaType.parseMediaType("application/zip")

                fileName.endsWith(".json", ignoreCase = true) ->
                    MediaType.APPLICATION_JSON

                else ->
                    MediaType.APPLICATION_OCTET_STREAM
            }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }

    /**
     * Replaces all POS data with the JSON body (same format as export).
     * Requires `confirm=true` (query) to avoid accidental wipes.
     */
    @PostMapping("/import", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun importJson(
        @RequestBody body: String,
        @RequestParam(defaultValue = "false") confirm: Boolean,
    ): BackupImportResponse {
        if (!confirm) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Set query parameter confirm=true to replace all database content with this backup.",
            )
        }
        return try {
            backupService.importFromJson(body, confirm)
        } catch (e: JsonProcessingException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
    }

    /** Import from raw JSON (`POST`) or multipart `file` (.zip bundle or `.json`). */
    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun importMultipart(
        @RequestPart("file") file: MultipartFile,
        @RequestParam(defaultValue = "false") confirm: Boolean,
    ): BackupImportResponse {
        if (!confirm) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Set query parameter confirm=true to replace all database content with this backup.",
            )
        }
        val json = try {
            backupService.decodeUploadedBackup(file.bytes, file.originalFilename)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
        return try {
            backupService.importFromJson(json, confirm)
        } catch (e: JsonProcessingException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
    }
}
