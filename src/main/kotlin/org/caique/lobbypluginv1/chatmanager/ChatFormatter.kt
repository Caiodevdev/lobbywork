package org.caique.lobbypluginv1.chatmanager

import org.bukkit.entity.Player

object ChatFormatter {

    fun formatMessage(player: Player, message: String): String {
        val playerName = player.name
        val formattedMessage = "§7$message"

        return "§f$playerName: $formattedMessage"
    }

    fun stripColors(message: String): String {
        return message.replace(Regex("§[0-9a-fk-or]"), "")
    }
}