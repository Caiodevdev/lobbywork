package org.caique.lobbypluginv1.tagmanager

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TagsCommand(private val tagManager: TagManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!")
            return true
        }

        val player = sender
        tagManager.showTagsList(player)

        return true
    }
}

class TagCommand(private val tagManager: TagManager) : CommandExecutor {

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
            "set", "equipar" -> {
                if (args.size < 2) {
                    TagUtils.sendErrorMessage(player, "Use: /tag set <id>")
                    return true
                }

                val tagId = args[1].lowercase()

                if (!TagUtils.isValidTagId(tagId)) {
                    TagUtils.sendErrorMessage(player, "Tag inválida! Use /tags para ver as disponíveis.")
                    return true
                }

                tagManager.setPlayerTag(player, tagId)
            }

            "list", "listar" -> {
                tagManager.showTagsList(player)
            }

            "preview", "visualizar" -> {
                if (args.size < 2) {
                    TagUtils.sendErrorMessage(player, "Use: /tag preview <id>")
                    return true
                }

                val tagId = args[1].lowercase()
                val tag = TagRegistry.getTag(tagId)

                if (tag == null) {
                    TagUtils.sendErrorMessage(player, "Tag inválida!")
                    return true
                }

                val preview = TagUtils.getTagPreview(tag, player.name)
                player.sendMessage("")
                player.sendMessage("§6§lPREVIEW DA TAG ${tag.getFormattedTag()}:")
                player.sendMessage("")
                player.sendMessage(preview)
                player.sendMessage("")
                if (tag.id != "membro") {
                    player.sendMessage("§7* O símbolo §l★ §7aparece apenas na nametag")
                    player.sendMessage("")
                }

                TagUtils.playNotificationSound(player)
            }

            "help", "ajuda" -> {
                sendUsage(player)
            }

            else -> {
                TagUtils.sendErrorMessage(player, "Comando inválido! Use /tag help")
            }
        }

        return true
    }

    private fun sendUsage(player: Player) {
        player.sendMessage("")
        player.sendMessage("§6§lSISTEMA DE TAGS")
        player.sendMessage("")
        player.sendMessage("§f/tags §7- Ver todas suas tags disponíveis")
        player.sendMessage("§f/tag set <id> §7- Equipar uma tag")
        player.sendMessage("§f/tag preview <id> §7- Visualizar como ficaria")
        player.sendMessage("§f/tag list §7- Mesmo que /tags")
        player.sendMessage("")
        player.sendMessage("§7Tags disponíveis:")

        val availableTags = tagManager.getAvailableTags(player)
        val tagList = availableTags.joinToString("§7, ") { "${it.color}${it.id}" }
        player.sendMessage("§f$tagList")
        player.sendMessage("")

        TagUtils.playNotificationSound(player)
    }
}