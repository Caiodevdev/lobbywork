package org.caique.lobbypluginv1.auth

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

/**
 * Extensão para enviar mensagens na ActionBar
 */
fun Player.sendActionBar(message: String) {
    this.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message))
}