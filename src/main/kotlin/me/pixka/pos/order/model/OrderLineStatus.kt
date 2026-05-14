package me.pixka.pos.order.model

/**
 * Persisted per `order_lines.status`.
 * Matches Angular `OrderLineStatus` (`WAIT` … `FINISH_COOKING` …).
 */
enum class OrderLineStatus {
    WAIT,

    /** Cooked / ready for floor; front-end labels this “Finish cooking”. */
    FINISH_COOKING,

    COMPLETE,
    CANCEL,
    ;

    companion object {
        /**
         * Strict parse for REST JSON bodies. Throws [IllegalArgumentException] when unknown.
         * Accepts legacy alias `CANSHIPNEW` (older clients).
         */
        fun fromWire(value: String): OrderLineStatus {
            val u = value.trim().uppercase()
            return when (u) {
                "WAIT" -> WAIT
                "FINISH_COOKING", "CANSHIPNEW" -> FINISH_COOKING
                "COMPLETE" -> COMPLETE
                "CANCEL" -> CANCEL
                else -> throw IllegalArgumentException("Unknown order line status: $value")
            }
        }

        /** Backup import: blanks / unknown tokens fall back to [WAIT]. */
        fun fromWireOrWait(raw: String?): OrderLineStatus {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) {
                return WAIT
            }
            return try {
                fromWire(trimmed)
            } catch (_: IllegalArgumentException) {
                WAIT
            }
        }
    }
}
