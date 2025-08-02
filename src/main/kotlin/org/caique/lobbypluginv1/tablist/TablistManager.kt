package org.caique.lobbypluginv1.tablist

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TablistManager {

    private val plugin = Lobbypluginv1.instance
    private val activeTablist = ConcurrentHashMap<UUID, Boolean>()
    private var ticks = 0
    private var updateTask: Int = -1

    fun initialize() {
        startUpdateTask()
    }

    fun createTablist(player: Player) {
        activeTablist[player.uniqueId] = true

        if (testTablistSupport(player)) {
            updateTablist(player)
        } else {
            player.sendMessage("§6§lMineFrex")
            player.sendMessage("§fWebsite: §bminefrex.com.br")
            player.sendMessage("§fDiscord: §bdiscord.gg/minefrex")
            player.sendMessage("§7Servidor em desenvolvimento.")
        }
    }

    private fun testTablistSupport(player: Player): Boolean {
        return try {
            player.setPlayerListHeader("§6Test")
            player.setPlayerListFooter("§7Test")

            player.setPlayerListHeader("")
            player.setPlayerListFooter("")
            true

        } catch (e: Exception) {
            try {
                val method = player.javaClass.getMethod("setPlayerListHeaderAndFooter", String::class.java, String::class.java)
                method.invoke(player, "§6Test", "§7Test")
                method.invoke(player, "", "")
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    fun removeTablist(player: Player) {
        activeTablist.remove(player.uniqueId)

        try {
            player.setPlayerListHeader("")
            player.setPlayerListFooter("")
        } catch (e: Exception) {
            // Ignora erro silenciosamente
        }
    }

    fun updateTablist(player: Player) {
        if (!activeTablist.containsKey(player.uniqueId)) return

        val animatedTitle = TablistAnimator.getAnimatedTitle(ticks)
        val header = TablistAnimator.getHeaderContent(animatedTitle)
        val footer = TablistAnimator.getFooterContent()

        try {
            player.setPlayerListHeader(header)
            player.setPlayerListFooter(footer)

        } catch (e: Exception) {
            try {
                val method = player.javaClass.getMethod("setPlayerListHeaderAndFooter", String::class.java, String::class.java)
                method.invoke(player, header, footer)
            } catch (ex: Exception) {
                activeTablist.remove(player.uniqueId)
            }
        }
    }

    private fun startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            ticks++
            updateAllTablists()
        }, 0L, 10L).taskId
    }

    private fun updateAllTablists() {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline && activeTablist.containsKey(player.uniqueId)) {
                updateTablist(player)
            }
        }
    }

    fun updatePlayerInfo(player: Player) {
        if (activeTablist.containsKey(player.uniqueId)) {
            updateTablist(player)
        }
    }

    fun shutdown() {
        if (updateTask != -1) {
            Bukkit.getScheduler().cancelTask(updateTask)
        }

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isOnline) {
                removeTablist(player)
            }
        }
        activeTablist.clear()
    }
}