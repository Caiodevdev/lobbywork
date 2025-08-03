package org.caique.lobbypluginv1.punish

import java.text.SimpleDateFormat
import java.util.*

data class Punishment(
    var id: Int,
    val playerUUID: UUID,
    val playerName: String,
    val adminUUID: UUID,
    val adminName: String,
    val type: PunishType,
    val reason: String,
    val createdAt: Long,
    val expiresAt: Long,
    var status: PunishStatus
) {
    fun formatCreatedAt(): String {
        val date = Date(createdAt)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm")
        return sdf.format(date)
    }

    fun formatExpiresAt(): String {
        if (expiresAt == -1L) {
            return "Permanente"
        }

        val currentTime = System.currentTimeMillis()
        val remainingTime = expiresAt - currentTime

        if (remainingTime <= 0) {
            return "Expirado"
        }

        val seconds = remainingTime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days dia(s)"
            hours > 0 -> "$hours hora(s)"
            minutes > 0 -> "$minutes minuto(s)"
            else -> "$seconds segundo(s)"
        }
    }
}