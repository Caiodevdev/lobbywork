package org.caique.lobbypluginv1.chatmanager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1

class ChatManager {

    private val plugin = Lobbypluginv1.instance

    fun initialize() {
        plugin.logger.info("Sistema de chat personalizado inicializado!")
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
        return authManager.isPlayerAuthenticated(player.uniqueId)
    }

    private fun broadcastMessage(message: String) {
        for (player in Bukkit.getOnlinePlayers()) {
            player.sendMessage(message)
        }

        plugin.logger.info(ChatFormatter.stripColors(message))
    }

    fun shutdown() {
        plugin.logger.info("Sistema de chat finalizado!")
    }
}