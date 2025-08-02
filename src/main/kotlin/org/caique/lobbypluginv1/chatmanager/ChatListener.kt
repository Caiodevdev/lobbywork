package org.caique.lobbypluginv1.chatmanager

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val chatManager: ChatManager) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        event.isCancelled = true

        val player = event.player
        val message = event.message

        chatManager.processMessage(player, message)
    }
}