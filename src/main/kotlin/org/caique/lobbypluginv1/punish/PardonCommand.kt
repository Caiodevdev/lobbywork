package org.caique.lobbypluginv1.punish

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PardonCommand(private val punishManager: PunishManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return true
        }

        if (!sender.hasPermission("lobbyplugin.pardon")) {
            sender.sendMessage("§cVocê não tem permissão para executar este comando.")
            sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUso correto: /pardon <id>")
            return true
        }

        val punishmentId = args[0].toIntOrNull()
        if (punishmentId == null) {
            sender.sendMessage("§cID da punição inválido.")
            return true
        }

        val success = punishManager.pardonPunishment(punishmentId, sender)
        if (success) {
            sender.sendMessage("§aPunição #$punishmentId perdoada com sucesso!")
            sender.playSound(sender.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        } else {
            sender.sendMessage("§cNão foi possível encontrar uma punição ativa com o ID #$punishmentId.")
            sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }

        return true
    }
}