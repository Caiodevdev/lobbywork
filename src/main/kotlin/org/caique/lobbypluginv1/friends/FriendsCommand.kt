package org.caique.lobbypluginv1.friends

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class FriendsCommand(private val friendsManager: FriendsManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!")
            return true
        }

        val player = sender

        if (args.isEmpty()) {
            sendUsage(player)
            return true
        }

        when (args[0].lowercase()) {
            "listar", "list" -> {
                friendsManager.showFriendsList(player)
            }

            "add", "adicionar" -> {
                if (args.size < 2) {
                    FriendsUtils.sendErrorMessage(player, "Use: /amigos add <nick>")
                    return true
                }

                val targetName = args[1]
                friendsManager.sendFriendRequest(player, targetName)
            }

            "remove", "remover" -> {
                if (args.size < 2) {
                    FriendsUtils.sendErrorMessage(player, "Use: /amigos remove <nick>")
                    return true
                }

                val targetName = args[1]
                friendsManager.removeFriend(player, targetName)
            }

            "aceitar", "accept" -> {
                if (args.size < 2) {
                    FriendsUtils.sendErrorMessage(player, "Use: /amigos aceitar <nick>")
                    return true
                }

                val senderName = args[1]
                friendsManager.acceptFriendRequest(player, senderName)
            }

            "recusar", "reject", "deny" -> {
                if (args.size < 2) {
                    FriendsUtils.sendErrorMessage(player, "Use: /amigos recusar <nick>")
                    return true
                }

                val senderName = args[1]
                friendsManager.rejectFriendRequest(player, senderName)
            }

            "ajuda", "help" -> {
                sendUsage(player)
            }

            else -> {
                FriendsUtils.sendErrorMessage(player, "Comando inválido! Use /amigos ajuda")
            }
        }

        return true
    }

    private fun sendUsage(player: Player) {
        player.sendMessage("")
        player.sendMessage("§b§lSISTEMA DE AMIGOS")
        player.sendMessage("")
        player.sendMessage("§f/amigos listar §7- Ver sua lista de amigos")
        player.sendMessage("§f/amigos add <nick> §7- Enviar solicitação de amizade")
        player.sendMessage("§f/amigos remove <nick> §7- Remover um amigo")
        player.sendMessage("§f/amigos aceitar <nick> §7- Aceitar solicitação")
        player.sendMessage("§f/amigos recusar <nick> §7- Recusar solicitação")
        player.sendMessage("")

        FriendsUtils.playNotificationSound(player)
    }
}