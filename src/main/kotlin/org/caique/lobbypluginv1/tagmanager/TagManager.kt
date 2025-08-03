package org.caique.lobbypluginv1.tagmanager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TagManager {

    private val plugin = Lobbypluginv1.instance
    private val database = TagDatabase()
    private val nametagManager = PacketNametagManager()

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

        // Configura scoreboard primeiro
        nametagManager.setupPlayerScoreboard(player)

        database.hasPlayerRecord(uuid).thenAccept { hasRecord ->
            if (!hasRecord) {
                val defaultTag = TagRegistry.getDefaultTag()
                playerTags[uuid] = defaultTag
                database.setPlayerTag(uuid, defaultTag.id)

                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (player.isOnline) {
                        updatePlayerDisplays(player)
                    }
                }, 5L)
            } else {
                database.getPlayerTag(uuid).thenAccept { tagId ->
                    val tag = TagRegistry.getTag(tagId) ?: TagRegistry.getDefaultTag()
                    playerTags[uuid] = tag

                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        if (player.isOnline) {
                            updatePlayerDisplays(player)
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

        // Atualiza imediatamente
        updatePlayerDisplays(player)

        database.setPlayerTag(uuid, tagId).thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    TagUtils.sendSuccessMessage(player, "Tag ${tag.getFormattedTag()} §aequipada com sucesso!")

                    // Atualiza para outros jogadores com delay pequeno
                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        updateOtherPlayersView(player)
                    }, 3L)

                    // Notifica sobre o símbolo especial se não for tag padrão
                    if (tag.id != "membro") {
                        TagUtils.sendInfoMessage(player, "Símbolo §l★ §eadicionado em cima da sua cabeça!")
                    }
                } else {
                    // Reverte se deu erro
                    if (oldTag != null) {
                        playerTags[uuid] = oldTag
                        updatePlayerDisplays(player)
                    }
                    TagUtils.sendErrorMessage(player, "Erro ao salvar sua tag!")
                }
            })
        }

        return true
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

    private fun updatePlayerDisplays(player: Player) {
        updatePlayerNameTag(player)
        updatePlayerTablistName(player)
        updatePlayerDisplayName(player)
    }

    private fun updatePlayerNameTag(player: Player) {
        val tag = getPlayerTag(player.uniqueId)
        val nameTag = if (tag.id != "membro") {
            "§l★ ${tag.getFormattedTag()} §f${player.name}"
        } else {
            "§f${player.name}"
        }

        // Múltiplas tentativas para compatibilidade
        try {
            // Método 1: CustomName com visibility
            player.setCustomName(nameTag)
            player.setCustomNameVisible(true)

            // Método 2: Usando reflection para forçar atualização
            val craftPlayer = player.javaClass
            val getHandle = craftPlayer.getMethod("getHandle")
            val entityPlayer = getHandle.invoke(player)

            // Força refresh da entidade
            for (other in Bukkit.getOnlinePlayers()) {
                if (other != player && other.canSee(player)) {
                    other.hidePlayer(plugin, player)
                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        if (player.isOnline && other.isOnline) {
                            other.showPlayer(plugin, player)
                        }
                    }, 1L)
                }
            }

        } catch (e: Exception) {
            // Método 3: Fallback usando comando
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "data modify entity ${player.uniqueId} CustomName set value '{\"text\":\"$nameTag\"}'"
                )
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "data modify entity ${player.uniqueId} CustomNameVisible set value 1b"
                )
            } catch (ex: Exception) {
                // Método 4: Usando teams (scoreboard)
                updateNameTagWithTeams(player, nameTag)
            }
        }
    }

    private fun updateNameTagWithTeams(player: Player, nameTag: String) {
        try {
            val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard ?: return
            val teamName = "tag_${player.name.lowercase()}"

            // Remove team existente se houver
            scoreboard.getTeam(teamName)?.unregister()

            // Cria novo team
            val team = scoreboard.registerNewTeam(teamName)
            team.setPrefix(nameTag.replace(player.name, ""))
            team.addEntry(player.name)

            // Aplica para todos os jogadores
            for (other in Bukkit.getOnlinePlayers()) {
                other.scoreboard = scoreboard
            }

        } catch (e: Exception) {
            plugin.logger.warning("Erro ao atualizar nametag para ${player.name}: ${e.message}")
        }
    }

    private fun updatePlayerTablistName(player: Player) {
        val tag = getPlayerTag(player.uniqueId)
        val tabName = "${tag.getFormattedTag()} §f${player.name}"

        // Spigot/Paper suporta nomes mais longos na tablist
        try {
            player.setPlayerListName(tabName)
        } catch (e: Exception) {
            // Fallback para nome mais curto se der erro
            if (tabName.length > 16) {
                player.setPlayerListName("${tag.color}${player.name}")
            }
        }
    }

    private fun updatePlayerDisplayName(player: Player) {
        val tag = getPlayerTag(player.uniqueId)
        val displayName = "${tag.getFormattedTag()} §f${player.name}"
        player.setDisplayName(displayName)
    }

    private fun updateOtherPlayersView(player: Player) {
        // Atualiza displays
        updatePlayerDisplays(player)

        // Força refresh do scoreboard
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            nametagManager.refreshAllNametags()
        }, 2L)
    }

    fun formatChatMessage(player: Player, message: String): String {
        val tag = getPlayerTag(player.uniqueId)
        return "${tag.getFormattedTag()} §f${player.name}§7: §7$message"
    }

    fun unloadPlayerData(uuid: UUID) {
        playerTags.remove(uuid)

        val player = Bukkit.getPlayer(uuid)
        if (player != null) {
            nametagManager.cleanupPlayer(player)
        }
    }

    fun reloadPlayerTags() {
        for (player in Bukkit.getOnlinePlayers()) {
            loadPlayerTag(player)
        }
    }

    fun shutdown() {
        nametagManager.cleanup()
        playerTags.clear()
        database.disconnect()
    }
}