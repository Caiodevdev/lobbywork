package org.caique.lobbypluginv1.auth

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LoginCommand(private val authManager: AuthManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!")
            return true
        }

        val player = sender


        if (authManager.isPlayerAuthenticated(player.uniqueId)) {
            player.sendMessage("§c✗ Você já está autenticado!")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return true
        }


        if (args.isEmpty()) {
            sendLoginUsage(player)
            return true
        }

        val password = args[0]


        if (password.isBlank()) {
            player.sendMessage("§c✗ A senha não pode estar vazia!")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return true
        }


        player.sendMessage("§e⌛ Verificando credenciais...")
        player.sendActionBar("§e⌛ Autenticando...")
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f)

        authManager.attemptLogin(player, password)

        return true
    }

    private fun sendLoginUsage(player: Player) {
        player.sendMessage("")
        player.sendMessage("                    §6§lCOMO FAZER LOGIN")
        player.sendMessage("")
        player.sendMessage("        §7Use o comando: §f/login <sua_senha>")
        player.sendMessage("")
        player.sendMessage("        §e💡 Exemplo: §f/login minhasenha123")
        player.sendMessage("")

        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        player.sendActionBar("§c✗ Use: /login <senha>")
    }
}