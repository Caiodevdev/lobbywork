package org.caique.lobbypluginv1.tagmanager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TagManager {

    private val plugin = Lobbypluginv1.instance
    private val database = TagDatabase()

    private val playerTags = ConcurrentHashMap<UUID, Tag>()

    fun initialize() {
        database.connect().thenAccept { success ->
            if (!success) {
                plugin.logger.severe("Falha ao conectar com database de tags!")
            }
        }
    }

    fun loadPlayerTag(player: Player) {
        val uuid = player.uniqueId
        plugin.logger.info("Carregando tag para ${player.name} (${uuid})")

        database.hasPlayerRecord(uuid).thenAccept { hasRecord ->
            plugin.logger.info("Player ${player.name} tem registro: $hasRecord")

            if (!hasRecord) {
                val defaultTag = TagRegistry.getDefaultTag()
                playerTags[uuid] = defaultTag
                database.setPlayerTag(uuid, defaultTag.id)

                plugin.logger.info("Tag padrão '${defaultTag.id}' definida para ${player.name}")

                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (player.isOnline) {
                        plugin.logger.info("Notificando ScoreboardManager sobre nova tag...")
                        notifyScoreboardManager(player)
                    }
                }, 5L)
            } else {
                database.getPlayerTag(uuid).thenAccept { tagId ->
                    val tag = TagRegistry.getTag(tagId) ?: TagRegistry.getDefaultTag()
                    playerTags[uuid] = tag

                    plugin.logger.info("Tag '${tag.id}' carregada para ${player.name}")

                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        if (player.isOnline) {
                            plugin.logger.info("Notificando ScoreboardManager sobre tag carregada...")
                            notifyScoreboardManager(player)
                        }
                    }, 5L)
                }
            }
        }
    }

    fun setPlayerTag(player: Player, tagId: String): Boolean {
        val tag = TagRegistry.getTag(tagId) ?: return false

        if (!player.hasPermission(tag.permission) && !player.isOp) {
            TagUtils.sendErrorMessage(player, "Você não tem permissão para usar esta tag!")
            return false
        }

        val uuid = player.uniqueId
        val oldTag = playerTags[uuid]
        playerTags[uuid] = tag

        notifyScoreboardManager(player)

        database.setPlayerTag(uuid, tagId).thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    TagUtils.sendSuccessMessage(player, "Tag ${tag.getFormattedTag()} §aequipada com sucesso!")

                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        notifyScoreboardManager(player)
                    }, 5L)

                    if (tag.id != "membro") {
                        TagUtils.sendInfoMessage(player, "Símbolo §l★ §eadicionado em cima da sua cabeça!")
                    }
                } else {

                    if (oldTag != null) {
                        playerTags[uuid] = oldTag
                        notifyScoreboardManager(player)
                    }
                    TagUtils.sendErrorMessage(player, "Erro ao salvar sua tag!")
                }
            })
        }

        return true
    }

    private fun notifyScoreboardManager(player: Player) {
        try {
            val scoreboardManager = Lobbypluginv1.getScoreboardManager()
            scoreboardManager.onPlayerTagChanged(player)
        } catch (e: Exception) {
            plugin.logger.warning("Erro ao notificar ScoreboardManager para ${player.name}: ${e.message}")
        }
    }

    fun getPlayerTag(uuid: UUID): Tag {
        return playerTags[uuid] ?: TagRegistry.getDefaultTag()
    }

    fun getAvailableTags(player: Player): List<Tag> {
        val allTags = TagRegistry.getAllTags()

        return if (player.isOp) {
            allTags
        } else {
            allTags.filter { player.hasPermission(it.permission) }
        }
    }

    fun showTagsList(player: Player) {
        val availableTags = getAvailableTags(player)
        val currentTag = getPlayerTag(player.uniqueId)

        if (availableTags.isEmpty()) {
            TagUtils.sendErrorMessage(player, "Você não possui nenhuma tag disponível!")
            return
        }

        player.sendMessage("")
        player.sendMessage("§6§lSUAS TAGS DISPONÍVEIS")
        player.sendMessage("")

        availableTags.forEach { tag ->
            val isCurrentTag = tag.id == currentTag.id
            val status = if (isCurrentTag) "§a✓ EQUIPADA" else "§7Clique para equipar"

            val message = if (isCurrentTag) {
                "§f● ${tag.getFormattedTag()} §7- $status"
            } else {
                "§f● ${tag.getFormattedTag()} §7- $status"
            }

            player.sendMessage(message)
        }

        player.sendMessage("")
        player.sendMessage("§7Use §f/tag set <id> §7para equipar uma tag")
        player.sendMessage("§7IDs disponíveis: §f${availableTags.joinToString(", ") { it.id }}")
        player.sendMessage("")

        TagUtils.playNotificationSound(player)
    }

    fun formatChatMessage(player: Player, message: String): String {
        val tag = getPlayerTag(player.uniqueId)
        return "${tag.getFormattedTag()} §f${player.name}§7: §7$message"
    }

    fun unloadPlayerData(uuid: UUID) {
        playerTags.remove(uuid)
    }

    fun reloadPlayerTags() {
        for (player in Bukkit.getOnlinePlayers()) {
            loadPlayerTag(player)
        }
    }

    fun shutdown() {
        playerTags.clear()
        database.disconnect()
    }
}