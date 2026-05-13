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
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@RestController
@RequestMapping("/api/backup")
class BackupController(
    private val backupService: BackupService,
) {
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.CREATED)
    fun exportAllRecords(): BackupExportResponse {
        val file = backupService.exportAllRecords()
        return BackupExportResponse(
            fileName = file.fileName.toString(),
            filePath = file.toAbsolutePath().toString(),
            bytes = Files.size(file),
            message = "Backup exported successfully.",
        )
    }

    /** Download a backup JSON file created under `app.backup-dir` (use `fileName` from export response). */
    @GetMapping("/download", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun download(@RequestParam fileName: String): ResponseEntity<ByteArray> {
        val bytes = try {
            backupService.readBackupFile(fileName)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message, e)
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .contentType(MediaType.APPLICATION_JSON)
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

    /** Same as JSON import but upload a `.json` file as `file` part. */
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
        val json = String(file.bytes, StandardCharsets.UTF_8)
        return try {
            backupService.importFromJson(json, confirm)
        } catch (e: JsonProcessingException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
        }
    }
}
