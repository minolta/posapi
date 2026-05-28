package me.pixka.pos.table.service

import me.pixka.pos.table.model.PosTable
import me.pixka.pos.table.repository.TableRepository
import me.pixka.pos.zone.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.ResourceLoader

@Configuration
class TableDefaultDataLoader(
    private val tableRepository: TableRepository,
    private val zoneRepository: ZoneRepository,
    private val resourceLoader: ResourceLoader,
    @Value("\${app.default-data-load.enabled:false}")
    private val defaultDataLoadEnabled: Boolean,
    @Value("\${app.table-default-file:classpath:defaults/tables.txt}")
    private val defaultFile: String
) {
    private val log = LoggerFactory.getLogger(TableDefaultDataLoader::class.java)

    @Bean
    @Order(30)
    fun loadDefaultTablesOnStartup(): ApplicationRunner = ApplicationRunner {
        if (!defaultDataLoadEnabled) {
            log.info("Default data load disabled; skip table seed (app.default-data-load.enabled=false).")
            return@ApplicationRunner
        }
        val resource = resourceLoader.getResource(defaultFile)
        if (!resource.exists()) {
            log.info("Table default file not found at {}", defaultFile)
            return@ApplicationRunner
        }

        val lines = resource.inputStream.bufferedReader().use { it.readLines() }
        var inserted = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split("|", limit = 3).map { it.trim() }
            if (parts.size != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
                log.warn("Skip invalid table default line: {}", raw)
                continue
            }

            val code = parts[0]
            val basePrice = parts[1].toDoubleOrNull()
            val zoneCode = parts[2]
            if (basePrice == null) {
                log.warn("Skip table with invalid basePrice in line: {}", raw)
                continue
            }

            if (tableRepository.existsByCodeIgnoreCase(code)) continue

            val zone = zoneRepository.findByCodeIgnoreCase(zoneCode)
            if (zone == null) {
                log.warn("Skip table {} because zone code {} not found", code, zoneCode)
                continue
            }

            tableRepository.save(
                PosTable(
                    code = code,
                    basePrice = basePrice,
                    zone = zone
                )
            )
            inserted++
        }
        log.info("Table default seed complete. Inserted {} record(s).", inserted)
    }
}
