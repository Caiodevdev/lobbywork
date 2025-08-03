package org.caique.lobbypluginv1.tagmanager

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class TagListener(private val tagManager: TagManager) : Listener {

    @EventHandler(priority = EventPriority.MONITOR) // Mudou para MONITOR para ser executado depois do ScoreboardManager
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Delay maior para garantir que o ScoreboardManager já criou o scoreboard
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.caique.lobbypluginv1.Lobbypluginv1.instance,
            Runnable {
                if (player.isOnline) {
                    tagManager.loadPlayerTag(player)
                }
            },
            30L // Aumentou o delay para 1.5 segundos
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        tagManager.unloadPlayerData(player.uniqueId)
    }
}