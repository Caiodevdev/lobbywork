package org.caique.lobbypluginv1.tagmanager

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class NametagTestCommand(private val tagManager: TagManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!")
            return true
        }

        val player = sender

        player.sendMessage("§6=== TESTE DE NAMETAG ===")

        // Informações básicas
        player.sendMessage("§7Jogador: §f${player.name}")
        player.sendMessage("§7UUID: §f${player.uniqueId}")

        // Testa sistema de autenticação
        val authManager = org.caique.lobbypluginv1.Lobbypluginv1.getAuthManager()
        val isAuthenticated = authManager.isPlayerAuthenticated(player.uniqueId)
        player.sendMessage("§7Autenticado: ${if (isAuthenticated) "§aSiml" else "§cNão"}")

        // Informações da tag atual
        val currentTag = tagManager.getPlayerTag(player.uniqueId)
        player.sendMessage("§7Tag atual: ${currentTag.getFormattedTag()}")
        player.sendMessage("§7ID da tag: §f${currentTag.id}")

        // Testa scoreboard
        try {
            val scoreboardManager = Bukkit.getScoreboardManager()
            if (scoreboardManager != null) {
                player.sendMessage("§aScoreboardManager: Disponível")

                val scoreboard = scoreboardManager.mainScoreboard
                player.sendMessage("§aMainScoreboard: Disponível")

                val teams = scoreboard.teams
                player.sendMessage("§7Teams existentes: §f${teams.size}")

                val playerTeam = scoreboard.getEntryTeam(player.name)
                if (playerTeam != null) {
                    player.sendMessage("§aJogador está em team: §f${playerTeam.name}")
                    player.sendMessage("§7Prefix: §f'${playerTeam.prefix}'")
                    player.sendMessage("§7Suffix: §f'${playerTeam.suffix}'")
                    player.sendMessage("§7Membros: §f${playerTeam.entries.size}")
                } else {
                    player.sendMessage("§cJogador NÃO está em nenhum team!")
                }

                // Lista todos os teams que começam com "tag_"
                val tagTeams = teams.filter { it.name.startsWith("tag_") }
                if (tagTeams.isNotEmpty()) {
                    player.sendMessage("§7Teams de tag encontrados:")
                    tagTeams.forEach { team ->
                        player.sendMessage("§f- ${team.name} (${team.entries.size} membros)")
                    }
                } else {
                    player.sendMessage("§cNenhum team de tag encontrado!")
                }

            } else {
                player.sendMessage("§cScoreboardManager: NÃO DISPONÍVEL!")
            }
        } catch (e: Exception) {
            player.sendMessage("§cErro ao verificar scoreboard: ${e.message}")
        }

        // Testa CustomName
        try {
            val customName = player.customName
            val isVisible = player.isCustomNameVisible
            player.sendMessage("§7CustomName: §f${customName ?: "null"}")
            player.sendMessage("§7CustomName visível: ${if (isVisible) "§aSiml" else "§cNão"}")
        } catch (e: Exception) {
            player.sendMessage("§cErro ao verificar CustomName: ${e.message}")
        }

        // Testa DisplayName
        try {
            val displayName = player.displayName
            player.sendMessage("§7DisplayName: §f$displayName")
        } catch (e: Exception) {
            player.sendMessage("§cErro ao verificar DisplayName: ${e.message}")
        }

        // Testa PlayerListName
        try {
            val listName = player.playerListName
            player.sendMessage("§7PlayerListName: §f$listName")
        } catch (e: Exception) {
            player.sendMessage("§cErro ao verificar PlayerListName: ${e.message}")
        }

        player.sendMessage("")
        player.sendMessage("§e=== TESTE MANUAL ===")

        if (args.isNotEmpty() && args[0].equals("force", true)) {
            player.sendMessage("§eForçando atualização...")

            try {
                // Força recriação da tag
                tagManager.loadPlayerTag(player)

                // Aguarda um pouco e testa novamente
                Bukkit.getScheduler().runTaskLater(
                    org.caique.lobbypluginv1.Lobbypluginv1.instance,
                    Runnable {
                        player.sendMessage("§aAtualização forçada concluída!")

                        // Verifica novamente
                        val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
                        val team = scoreboard?.getEntryTeam(player.name)
                        if (team != null) {
                            player.sendMessage("§aAgora em team: §f${team.name}")
                            player.sendMessage("§aPrefix: §f'${team.prefix}'")
                        } else {
                            player.sendMessage("§cAinda não está em team!")
                        }
                    },
                    40L
                )

            } catch (e: Exception) {
                player.sendMessage("§cErro no teste manual: ${e.message}")
            }
        } else {
            player.sendMessage("§7Use §f/nametagtest force §7para forçar atualização")
        }

        player.sendMessage("§6========================")

        return true
    }
}