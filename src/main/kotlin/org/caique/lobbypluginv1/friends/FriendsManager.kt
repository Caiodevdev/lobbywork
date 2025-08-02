package org.caique.lobbypluginv1.friends

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FriendsManager {

    private val plugin = Lobbypluginv1.instance
    private val database = FriendsDatabase()

    private val onlineFriends = ConcurrentHashMap<UUID, MutableList<FriendData>>()
    private val pendingRequests = ConcurrentHashMap<UUID, MutableList<FriendRequest>>()

    fun initialize() {
        database.connect().thenAccept { success ->
            if (!success) {
                plugin.logger.severe("Falha ao conectar com database de amigos!")
            }
        }
    }

    fun loadPlayerData(player: Player) {
        val uuid = player.uniqueId

        database.getFriends(uuid).thenAccept { friends ->
            val updatedFriends = friends.map { friend ->
                friend.copy(
                    isOnline = FriendsUtils.isPlayerOnline(friend.username),
                    server = if (FriendsUtils.isPlayerOnline(friend.username)) FriendsUtils.getServerName() else "Offline"
                )
            }.toMutableList()

            onlineFriends[uuid] = updatedFriends
        }

        database.getPendingRequests(uuid).thenAccept { requests ->
            pendingRequests[uuid] = requests.toMutableList()

            if (requests.isNotEmpty()) {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    FriendsUtils.sendInfoMessage(player, "Você tem ${requests.size} solicitação(ões) de amizade pendente(s)!")
                }, 60L)
            }
        }
    }

    fun unloadPlayerData(uuid: UUID) {
        onlineFriends.remove(uuid)
        pendingRequests.remove(uuid)
    }

    fun sendFriendRequest(sender: Player, targetName: String) {
        if (!FriendsUtils.isValidUsername(targetName)) {
            FriendsUtils.sendErrorMessage(sender, "Nome de usuário inválido!")
            return
        }

        if (sender.name.equals(targetName, ignoreCase = true)) {
            FriendsUtils.sendErrorMessage(sender, "Você não pode adicionar a si mesmo!")
            return
        }

        val targetPlayer = Bukkit.getPlayer(targetName)
        if (targetPlayer == null) {
            FriendsUtils.sendErrorMessage(sender, "Jogador não encontrado ou offline!")
            return
        }

        val senderUuid = sender.uniqueId
        val targetUuid = targetPlayer.uniqueId

        database.areFriends(senderUuid, targetUuid).thenAccept { areFriends ->
            if (areFriends) {
                FriendsUtils.sendErrorMessage(sender, "Vocês já são amigos!")
                return@thenAccept
            }

            database.hasRequest(senderUuid, targetUuid).thenAccept { hasRequest ->
                if (hasRequest) {
                    FriendsUtils.sendErrorMessage(sender, "Você já enviou uma solicitação para este jogador!")
                    return@thenAccept
                }

                database.hasRequest(targetUuid, senderUuid).thenAccept { hasReverseRequest ->
                    if (hasReverseRequest) {
                        acceptFriendRequest(sender, targetName)
                        return@thenAccept
                    }

                    val request = FriendRequest(senderUuid, sender.name, targetUuid, targetPlayer.name)

                    database.addFriendRequest(request).thenAccept { success ->
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            if (success) {
                                FriendsUtils.sendSuccessMessage(sender, "Solicitação de amizade enviada para §f${targetPlayer.name}§a!")

                                pendingRequests.computeIfAbsent(targetUuid) { mutableListOf() }.add(request)
                                FriendsUtils.sendFriendRequest(targetPlayer, sender.name, senderUuid.toString())
                            } else {
                                FriendsUtils.sendErrorMessage(sender, "Erro ao enviar solicitação!")
                            }
                        })
                    }
                }
            }
        }
    }

    fun acceptFriendRequest(receiver: Player, senderName: String) {
        val senderPlayer = Bukkit.getPlayer(senderName)
        if (senderPlayer == null) {
            FriendsUtils.sendErrorMessage(receiver, "Jogador não encontrado!")
            return
        }

        val receiverUuid = receiver.uniqueId
        val senderUuid = senderPlayer.uniqueId

        database.hasRequest(senderUuid, receiverUuid).thenAccept { hasRequest ->
            if (!hasRequest) {
                FriendsUtils.sendErrorMessage(receiver, "Solicitação não encontrada!")
                return@thenAccept
            }

            database.removeFriendRequest(senderUuid, receiverUuid).thenAccept { removed ->
                if (removed) {
                    val addFriend1 = database.addFriend(receiverUuid, senderUuid, senderPlayer.name)
                    val addFriend2 = database.addFriend(senderUuid, receiverUuid, receiver.name)

                    addFriend1.thenCombine(addFriend2) { success1, success2 ->
                        success1 && success2
                    }.thenAccept { success ->
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            if (success) {
                                FriendsUtils.sendSuccessMessage(receiver, "Agora você e §f${senderPlayer.name} §asão amigos!")
                                FriendsUtils.sendSuccessMessage(senderPlayer, "§f${receiver.name} §aaceitou sua solicitação de amizade!")

                                updateFriendsList(receiver)
                                updateFriendsList(senderPlayer)

                                pendingRequests[receiverUuid]?.removeIf { it.senderUuid == senderUuid }
                            } else {
                                FriendsUtils.sendErrorMessage(receiver, "Erro ao aceitar solicitação!")
                            }
                        })
                    }
                }
            }
        }
    }

    fun rejectFriendRequest(receiver: Player, senderName: String) {
        val senderPlayer = Bukkit.getPlayer(senderName)
        if (senderPlayer == null) {
            FriendsUtils.sendErrorMessage(receiver, "Jogador não encontrado!")
            return
        }

        val receiverUuid = receiver.uniqueId
        val senderUuid = senderPlayer.uniqueId

        database.removeFriendRequest(senderUuid, receiverUuid).thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    FriendsUtils.sendInfoMessage(receiver, "Solicitação de §f${senderPlayer.name} §erecusada.")
                    FriendsUtils.sendErrorMessage(senderPlayer, "§f${receiver.name} §crecusou sua solicitação de amizade.")

                    pendingRequests[receiverUuid]?.removeIf { it.senderUuid == senderUuid }
                } else {
                    FriendsUtils.sendErrorMessage(receiver, "Solicitação não encontrada!")
                }
            })
        }
    }

    fun removeFriend(player: Player, friendName: String) {
        val friendPlayer = Bukkit.getPlayer(friendName)
        if (friendPlayer == null) {
            FriendsUtils.sendErrorMessage(player, "Jogador não encontrado!")
            return
        }

        val playerUuid = player.uniqueId
        val friendUuid = friendPlayer.uniqueId

        database.areFriends(playerUuid, friendUuid).thenAccept { areFriends ->
            if (!areFriends) {
                FriendsUtils.sendErrorMessage(player, "Vocês não são amigos!")
                return@thenAccept
            }

            val removeFriend1 = database.removeFriend(playerUuid, friendUuid)
            val removeFriend2 = database.removeFriend(friendUuid, playerUuid)

            removeFriend1.thenCombine(removeFriend2) { success1, success2 ->
                success1 && success2
            }.thenAccept { success ->
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (success) {
                        FriendsUtils.sendSuccessMessage(player, "§f${friendPlayer.name} §afoi removido da sua lista de amigos.")

                        if (friendPlayer.isOnline) {
                            FriendsUtils.sendInfoMessage(friendPlayer, "§f${player.name} §eremoveu você da lista de amigos.")
                        }

                        updateFriendsList(player)
                        if (friendPlayer.isOnline) {
                            updateFriendsList(friendPlayer)
                        }
                    } else {
                        FriendsUtils.sendErrorMessage(player, "Erro ao remover amigo!")
                    }
                })
            }
        }
    }

    fun showFriendsList(player: Player) {
        val uuid = player.uniqueId
        val friends = onlineFriends[uuid] ?: emptyList()

        val messages = FriendsUtils.formatFriendsList(friends)
        messages.forEach { player.sendMessage(it) }

        FriendsUtils.playNotificationSound(player)
    }

    private fun updateFriendsList(player: Player) {
        if (player.isOnline) {
            loadPlayerData(player)
        }
    }

    fun shutdown() {
        database.disconnect()
    }
}