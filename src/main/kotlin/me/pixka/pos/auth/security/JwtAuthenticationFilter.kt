package me.pixka.pos.auth.security

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.pixka.pos.auth.service.AppUserDetailsService
import me.pixka.pos.auth.service.JwtService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: AppUserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ", ignoreCase = true)) {
            val token = header.substring(7).trim()
            if (token.isNotEmpty() && SecurityContextHolder.getContext().authentication == null) {
                try {
                    val claims = jwtService.parseClaims(token)
                    val username = jwtService.usernameFromClaims(claims)
                    val userDetails = userDetailsService.loadUserByUsername(username)
                    val auth = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities,
                    )
                    auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = auth
                } catch (_: JwtException) {
                    SecurityContextHolder.clearContext()
                } catch (_: IllegalArgumentException) {
                    SecurityContextHolder.clearContext()
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
