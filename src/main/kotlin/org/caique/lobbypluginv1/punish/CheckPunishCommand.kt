package org.caique.lobbypluginv1.punish

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

class CheckPunishCommand(private val punishManager: PunishManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return true
        }

        if (!sender.hasPermission("lobbyplugin.checkpunish")) {
            sender.sendMessage("§cVocê não tem permissão para executar este comando.")
            sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUso correto: /checkpunir <jogador>")
            return true
        }

        val targetName = args[0]
        val punishments = punishManager.getPlayerPunishments(targetName)

        if (punishments.isEmpty()) {
            sender.sendMessage("§aO jogador §f$targetName §anão possui histórico de punições.")
            return true
        }

        displayPunishmentList(sender, targetName, punishments)
        return true
    }

    private fun displayPunishmentList(player: Player, targetName: String, punishments: List<Punishment>) {
        player.sendMessage("")
        player.sendMessage("§e§l>> Histórico de Punições: §f$targetName")
        player.sendMessage("§7Total de punições: §f${punishments.size}")

        // Adicionar legenda com os possíveis status
        player.sendMessage("")
        player.sendMessage("§7Status: §a[ATIVO] §c[EXPIRADO] §7[DESBANIDO]")
        player.sendMessage("")

        // Organizar punições por data (mais recentes primeiro)
        val sortedPunishments = punishments.sortedByDescending { it.createdAt }

        for ((index, punishment) in sortedPunishments.withIndex()) {
            if (index >= 10) {
                player.sendMessage("§7E mais ${punishments.size - 10} punições...")
                break
            }

            // Cores conforme solicitado
            val statusColor = when (punishment.status) {
                PunishStatus.ATIVO -> "§a"     // Verde para ativo
                PunishStatus.EXPIRADO -> "§c"  // Vermelho para expirado
                PunishStatus.DESBANIDO -> "§7" // Cinza para desbanido
            }

            val statusText = when (punishment.status) {
                PunishStatus.ATIVO -> "ATIVO"
                PunishStatus.EXPIRADO -> "EXPIRADO"
                PunishStatus.DESBANIDO -> "DESBANIDO"
            }

            val typeText = when (punishment.type) {
                PunishType.BAN -> "BANIMENTO"
                PunishType.MUTE -> "SILENCIAMENTO"
            }

            val date = Date(punishment.createdAt)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm")
            val formattedDate = sdf.format(date)


            val punishmentComponent = TextComponent(
                "$statusColor#${punishment.id} $typeText - ${punishment.reason} - $statusText"
            )

            val detailsBuilder = ComponentBuilder("")
                .append("§eID: §f#${punishment.id}\n")
                .append("§eTipo: §f$typeText\n")
                .append("§eMotivo: §f${punishment.reason}\n")
                .append("§eAplicado por: §f${punishment.adminName}\n")
                .append("§eData: §f$formattedDate\n")
                .append("§eStatus: $statusColor$statusText\n")

            if (punishment.expiresAt > 0) {
                val expireDate = Date(punishment.expiresAt)
                detailsBuilder.append("§eExpira em: §f${sdf.format(expireDate)}\n")
            } else if (punishment.expiresAt == -1L) {
                detailsBuilder.append("§eDuração: §fPermanente\n")
            }

            punishmentComponent.hoverEvent = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                detailsBuilder.create()
            )

            if (player.hasPermission("lobbyplugin.pardon") && punishment.status == PunishStatus.ATIVO) {
                val pardonComponent = TextComponent(" §f[Perdoar]")
                pardonComponent.clickEvent = ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/pardon ${punishment.id}"
                )
                pardonComponent.hoverEvent = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    ComponentBuilder("§7Clique para perdoar esta punição").create()
                )

                punishmentComponent.addExtra(pardonComponent)
            }

            player.spigot().sendMessage(punishmentComponent)
        }

        player.sendMessage("")
    }
}