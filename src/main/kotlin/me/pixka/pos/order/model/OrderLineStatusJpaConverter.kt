package me.pixka.pos.order.model

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Maps `order_lines.status` VARCHAR ↔ enum. Imports legacy `CANSHIPNEW` rows as [OrderLineStatus.FINISH_COOKING].
 */
@Converter(autoApply = true)
class OrderLineStatusJpaConverter : AttributeConverter<OrderLineStatus, String?> {
    override fun convertToDatabaseColumn(attribute: OrderLineStatus?): String? =
        attribute?.name

    override fun convertToEntityAttribute(dbData: String?): OrderLineStatus =
        OrderLineStatus.fromWireOrWait(dbData)
}
