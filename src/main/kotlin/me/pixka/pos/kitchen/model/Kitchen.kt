package me.pixka.pos.kitchen.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "kitchens")
class Kitchen(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Version
    @Column(nullable = false)
    var version: Int = 0
)
