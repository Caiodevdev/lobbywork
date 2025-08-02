package org.caique.lobbypluginv1.auth

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.entity.Player

class AuthListener(private val authManager: AuthManager) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        event.joinMessage = null

        authManager.handlePlayerJoin(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {

        event.quitMessage = null


        authManager.cleanup(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {

            if (event.from.distance(event.to ?: return) > 0.1) {
                event.isCancelled = true
                player.teleport(event.from)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendMessage("§c✗ Você precisa se autenticar antes de falar no chat!")
            player.sendActionBar("§c✗ Faça login para falar no chat")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val command = event.message.lowercase()

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {

            val allowedCommands = listOf("/login", "/register", "/l", "/reg")
            val isAllowed = allowedCommands.any { command.startsWith(it) }

            if (!isAllowed) {
                event.isCancelled = true
                player.sendMessage("§c✗ Você só pode usar comandos de autenticação!")
                player.sendActionBar("§c✗ Use /login ou /register")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para interagir com blocos")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para interagir com blocos")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para interagir")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para usar inventários")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para dropar itens")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
            player.sendActionBar("§c✗ Faça login para pegar itens")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player

        if (!authManager.isPlayerAuthenticated(player.uniqueId)) {

            if (event.cause == PlayerTeleportEvent.TeleportCause.PLUGIN ||
                event.cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
                event.isCancelled = true
                player.sendActionBar("§c✗ Faça login para se teleportar")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerKick(event: PlayerKickEvent) {

        authManager.cleanup(event.player.uniqueId)
    }
}