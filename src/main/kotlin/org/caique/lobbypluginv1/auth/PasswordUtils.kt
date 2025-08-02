package org.caique.lobbypluginv1.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

object PasswordUtils {

    private val secureRandom = SecureRandom()

    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val hashedPassword = md.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hashedPassword)
    }

    fun generateSalt(): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hashPasswordWithSalt(password: String): String {
        val salt = generateSalt()
        val hashedPassword = hashPassword(password, salt)
        return "$salt:$hashedPassword"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false

            val salt = parts[0]
            val hash = parts[1]
            val hashedInput = hashPassword(password, salt)

            hashedInput == hash
        } catch (e: Exception) {
            false
        }
    }

    fun isPasswordStrong(password: String): Boolean {

        if (password.length < 6) return false


        val weakPasswords = listOf(
            "123456", "1234", "12345", "password", "admin",
            "qwerty", "abc123", "111111", "000000", "senha"
        )

        if (weakPasswords.any { it.equals(password, ignoreCase = true) }) {
            return false
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        return hasLetter && hasDigit
    }

    fun getPasswordStrength(password: String): PasswordStrength {
        return when {
            password.length < 6 -> PasswordStrength.WEAK
            !isPasswordStrong(password) -> PasswordStrength.WEAK
            password.length >= 8 && password.any { it.isUpperCase() } &&
                    password.any { it.isLowerCase() } && password.any { !it.isLetterOrDigit() } -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
    }
}

enum class PasswordStrength(val displayName: String, val color: String) {
    WEAK("Fraca", "§c"),
    MEDIUM("Média", "§e"),
    STRONG("Forte", "§a")
}