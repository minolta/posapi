package me.pixka.pos.printer.api

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PrinterRequest(
    @field:NotBlank(message = "code is required")
    val code: String,

    @field:NotBlank(message = "name is required")
    val name: String,

    @field:NotBlank(message = "host is required")
    val host: String,

    @field:NotNull(message = "port is required")
    @field:Min(value = 1, message = "port must be >= 1")
    @field:Max(value = 65535, message = "port must be <= 65535")
    val port: Int,

    @field:NotNull(message = "enabled is required")
    val enabled: Boolean,

    @field:NotNull(message = "connectTimeoutMs is required")
    @field:Min(value = 100, message = "connectTimeoutMs must be >= 100")
    val connectTimeoutMs: Int,

    @field:NotNull(message = "readTimeoutMs is required")
    @field:Min(value = 100, message = "readTimeoutMs must be >= 100")
    val readTimeoutMs: Int,

    @field:NotNull(message = "version is required")
    @field:Min(value = 0, message = "version must be >= 0")
    val version: Int,
)
