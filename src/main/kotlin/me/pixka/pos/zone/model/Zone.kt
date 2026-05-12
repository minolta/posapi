package me.pixka.pos.zone.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "zones")
class Zone(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(nullable = false)
    var name: String = "",

    @field:JsonIgnore
    @Column(name = "picture_ext", length = 10)
    var pictureExtension: String? = null,

    @Version
    @Column(nullable = false)
    var version: Int = 0
) {
    @JsonProperty("pictureUrl")
    fun getPictureUrl(): String? =
        id?.takeIf { !pictureExtension.isNullOrBlank() }?.let { "/api/zones/$it/picture" }
}
