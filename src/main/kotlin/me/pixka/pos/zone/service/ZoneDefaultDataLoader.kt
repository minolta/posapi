package me.pixka.pos.zone.service

import me.pixka.pos.zone.model.Zone
import me.pixka.pos.zone.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class ZoneDefaultDataLoader(
    private val zoneRepository: ZoneRepository,
    private val resourceLoader: ResourceLoader,
    @Value("\${app.zone-default-file:classpath:defaults/zones.txt}")
    private val defaultFile: String
) {
    private val log = LoggerFactory.getLogger(ZoneDefaultDataLoader::class.java)

    @Bean
    fun loadDefaultZonesOnStartup(): ApplicationRunner = ApplicationRunner {
        val resource = resourceLoader.getResource(defaultFile)
        if (!resource.exists()) {
            log.info("Zone default file not found at {}", defaultFile)
            return@ApplicationRunner
        }

        val lines = resource.inputStream.bufferedReader().use { it.readLines() }
        var inserted = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split("|", limit = 2).map { it.trim() }
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                log.warn("Skip invalid zone default line: {}", raw)
                continue
            }

            val code = parts[0]
            val name = parts[1]
            if (!zoneRepository.existsByCodeIgnoreCase(code)) {
                zoneRepository.save(Zone(code = code, name = name))
                inserted++
            }
        }
        log.info("Zone default seed complete. Inserted {} record(s).", inserted)
    }
}
