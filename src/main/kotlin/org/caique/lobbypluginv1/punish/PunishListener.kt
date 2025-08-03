package org.caique.lobbypluginv1.punish

import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PunishListener(private val punishManager: PunishManager) : Listener {

    // Usar um mapa para controlar quando exibir a mensagem de mute novamente
    private val muteMessageCooldown = ConcurrentHashMap<UUID, Long>()
    private val COOLDOWN_TIME = 30000L // 30 segundos em milissegundos

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        val activeBan = punishManager.getActiveBan(player.uniqueId)

        if (activeBan != null) {
            val banMessage = buildBanMessage(activeBan)
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAsyncChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val activeMute = punishManager.getActiveMute(player.uniqueId)

        if (activeMute != null) {
            event.isCancelled = true

            // Verificar se já exibimos a mensagem recentemente
            val lastMessageTime = muteMessageCooldown.getOrDefault(player.uniqueId, 0L)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastMessageTime > COOLDOWN_TIME) {
                // Só exibe a mensagem se passou o tempo de cooldown
                val expiresText = if (activeMute.expiresAt == -1L) {
                    "nunca (permanente)"
                } else {
                    SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(activeMute.expiresAt))
                }

                player.sendMessage("§cVocê está silenciado e não pode falar no chat.")
                player.sendMessage("§cMotivo: §f${activeMute.reason}")
                player.sendMessage("§cExpira em: §f$expiresText")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)

                // Atualizar o tempo da última mensagem
                muteMessageCooldown[player.uniqueId] = currentTime
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val activeMute = punishManager.getActiveMute(player.uniqueId)

        // Notificar jogador sobre silenciamento ativo ao entrar
        if (activeMute != null) {
            val expiresText = if (activeMute.expiresAt == -1L) {
                "nunca (permanente)"
            } else {
                SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(activeMute.expiresAt))
            }

            player.sendMessage("§cVocê está silenciado e não pode falar no chat.")
            player.sendMessage("§cMotivo: §f${activeMute.reason}")
            player.sendMessage("§cExpira em: §f$expiresText")

            // Registrar que a mensagem foi exibida agora
            muteMessageCooldown[player.uniqueId] = System.currentTimeMillis()
        }
    }

    private fun buildBanMessage(punishment: Punishment): String {
        val sb = StringBuilder()
        sb.appendLine("§c§lVocê foi banido do servidor!")
        sb.appendLine("§r")
        sb.appendLine("§7Motivo: §f${punishment.reason}")
        sb.appendLine("§7Aplicado por: §f${punishment.adminName}")
        sb.appendLine("§7Data: §f${punishment.formatCreatedAt()}")

        if (punishment.expiresAt == -1L) {
            sb.appendLine("§7Duração: §fPermanente")
        } else {
            sb.appendLine("§7Expira em: §f${punishment.formatExpiresAt()}")
        }

        sb.appendLine("§r")
        sb.appendLine("§7Para contestar, entre em contato no Discord: §fdiscord.servidor.com")

        return sb.toString()
    }
}