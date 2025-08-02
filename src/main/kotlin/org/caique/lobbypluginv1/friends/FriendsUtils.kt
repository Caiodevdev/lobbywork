package org.caique.lobbypluginv1.friends

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.text.SimpleDateFormat

object FriendsUtils {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")

    fun sendFriendRequest(receiver: Player, senderName: String, senderUuid: String) {
        receiver.sendMessage("")
        receiver.sendMessage("§b§lSOLICITAÇÃO DE AMIZADE")
        receiver.sendMessage("")
        receiver.sendMessage("§f$senderName §7te enviou uma solicitação de amizade.")
        receiver.sendMessage("")

        val acceptButton = TextComponent("§a§lACEITAR")
        acceptButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/amigos aceitar $senderName")
        acceptButton.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("§aClique para aceitar a solicitação").create())

        val rejectButton = TextComponent("§c§lRECUSAR")
        rejectButton.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/amigos recusar $senderName")
        rejectButton.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("§cClique para recusar a solicitação").create())

        val message = TextComponent("Clique para ")
        message.addExtra(acceptButton)
        message.addExtra(TextComponent(" §7ou para "))
        message.addExtra(rejectButton)

        receiver.spigot().sendMessage(message)
        receiver.sendMessage("")

        receiver.playSound(receiver.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
    }

    fun formatFriendsList(friends: List<FriendData>): List<String> {
        if (friends.isEmpty()) {
            return listOf(
                "",
                "§7Sua lista de amigos está vazia.",
                "§7Use §f/amigos add <nick> §7para adicionar amigos!",
                ""
            )
        }

        val messages = mutableListOf<String>()
        messages.add("")
        messages.add("§b§lSEUS AMIGOS §7(${friends.size})")
        messages.add("")

        friends.sortedWith(compareBy<FriendData> { !it.isOnline }.thenBy { it.username.lowercase() }).forEach { friend ->
            val status = if (friend.isOnline) {
                if (friend.server == "Lobby") "§a● Online" else "§a● ${friend.server}"
            } else {
                "§c● Offline"
            }

            val addedDate = dateFormat.format(friend.addedAt)
            messages.add("§f${friend.username} $status §8- §7Desde $addedDate")
        }

        messages.add("")
        messages.add("§7Use §f/amigos remove <nick> §7para remover um amigo.")
        messages.add("")

        return messages
    }

    fun getServerName(): String {
        return "Lobby"
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

    fun isValidUsername(username: String): Boolean {
        return username.matches(Regex("^[a-zA-Z0-9_]{3,16}$"))
    }

    fun getPlayerUuid(username: String): String? {
        val player = Bukkit.getPlayer(username)
        return player?.uniqueId?.toString()
    }

    fun isPlayerOnline(username: String): Boolean {
        return Bukkit.getPlayer(username) != null
    }
}