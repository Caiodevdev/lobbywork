package org.caique.lobbypluginv1.chatmanager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatManager {

    private val plugin = Lobbypluginv1.instance
    private val lastMuteNotification = ConcurrentHashMap<UUID, Long>()
    private val NOTIFICATION_COOLDOWN = 2000L

    fun initialize() {
    }

    fun processMessage(player: Player, message: String): Boolean {
        if (!canPlayerChat(player)) {
            return false
        }

        val formattedMessage = ChatFormatter.formatMessage(player, message)
        broadcastMessage(formattedMessage)

        return true
    }

    private fun canPlayerChat(player: Player): Boolean {
        val authManager = Lobbypluginv1.getAuthManager()
        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            return false
        }

        val punishManager = Lobbypluginv1.getPunishManager()
        if (punishManager.isPlayerMuted(player.uniqueId)) {
            val now = System.currentTimeMillis()
            val lastNotification = lastMuteNotification[player.uniqueId] ?: 0L

            if (now - lastNotification > NOTIFICATION_COOLDOWN) {
                val mute = punishManager.getActiveMute(player.uniqueId)

                if (mute != null) {

                    if (mute.expiresAt > 0) {
                        val timeRemaining = mute.expiresAt - now
                        if (timeRemaining > 0) {
                            val hours = timeRemaining / (1000 * 60 * 60)
                            val minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60)

                        }
                    } else if (mute.expiresAt == -1L) {
                    }
                }

                lastMuteNotification[player.uniqueId] = now
            }

            return false
        }

        return true
    }

    private fun broadcastMessage(message: String) {
        for (player in Bukkit.getOnlinePlayers()) {
            player.sendMessage(message)
        }

        plugin.logger.info(ChatFormatter.stripColors(message))
    }

    fun shutdown() {
    }
}