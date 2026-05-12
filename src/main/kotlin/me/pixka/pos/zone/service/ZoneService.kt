package me.pixka.pos.zone.service

import me.pixka.pos.common.exception.PictureValidationException
import me.pixka.pos.zone.api.ZoneRequest
import me.pixka.pos.zone.exception.ZoneNotFoundException
import me.pixka.pos.zone.model.Zone
import me.pixka.pos.zone.repository.ZoneRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@Service
class ZoneService(
    private val zoneRepository: ZoneRepository,
    private val pictureStorage: ZonePictureStorage,
    @Value("\${app.zone-picture-max-bytes:5242880}") private val maxPictureBytes: Long,
) {
    fun create(request: ZoneRequest): Zone {
        val zone = Zone(
            code = request.code.trim(),
            name = request.name.trim()
        )
        return zoneRepository.save(zone)
    }

    fun update(id: Long, request: ZoneRequest): Zone {
        val zone = zoneRepository.findById(id).orElseThrow { ZoneNotFoundException(id) }
        zone.code = request.code.trim()
        zone.name = request.name.trim()
        return zoneRepository.save(zone)
    }

    fun delete(id: Long) {
        if (!zoneRepository.existsById(id)) {
            throw ZoneNotFoundException(id)
        }
        pictureStorage.deleteAllForZone(id)
        zoneRepository.deleteById(id)
    }

    fun search(q: String?): List<Zone> {
        val trimmed = q?.trim().orEmpty()
        return if (trimmed.isEmpty()) {
            zoneRepository.findAll()
        } else {
            zoneRepository.searchByCodeOrNameContaining(trimmed)
        }
    }

    fun savePicture(id: Long, file: MultipartFile): Zone {
        if (file.isEmpty) {
            throw PictureValidationException("Choose a non-empty image file.")
        }
        if (file.size > maxPictureBytes) {
            val mb = maxPictureBytes / 1024 / 1024
            throw PictureValidationException("Image is too large (max $mb MB).")
        }
        val zone = zoneRepository.findById(id).orElseThrow { ZoneNotFoundException(id) }
        val ext = pictureStorage.resolveExtension(file.contentType, file.originalFilename)
        pictureStorage.deleteAllForZone(id)
        file.inputStream.use { pictureStorage.replaceFromStream(id, ext, it) }
        zone.pictureExtension = ext
        return zoneRepository.save(zone)
    }

    @Transactional
    fun loadPicture(id: Long): Pair<Resource, MediaType>? {
        val zone = zoneRepository.findById(id).orElseThrow { ZoneNotFoundException(id) }
        val ext = zone.pictureExtension ?: return null
        val zid = zone.id ?: return null
        val path = pictureStorage.fileFor(zid, ext)
        if (!Files.exists(path)) {
            zone.pictureExtension = null
            zoneRepository.save(zone)
            return null
        }
        val resource: Resource = FileSystemResource(path.toFile())
        return resource to pictureStorage.mimeTypeForExtension(ext)
    }
}
