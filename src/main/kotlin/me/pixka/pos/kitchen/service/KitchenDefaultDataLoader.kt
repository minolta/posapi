package me.pixka.pos.kitchen.service

import me.pixka.pos.kitchen.model.Kitchen
import me.pixka.pos.kitchen.repository.KitchenRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class KitchenDefaultDataLoader(
    private val kitchenRepository: KitchenRepository,
    private val resourceLoader: ResourceLoader,
    @Value("\${app.kitchen-default-file:classpath:defaults/kitchens.txt}")
    private val kitchenDefaultFile: String
) {
    private val log = LoggerFactory.getLogger(KitchenDefaultDataLoader::class.java)

    @Bean
    fun loadDefaultKitchensOnStartup(): ApplicationRunner = ApplicationRunner {
        val resource = resourceLoader.getResource(kitchenDefaultFile)
        if (!resource.exists()) {
            log.info("Kitchen default file not found at {}", kitchenDefaultFile)
            return@ApplicationRunner
        }

        val lines = resource.inputStream.bufferedReader().use { it.readLines() }
        var inserted = 0
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split("|", limit = 2).map { it.trim() }
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                log.warn("Skip invalid kitchen default line: {}", raw)
                continue
            }

            val code = parts[0]
            val name = parts[1]
            if (!kitchenRepository.existsByCodeIgnoreCase(code)) {
                kitchenRepository.save(Kitchen(code = code, name = name))
                inserted++
            }
        }
        log.info("Kitchen default seed complete. Inserted {} record(s).", inserted)
    }
}
