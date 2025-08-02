package org.caique.lobbypluginv1.scoreboard

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ScoreboardManager {

    private val plugin = Lobbypluginv1.instance
    private val playerScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private val playerObjectives = ConcurrentHashMap<UUID, Objective>()
    private var ticks = 0

    fun initialize() {
        startUpdateTask()
        plugin.logger.info("Sistema de scoreboard inicializado!")
    }

    fun createScoreboard(player: Player) {
        val manager = Bukkit.getScoreboardManager() ?: return
        val scoreboard = manager.newScoreboard
        val objective = scoreboard.registerNewObjective("lobby", "dummy", "§6§lMineFrex")

        objective.displaySlot = DisplaySlot.SIDEBAR

        playerScoreboards[player.uniqueId] = scoreboard
        playerObjectives[player.uniqueId] = objective
        player.scoreboard = scoreboard

        updateScoreboard(player)
    }

    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        playerObjectives.remove(player.uniqueId)

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
            if (player.isOnline && playerScoreboards.containsKey(player.uniqueId)) {
                updateScoreboard(player)
            }
        }
    }

    fun updatePlayerAuth(player: Player) {
        if (player.isOnline && playerScoreboards.containsKey(player.uniqueId)) {
            updateScoreboard(player)
        }
    }

    fun shutdown() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline) {
                removeScoreboard(player)
            }
        }
        playerScoreboards.clear()
        playerObjectives.clear()
    }
}