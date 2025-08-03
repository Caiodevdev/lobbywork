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

class PunishCommand(private val punishManager: PunishManager) : CommandExecutor {

    // Tipos de infrações pré-definidas
    private val punishmentTypes = mapOf(
        "HACK_CHEAT" to Pair("Uso de Hack/Cheat", PunishType.BAN),
        "OFENSA_GRAVE" to Pair("Ofensas graves", PunishType.BAN),
        "AMEACA" to Pair("Ameaças", PunishType.BAN),
        "DIVULGACAO" to Pair("Divulgação", PunishType.BAN),
        "EVASAO" to Pair("Evasão de punição", PunishType.BAN),
        "FLOOD_SPAM" to Pair("Flood/Spam", PunishType.MUTE),
        "OFENSA_LEVE" to Pair("Ofensas leves", PunishType.MUTE),
        "OFENSA_STAFF" to Pair("Ofensas a Staff", PunishType.MUTE),
        "CAPS" to Pair("Uso excessivo de caps", PunishType.MUTE),
        "PROVOCACAO" to Pair("Provocação", PunishType.MUTE)
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.")
            return true
        }

        if (!sender.hasPermission("lobbyplugin.punish")) {
            sender.sendMessage("§cVocê não tem permissão para executar este comando.")
            sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return true
        }

        // /punir <nick>
        if (args.size == 1) {
            val targetName = args[0]
            val targetPlayer = Bukkit.getOfflinePlayer(targetName)

            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                sender.sendMessage("§cJogador não encontrado.")
                return true
            }

            showPunishmentOptions(sender, targetName)
            return true
        }

        // /punir <nick> <tipo> <prova>
        if (args.size >= 3) {
            val targetName = args[0]
            val infractionType = args[1].uppercase()
            val proof = args.drop(2).joinToString(" ")

            // Verificar se o tipo de infração existe
            if (!punishmentTypes.containsKey(infractionType)) {
                sender.sendMessage("§cTipo de infração inválido!")
                return true
            }

            val (reason, type) = punishmentTypes[infractionType]!!

            // Calcular duração com base no histórico
            val targetPlayer = Bukkit.getOfflinePlayer(targetName)
            val punishments = punishManager.getPlayerPunishments(targetName)
            val duration = calculatePunishmentDuration(targetName, type, punishments)

            // Aplicar punição
            val punishment = punishManager.punishPlayer(
                targetName,
                type,
                "$reason - Prova: $proof",
                duration,
                sender
            )

            if (punishment != null) {
                // Formatar duração para exibição
                val durationText = formatDuration(duration)

                sender.sendMessage("§aPunição aplicada com sucesso!")
                sender.sendMessage("§7Jogador: §f$targetName")
                sender.sendMessage("§7Tipo: §f$type")
                sender.sendMessage("§7Motivo: §f$reason")
                sender.sendMessage("§7Duração: §f$durationText")
                sender.sendMessage("§7Prova: §f$proof")

                sender.playSound(sender.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

                // Notificar staff
                val announcement = "§c§lPUNIÇÃO: §f${targetName} §cfoi ${if (type == PunishType.BAN) "banido" else "silenciado"} por §f${sender.name}§c."
                Bukkit.getOnlinePlayers().forEach { p ->
                    if (p.hasPermission("lobbyplugin.punish")) {
                        p.sendMessage(announcement)
                    }
                }
            } else {
                sender.sendMessage("§cErro ao aplicar punição. Verifique se o jogador existe.")
            }

            return true
        }

        sender.sendMessage("§cUso correto: /punir <jogador> ou /punir <jogador> <tipo> <prova>")
        return true
    }

    private fun showPunishmentOptions(player: Player, targetName: String) {
        player.sendMessage("")
        player.sendMessage("§e§l>> Punir Jogador: §f$targetName")
        player.sendMessage("§7Selecione o tipo de infração:")
        player.sendMessage("")

        // Listar tipos de ban
        player.sendMessage("§c§lBanimentos:")
        punishmentTypes.filter { it.value.second == PunishType.BAN }.forEach { (code, pair) ->
            val (description, _) = pair

            val component = TextComponent("§7- §c$description")
            component.clickEvent = ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/punir $targetName $code (Coloque o link da prova aqui)"
            )
            component.hoverEvent = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ComponentBuilder("§7Clique para selecionar este motivo").create()
            )

            player.spigot().sendMessage(component)
        }

        // Listar tipos de mute
        player.sendMessage("")
        player.sendMessage("§6§lSilenciamentos:")
        punishmentTypes.filter { it.value.second == PunishType.MUTE }.forEach { (code, pair) ->
            val (description, _) = pair

            val component = TextComponent("§7- §6$description")
            component.clickEvent = ClickEvent(
                ClickEvent.Action.SUGGEST_COMMAND,
                "/punir $targetName $code (Coloque o link da prova aqui)"
            )
            component.hoverEvent = HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                ComponentBuilder("§7Clique para selecionar este motivo").create()
            )

            player.spigot().sendMessage(component)
        }

        player.sendMessage("")
    }

    private fun calculatePunishmentDuration(playerName: String, type: PunishType, punishments: List<Punishment>): Long {
        // Verificar punições anteriores do mesmo tipo
        val previousPunishments = punishments.filter { it.type == type }
        val count = previousPunishments.size

        // Duração base (em milissegundos)
        return when (type) {
            PunishType.BAN -> {
                when (count) {
                    0 -> 7 * 24 * 60 * 60 * 1000L  // 7 dias para primeira punição
                    1 -> 15 * 24 * 60 * 60 * 1000L // 15 dias para segunda punição
                    2 -> 30 * 24 * 60 * 60 * 1000L // 30 dias para terceira punição
                    else -> -1L  // Permanente para 4+ punições
                }
            }
            PunishType.MUTE -> {
                when (count) {
                    0 -> 6 * 60 * 60 * 1000L       // 6 horas para primeira punição
                    1 -> 1 * 24 * 60 * 60 * 1000L  // 1 dia para segunda punição
                    2 -> 3 * 24 * 60 * 60 * 1000L  // 3 dias para terceira punição
                    3 -> 7 * 24 * 60 * 60 * 1000L  // 7 dias para quarta punição
                    else -> 15 * 24 * 60 * 60 * 1000L // 15 dias para 5+ punições
                }
            }
        }
    }

    private fun formatDuration(duration: Long): String {
        if (duration == -1L) {
            return "Permanente"
        }

        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days dia(s)"
            hours > 0 -> "$hours hora(s)"
            minutes > 0 -> "$minutes minuto(s)"
            else -> "$seconds segundo(s)"
        }
    }
}