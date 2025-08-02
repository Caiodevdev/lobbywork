package org.caique.lobbypluginv1.auth

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RegisterCommand(private val authManager: AuthManager) : CommandExecutor {

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


        if (args.size < 2) {
            sendRegisterUsage(player)
            return true
        }

        val password = args[0]
        val confirmPassword = args[1]


        if (password.isBlank() || confirmPassword.isBlank()) {
            player.sendMessage("§c✗ As senhas não podem estar vazias!")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return true
        }


        if (!PasswordUtils.isPasswordStrong(password)) {
            sendPasswordStrengthInfo(player, password)
            return true
        }


        player.sendMessage("§e⌛ Criando sua conta...")
        player.sendActionBar("§e⌛ Registrando...")
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f)

        authManager.attemptRegister(player, password, confirmPassword)

        return true
    }

    private fun sendRegisterUsage(player: Player) {
        player.sendMessage("")
        player.sendMessage("                    §a§lCOMO SE REGISTRAR")
        player.sendMessage("")
        player.sendMessage("        §7Use o comando: §f/register <senha> <confirmar_senha>")
        player.sendMessage("")
        player.sendMessage("        §e💡 Exemplo: §f/register minhasenha123 minhasenha123")
        player.sendMessage("")
        player.sendMessage("        §c⚠ §7Requisitos da senha:")
        player.sendMessage("        §7• Mínimo de §f6 caracteres")
        player.sendMessage("        §7• Deve conter §fletras §7e §fnúmeros")
        player.sendMessage("        §7• Não pode ser uma senha comum")
        player.sendMessage("")

        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        player.sendActionBar("§c✗ Use: /register <senha> <confirmar_senha>")
    }

    private fun sendPasswordStrengthInfo(player: Player, password: String) {
        val strength = PasswordUtils.getPasswordStrength(password)

        player.sendMessage("")
        player.sendMessage("                    §c§lSENHA MUITO FRACA!")
        player.sendMessage("")
        player.sendMessage("        §7Força atual: ${strength.color}${strength.displayName}")
        player.sendMessage("")
        player.sendMessage("        §7Para uma senha segura:")
        player.sendMessage("        §a✓ §7Use pelo menos §f6 caracteres")
        player.sendMessage("        §a✓ §7Inclua §fletras §7e §fnúmeros")
        player.sendMessage("        §a✓ §7Evite senhas comuns como §f123456")
        player.sendMessage("")
        player.sendMessage("        §e💡 Exemplo de senha forte: §fMinhaSenh4123")
        player.sendMessage("")

        player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f)
        player.sendActionBar("§c✗ Senha muito fraca! Use uma senha mais segura")
    }
}