package org.caique.lobbypluginv1.tablist

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class TablistListener(private val tablistManager: TablistManager) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.caique.lobbypluginv1.Lobbypluginv1.instance,
            Runnable {
                if (player.isOnline) {
                    tablistManager.createTablist(player)
                }
            },
            40L
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        tablistManager.removeTablist(player)
    }
}