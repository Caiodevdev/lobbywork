package org.caique.lobbypluginv1.tagmanager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PacketNametagManager {

    private val plugin = Lobbypluginv1.instance
    private val teams = ConcurrentHashMap<String, Team>()
    private val playerTeams = ConcurrentHashMap<UUID, String>()

    fun setNametag(player: Player, prefix: String, suffix: String = "") {
        val teamName = "tag_${player.name.lowercase()}"

        removePlayerFromTeam(player)

        val scoreboard = getOrCreateScoreboard()
        var team = scoreboard.getTeam(teamName)

        if (team != null) {
            team.unregister()
        }

        team = scoreboard.registerNewTeam(teamName)

        try {
            team.setPrefix(prefix)
            team.setSuffix(suffix)
            team.setCanSeeFriendlyInvisibles(false)
            team.setAllowFriendlyFire(true)

            team.addEntry(player.name)

            teams[teamName] = team
            playerTeams[player.uniqueId] = teamName

            applyScoreboardToAll(scoreboard)

            plugin.logger.info("Nametag definida para ${player.name}: $prefix${player.name}$suffix")

        } catch (e: Exception) {
            plugin.logger.warning("Erro ao criar team para ${player.name}: ${e.message}")
        }
    }

    fun removePlayerFromTeam(player: Player) {
        val uuid = player.uniqueId
        val teamName = playerTeams[uuid]

        if (teamName != null) {
            val team = teams[teamName]
            team?.let {
                try {
                    it.removeEntry(player.name)
                    if (it.entries.isEmpty()) {
                        it.unregister()
                        teams.remove(teamName)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Erro ao remover ${player.name} do team: ${e.message}")
                }
            }
            playerTeams.remove(uuid)
        }
    }

    private fun getOrCreateScoreboard(): Scoreboard {
        return Bukkit.getScoreboardManager()?.mainScoreboard
            ?: throw IllegalStateException("ScoreboardManager não disponível!")
    }

    private fun applyScoreboardToAll(scoreboard: Scoreboard) {
        for (player in Bukkit.getOnlinePlayers()) {
            try {
                player.scoreboard = scoreboard
            } catch (e: Exception) {
                plugin.logger.warning("Erro ao aplicar scoreboard para ${player.name}: ${e.message}")
            }
        }
    }

    fun setupPlayerScoreboard(player: Player) {
        try {
            val scoreboard = getOrCreateScoreboard()
            player.scoreboard = scoreboard
        } catch (e: Exception) {
            plugin.logger.warning("Erro ao configurar scoreboard para ${player.name}: ${e.message}")
        }
    }

    fun cleanupPlayer(player: Player) {
        removePlayerFromTeam(player)
    }

    fun cleanup() {
        for (team in teams.values) {
            try {
                team.unregister()
            } catch (e: Exception) {

            }
        }
        teams.clear()
        playerTeams.clear()
    }

    fun refreshAllNametags() {
        val scoreboard = getOrCreateScoreboard()
        applyScoreboardToAll(scoreboard)
    }
}