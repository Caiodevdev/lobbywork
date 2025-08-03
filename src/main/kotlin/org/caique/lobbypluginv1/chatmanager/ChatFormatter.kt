package org.caique.lobbypluginv1.chatmanager

import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1

object ChatFormatter {

    fun formatMessage(player: Player, message: String): String {
        val tagManager = Lobbypluginv1.instance.getTagManager()
        return tagManager.formatChatMessage(player, message)
    }

    fun stripColors(message: String): String {
        return message.replace(Regex("§[0-9a-fk-or]"), "")
    }
}