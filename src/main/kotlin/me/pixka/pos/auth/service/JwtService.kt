package me.pixka.pos.auth.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import me.pixka.pos.auth.model.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") val expirationMs: Long,
) {
    /** SHA-256 of the configured secret → 256-bit key (RFC 7518 HS256 minimum). */
    private val key: SecretKey = Keys.hmacShaKeyFor(
        MessageDigest.getInstance("SHA-256")
            .digest(secret.toByteArray(StandardCharsets.UTF_8)),
    )

    fun generateToken(user: User): String {
        val now = Instant.now()
        val userId = user.id ?: error("User id required for JWT")
        return Jwts.builder()
            .subject(user.username)
            .claim("uid", userId)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(key)
            .compact()
    }

    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

    fun usernameFromClaims(claims: Claims): String = claims.subject

    fun userIdFromClaims(claims: Claims): Long = claims["uid"].toString().toLong()
}
