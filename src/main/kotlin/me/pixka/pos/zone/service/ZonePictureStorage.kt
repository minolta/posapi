package me.pixka.pos.zone.service

import jakarta.annotation.PostConstruct
import me.pixka.pos.common.exception.PictureValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

@Component
class ZonePictureStorage(
    @Value("\${app.zone-picture-upload-dir}") private val uploadDir: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun logConfiguredDirectory() {
        val root = uploadRoot()
        log.info("Zone picture uploads resolved to {}", root)
    }

    fun uploadRoot(): Path =
        Paths.get(uploadDir).toAbsolutePath().normalize().also {
            Files.createDirectories(it)
        }

    fun fileFor(zoneId: Long, extension: String): Path =
        uploadRoot().resolve("zone-$zoneId.$extension")

    fun deleteAllForZone(zoneId: Long) {
        val dir = uploadRoot()
        if (dir.notExists() || !dir.isDirectory()) {
            return
        }
        Files.list(dir).use { stream ->
            stream.filter { it.fileName.toString().startsWith("zone-$zoneId.") }
                .forEach { it.deleteIfExists() }
        }
    }

    fun replaceFromStream(zoneId: Long, extension: String, input: java.io.InputStream) {
        val target = fileFor(zoneId, extension)
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
    }

    fun resolveExtension(contentType: String?, originalFilename: String?): String {
        extensionFromContentType(contentType)?.let { return it }
        extensionFromFilename(originalFilename)?.let { return it }
        throw PictureValidationException("Unsupported or missing image type (use JPEG, PNG, GIF, or WebP).")
    }

    fun mimeTypeForExtension(ext: String): MediaType =
        when (ext.lowercase()) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "gif" -> MediaType.IMAGE_GIF
            "webp" -> MediaType("image", "webp")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }

    private fun extensionFromContentType(contentType: String?): String? {
        val t = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        return when (t) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> null
        }
    }

    private fun extensionFromFilename(originalFilename: String?): String? {
        val raw = originalFilename ?: return null
        val name = raw.substringAfterLast('/').substringAfterLast('\\')
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.length - 1) {
            return null
        }
        val ext = name.substring(dot + 1).lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "jpg"
            "png", "gif", "webp" -> ext
            else -> null
        }
    }
}
