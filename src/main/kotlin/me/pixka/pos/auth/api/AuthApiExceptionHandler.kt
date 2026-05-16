package me.pixka.pos.auth.api

import me.pixka.pos.auth.exception.InvalidCredentialsException
import me.pixka.pos.auth.exception.UserAlreadyExistsException
import me.pixka.pos.auth.exception.UserNotFoundByUsernameException
import me.pixka.pos.auth.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AuthController::class, UserController::class])
class AuthApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Invalid request"))
    @ExceptionHandler(InvalidCredentialsException::class, BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidCredentials(ex: Exception): Map<String, String> =
        mapOf("message" to (ex.message ?: "Invalid username or password"))

    @ExceptionHandler(UserAlreadyExistsException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): Map<String, String> =
        mapOf("message" to ex.message.orEmpty())

    @ExceptionHandler(UserNotFoundException::class, UserNotFoundByUsernameException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFound(ex: RuntimeException): Map<String, String> =
        mapOf("message" to ex.message.orEmpty())

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDenied(ex: AccessDeniedException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Access denied"))
}
