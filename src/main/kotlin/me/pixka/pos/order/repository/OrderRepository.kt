package me.pixka.pos.order.repository

import me.pixka.pos.order.model.PosOrder
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<PosOrder, Long> {
    fun findByOrderNoContainingIgnoreCaseOrderByOrderNoAsc(orderNo: String): List<PosOrder>
    fun countByOrderNoStartingWith(prefix: String): Long
    fun existsByOrderNo(orderNo: String): Boolean
}
