package me.pixka.pos.order.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import me.pixka.pos.order.model.OrderLineStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private class OrderLineStatusDeserializer :
    StdDeserializer<OrderLineStatus>(OrderLineStatus::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OrderLineStatus {
        val raw = p.valueAsString ?: return OrderLineStatus.WAIT
        if (raw.isBlank()) {
            return OrderLineStatus.WAIT
        }
        return OrderLineStatus.fromWire(raw)
    }
}

@Configuration
class OrderLineStatusJacksonConfig {
    @Bean
    fun orderLineStatusJacksonModule(): SimpleModule =
        SimpleModule("order-line-status").apply {
            addDeserializer(OrderLineStatus::class.java, OrderLineStatusDeserializer())
        }
}
