package org.caique.lobbypluginv1.auth

import java.util.*

data class PlayerData(
    val uuid: UUID,
    val username: String,
    val password: String,
    val registeredAt: Date,
    val lastLogin: Date,
    val ipAddress: String
)

data class AuthSession(
    val uuid: UUID,
    val isAuthenticated: Boolean = false,
    val loginAttempts: Int = 0,
    val registrationStartTime: Long = System.currentTimeMillis(),
    val lastAttempt: Long = System.currentTimeMillis()
)