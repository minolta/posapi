package me.pixka.pos.auth.exception

class UserNotFoundException(id: Long) : RuntimeException("User id=$id not found")

class UserNotFoundByUsernameException(username: String) : RuntimeException("User '$username' not found")
