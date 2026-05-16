package me.pixka.pos.food

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Migrates legacy H2 DBs for [Food.blockOrderLine]:
 * - missing column breaks selects;
 * - `ddl-auto=update` can add NOT NULL columns without fixing existing rows, leaving NULL values.
 */
@Configuration
class FoodsSchemaPatch {
    private val log = LoggerFactory.getLogger(FoodsSchemaPatch::class.java)

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun ensureFoodsBlockOrderLineColumn(
        jdbc: JdbcTemplate,
        @Value("\${spring.datasource.url}") datasourceUrl: String,
    ): ApplicationRunner = ApplicationRunner {
        if (!datasourceUrl.contains(":h2:", ignoreCase = true)) {
            return@ApplicationRunner
        }
        jdbc.execute(
            """
            ALTER TABLE foods ADD COLUMN IF NOT EXISTS block_order_line BOOLEAN DEFAULT FALSE
            """.trimIndent(),
        )
        val nulledFilled = jdbc.update("UPDATE foods SET block_order_line = FALSE WHERE block_order_line IS NULL")
        if (nulledFilled > 0) {
            log.info(
                "Set foods.block_order_line=false for {} row(s) that had NULL (legacy schema migration).",
                nulledFilled,
            )
        }
        jdbc.execute("ALTER TABLE foods ALTER COLUMN block_order_line SET DEFAULT FALSE")
        jdbc.execute("ALTER TABLE foods ALTER COLUMN block_order_line SET NOT NULL")
        log.debug("Ensured foods.block_order_line is NOT NULL with default FALSE.")
    }
}
