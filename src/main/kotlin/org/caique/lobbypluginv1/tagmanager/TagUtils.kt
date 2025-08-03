package org.caique.lobbypluginv1.tagmanager

import org.bukkit.Sound
import org.bukkit.entity.Player

object TagUtils {

    fun sendSuccessMessage(player: Player, message: String) {
        player.sendMessage("§a✓ $message")
        playSuccessSound(player)
    }

    fun sendErrorMessage(player: Player, message: String) {
        player.sendMessage("§c✗ $message")
        playErrorSound(player)
    }

    fun sendInfoMessage(player: Player, message: String) {
        player.sendMessage("§e! $message")
        playNotificationSound(player)
    }

    fun playSuccessSound(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f)
    }

    fun playErrorSound(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
    }

    fun playNotificationSound(player: Player) {
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
    }

    fun isValidTagId(tagId: String): Boolean {
        return TagRegistry.getTag(tagId) != null
    }

    fun formatTagInfo(tag: Tag, isEquipped: Boolean, hasPermission: Boolean): String {
        val prefix = when {
            isEquipped -> "§a✓"
            hasPermission -> "§f●"
            else -> "§c✗"
        }

        val status = when {
            isEquipped -> "§a(EQUIPADA)"
            hasPermission -> "§7(Disponível)"
            else -> "§c(Sem permissão)"
        }

        return "$prefix ${tag.getFormattedTag()} $status"
    }

    fun getTagPreview(tag: Tag, playerName: String): String {
        val chatPreview = "${tag.getFormattedTag()} §f$playerName"
        val nametagPreview = if (tag.id != "membro") {
            "§l★ ${tag.getFormattedTag()} §f$playerName"
        } else {
            "§f$playerName"
        }

        return "§7Chat: $chatPreview\n§7Nametag: $nametagPreview\n§7Tablist: $chatPreview"
    }
}