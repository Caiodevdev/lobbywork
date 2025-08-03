package org.caique.lobbypluginv1.scoreboard

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScoreboardManager {

    private val plugin = Lobbypluginv1.instance
    private val playerObjectives = ConcurrentHashMap<UUID, Objective>()
    private val playerTeams = ConcurrentHashMap<UUID, Team>()
    private var ticks = 0

    private var sharedScoreboard: Scoreboard? = null

    fun initialize() {
        val manager = Bukkit.getScoreboardManager()
        if (manager != null) {
            sharedScoreboard = manager.newScoreboard
            plugin.logger.info("Scoreboard compartilhado criado!")
        } else {
            plugin.logger.severe("ScoreboardManager não disponível!")
        }

        startUpdateTask()
        plugin.logger.info("Sistema de scoreboard integrado inicializado!")
    }

    fun createScoreboard(player: Player) {
        val scoreboard = sharedScoreboard ?: return

        val objectiveName = "lobby_${player.name.lowercase()}"

        val existingObjective = scoreboard.getObjective(objectiveName)
        existingObjective?.unregister()

        val objective = scoreboard.registerNewObjective(objectiveName, "dummy", "§6§lMineFrex")
        objective.displaySlot = DisplaySlot.SIDEBAR

        playerObjectives[player.uniqueId] = objective
        player.scoreboard = scoreboard

        plugin.logger.info("Scoreboard criado para ${player.name} usando scoreboard compartilhado")

        updateScoreboard(player)

        loadPlayerTag(player)
    }

    fun removeScoreboard(player: Player) {
        removePlayerTeam(player)

        val objective = playerObjectives[player.uniqueId]
        if (objective != null) {
            try {
                objective.unregister()
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao remover objetivo para ${player.name}: ${e.message}")
            }
        }

        playerObjectives.remove(player.uniqueId)
        playerTeams.remove(player.uniqueId)

        val manager = Bukkit.getScoreboardManager()
        manager?.let {
            player.scoreboard = it.mainScoreboard
        }
    }

    fun updateScoreboard(player: Player) {
        val objective = playerObjectives[player.uniqueId] ?: return

        clearScoreboard(objective)

        val animatedTitle = AnimationUtil.getAnimatedTitle(ticks)
        objective.displayName = animatedTitle

        val lines = getScoreboardLines(player)

        for (i in lines.indices) {
            val line = lines[i]
            val score = lines.size - i
            objective.getScore(line).score = score
        }
    }

    private fun getScoreboardLines(player: Player): List<String> {
        val playerCount = AnimationUtil.getPlayerCount()
        val authStatus = AnimationUtil.getAuthStatus(player.uniqueId)
        val animatedWebsite = AnimationUtil.getAnimatedWebsite(ticks)

        return listOf(
            " ",
            "§fJogadores: §b$playerCount",
            "§fStatus: $authStatus",
            "  ",
            "§fVersão: §b1.0",
            "   ",
            animatedWebsite
        )
    }

    private fun clearScoreboard(objective: Objective) {
        val scoreboard = objective.scoreboard
        if (scoreboard != null) {
            for (entry in scoreboard.getEntries()) {
                scoreboard.resetScores(entry)
            }
        }
    }

    private fun startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            ticks++
            updateAllScoreboards()
        }, 0L, 4L)
    }

    private fun updateAllScoreboards() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline && playerObjectives.containsKey(player.uniqueId)) {
                updateScoreboard(player)
            }
        }
    }

    fun updatePlayerAuth(player: Player) {
        if (player.isOnline && playerObjectives.containsKey(player.uniqueId)) {
            updateScoreboard(player)
        }
    }

    fun loadPlayerTag(player: Player) {
        val uuid = player.uniqueId
        val tagManager = Lobbypluginv1.getTagManager()

        plugin.logger.info("Carregando tag no scoreboard integrado para ${player.name}")

        // Aguarda um pouco para garantir que a tag foi carregada
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            if (player.isOnline) {
                val tag = tagManager.getPlayerTag(uuid)
                updatePlayerNametag(player, tag)
            }
        }, 20L)
    }

    fun updatePlayerNametag(player: Player, tag: org.caique.lobbypluginv1.tagmanager.Tag) {
        val scoreboard = sharedScoreboard ?: return

        plugin.logger.info("Atualizando nametag no scoreboard compartilhado para ${player.name} com tag ${tag.id}")

        removePlayerTeam(player)

        createPlayerTeam(player, scoreboard, tag)

        updatePlayerDisplays(player, tag)
    }

    private fun createPlayerTeam(player: Player, scoreboard: Scoreboard, tag: org.caique.lobbypluginv1.tagmanager.Tag) {
        try {
            val teamName = "tag_${player.name.lowercase()}"

            val existingTeam = scoreboard.getTeam(teamName)
            existingTeam?.unregister()

            val team = scoreboard.registerNewTeam(teamName)

            val prefix = if (tag.id != "membro") {
                "§l★ ${tag.getFormattedTag()} §f"
            } else {
                ""
            }

            team.setPrefix(prefix)
            team.setSuffix("")
            team.setCanSeeFriendlyInvisibles(false)
            team.setAllowFriendlyFire(true)

            team.addEntry(player.name)

            playerTeams[player.uniqueId] = team

            plugin.logger.info("Team '${teamName}' criado com prefix '${prefix}' para ${player.name}")


            refreshPlayerForOthers(player)

        } catch (e: Exception) {
            plugin.logger.severe("Erro ao criar team integrado para ${player.name}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun removePlayerTeam(player: Player) {
        val team = playerTeams[player.uniqueId]
        if (team != null) {
            try {
                team.removeEntry(player.name)
                if (team.entries.isEmpty()) {
                    team.unregister()
                }
                playerTeams.remove(player.uniqueId)
                plugin.logger.info("Team removido para ${player.name}")
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao remover team para ${player.name}: ${e.message}")
            }
        }
    }

    private fun updatePlayerDisplays(player: Player, tag: org.caique.lobbypluginv1.tagmanager.Tag) {
        try {
            val nameTag = if (tag.id != "membro") {
                "§l★ ${tag.getFormattedTag()} §f${player.name}"
            } else {
                player.name
            }

            player.setCustomName(nameTag)
            player.setCustomNameVisible(true)

        } catch (e: Exception) {
            plugin.logger.warning("Erro ao definir CustomName para ${player.name}: ${e.message}")
        }

        try {
            val tabName = "${tag.getFormattedTag()} §f${player.name}"
            player.setPlayerListName(tabName)
        } catch (e: Exception) {
            val fallbackName = "${tag.color}${player.name}"
            if (fallbackName.length <= 16) {
                player.setPlayerListName(fallbackName)
            } else {
                player.setPlayerListName(player.name)
            }
        }

        try {
            val displayName = "${tag.getFormattedTag()} §f${player.name}"
            player.setDisplayName(displayName)
        } catch (e: Exception) {
            plugin.logger.warning("Erro ao definir DisplayName para ${player.name}: ${e.message}")
        }
    }

    private fun refreshPlayerForOthers(player: Player) {
        for (other in Bukkit.getOnlinePlayers()) {
            if (other != player && other.canSee(player)) {
                try {
                    other.hidePlayer(plugin, player)

                    Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                        if (player.isOnline && other.isOnline) {
                            other.showPlayer(plugin, player)
                        }
                    }, 2L)
                } catch (e: Exception) {
                }
            }
        }
    }

    fun onPlayerTagChanged(player: Player) {
        if (player.isOnline && playerObjectives.containsKey(player.uniqueId)) {
            loadPlayerTag(player)
        }
    }

    fun shutdown() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline) {
                removeScoreboard(player)
            }
        }
        playerObjectives.clear()
        playerTeams.clear()
        sharedScoreboard = null
    }
}