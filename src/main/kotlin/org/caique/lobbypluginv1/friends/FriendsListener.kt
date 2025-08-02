package org.caique.lobbypluginv1.friends

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class FriendsListener(private val friendsManager: FriendsManager) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Carrega dados do jogador após um pequeno delay
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.caique.lobbypluginv1.Lobbypluginv1.instance,
            Runnable {
                if (player.isOnline) {
                    friendsManager.loadPlayerData(player)
                }
            },
            20L
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // Remove dados do jogador da memória
        friendsManager.unloadPlayerData(player.uniqueId)
    }
}