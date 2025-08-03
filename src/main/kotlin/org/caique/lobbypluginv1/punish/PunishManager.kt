package org.caique.lobbypluginv1.punish

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PunishManager {
    private val punishments = ConcurrentHashMap<UUID, MutableList<Punishment>>()
    private val activePunishments = ConcurrentHashMap<UUID, MutableMap<PunishType, Punishment>>()
    private var nextPunishmentId = 1
    private val database = PunishDatabase()

    fun initialize() {
        // Inicializar conexão com o banco de dados
        database.connect().thenAccept { success ->
            if (success) {
                Lobbypluginv1.instance.logger.info("Conexão com banco de dados de punições estabelecida com sucesso!")
                // Carregar punições do banco de dados
                loadPunishmentsFromDatabase()
            } else {
                Lobbypluginv1.instance.logger.severe("Falha ao conectar com banco de dados de punições!")
            }
        }

        // Agendar verificação periódica de punições expiradas
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            Lobbypluginv1.instance,
            Runnable { checkExpiredPunishments() },
            20L * 60, // A cada 1 minuto
            20L * 60
        )
    }

    fun shutdown() {
        // Atualizar punições expiradas antes de desconectar
        database.updateExpiredPunishments().join()

        // Fechar conexão com o banco de dados
        database.disconnect()
    }

    private fun loadPunishmentsFromDatabase() {
        Bukkit.getScheduler().runTaskAsynchronously(Lobbypluginv1.instance, Runnable {
            // Obter o maior ID para continuar a sequência
            Bukkit.getOnlinePlayers().forEach { player ->
                database.getPlayerPunishments(player.name).thenAccept { dbPunishments ->
                    if (dbPunishments.isNotEmpty()) {
                        // Atualizar o maior ID
                        val maxId = dbPunishments.maxOfOrNull { it.id } ?: 0
                        if (maxId >= nextPunishmentId) {
                            nextPunishmentId = maxId + 1
                        }

                        // Adicionar punições ao cache
                        val playerPunishments = punishments.computeIfAbsent(player.uniqueId) { mutableListOf() }
                        playerPunishments.addAll(dbPunishments)

                        // Adicionar punições ativas
                        val active = dbPunishments.filter { it.status == PunishStatus.ATIVO }
                        val playerActivePunishments = activePunishments.computeIfAbsent(player.uniqueId) { mutableMapOf() }

                        active.forEach { punishment ->
                            // Verificar se está expirado
                            if (punishment.expiresAt > 0 && punishment.expiresAt < System.currentTimeMillis()) {
                                punishment.status = PunishStatus.EXPIRADO
                                database.updatePunishmentStatus(punishment.id, PunishStatus.EXPIRADO)
                            } else {
                                playerActivePunishments[punishment.type] = punishment
                            }
                        }
                    }
                }
            }
        })
    }

    private fun checkExpiredPunishments() {
        val currentTime = System.currentTimeMillis()

        // Atualizar punições expiradas no banco de dados
        database.updateExpiredPunishments().thenAccept { count ->
            if (count > 0) {
                Lobbypluginv1.instance.logger.info("$count punições marcadas como expiradas no banco de dados.")
            }
        }

        // Verificar cache local
        activePunishments.forEach { (uuid, typeToPunishment) ->
            val toRemove = mutableListOf<PunishType>()

            typeToPunishment.forEach { (type, punishment) ->
                if (punishment.expiresAt > 0 && punishment.expiresAt < currentTime) {
                    punishment.status = PunishStatus.EXPIRADO
                    toRemove.add(type)

                    // Atualizar no banco de dados
                    database.updatePunishmentStatus(punishment.id, PunishStatus.EXPIRADO)

                    // Notificar jogador caso esteja online
                    val player = Bukkit.getPlayer(uuid)
                    player?.let {
                        if (type == PunishType.MUTE) {
                            it.sendMessage("§aSua punição de silenciamento expirou. Você pode falar novamente.")
                            it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                        }
                    }
                }
            }

            toRemove.forEach { type ->
                typeToPunishment.remove(type)
            }

            if (typeToPunishment.isEmpty()) {
                activePunishments.remove(uuid)
            }
        }
    }

    fun punishPlayer(
        playerName: String,
        type: PunishType,
        reason: String,
        duration: Long,
        admin: Player
    ): Punishment? {
        val targetPlayer = Bukkit.getOfflinePlayer(playerName)
        if (targetPlayer.uniqueId == null) {
            return null
        }

        val currentTime = System.currentTimeMillis()
        val expiresAt = if (duration <= 0) -1L else currentTime + duration

        val punishment = Punishment(
            id = nextPunishmentId++,
            playerUUID = targetPlayer.uniqueId,
            playerName = playerName,
            reason = reason,
            type = type,
            adminUUID = admin.uniqueId,
            adminName = admin.name,
            createdAt = currentTime,
            expiresAt = expiresAt,
            status = PunishStatus.ATIVO
        )

        // Adicionar à lista de punições do jogador
        punishments.computeIfAbsent(targetPlayer.uniqueId) { mutableListOf() }.add(punishment)

        // Adicionar às punições ativas
        activePunishments
            .computeIfAbsent(targetPlayer.uniqueId) { mutableMapOf() }
            .put(type, punishment)

        // Salvar no banco de dados
        database.addPunishment(punishment).thenAccept { id ->
            if (id > 0) {
                punishment.id = id
            }
        }

        // Se o jogador estiver online, aplicar efeitos da punição
        val onlinePlayer = Bukkit.getPlayer(targetPlayer.uniqueId)
        onlinePlayer?.let {
            when (type) {
                PunishType.BAN -> {
                    val banMessage = buildBanMessage(punishment)
                    it.kickPlayer(banMessage)
                }
                PunishType.MUTE -> {
                    it.sendMessage("§cVocê foi silenciado por ${admin.name}. Motivo: ${reason}")
                    it.sendMessage("§cExpiração: ${punishment.formatExpiresAt()}")
                    it.playSound(it.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                }
            }
        }

        return punishment
    }

    fun pardonPunishment(id: Int, admin: Player): Boolean {
        // Primeiro verificar no cache em memória
        for ((uuid, playerPunishments) in punishments) {
            for (punishment in playerPunishments) {
                if (punishment.id == id && punishment.status == PunishStatus.ATIVO) {
                    // Atualizar status
                    punishment.status = PunishStatus.DESBANIDO

                    // Remover das punições ativas
                    activePunishments[uuid]?.remove(punishment.type)
                    if (activePunishments[uuid]?.isEmpty() == true) {
                        activePunishments.remove(uuid)
                    }

                    // Atualizar no banco de dados
                    database.updatePunishmentStatus(id, PunishStatus.DESBANIDO, admin.uniqueId)

                    // Notificar jogador se estiver online
                    val player = Bukkit.getPlayer(uuid)
                    player?.let {
                        when (punishment.type) {
                            PunishType.MUTE -> {
                                it.sendMessage("§aSua punição de silenciamento foi perdoada por ${admin.name}.")
                                it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                            }
                            else -> {}
                        }
                    }

                    return true
                }
            }
        }

        // Se não encontrou no cache, verificar no banco de dados
        val dbPunishment = database.getPunishmentById(id).join()

        if (dbPunishment != null && dbPunishment.status == PunishStatus.ATIVO) {
            // Atualizar status no banco de dados
            database.updatePunishmentStatus(id, PunishStatus.DESBANIDO, admin.uniqueId)

            // Atualizar o cache após o perdão
            // Recarregar todas as punições do jogador para manter o cache atualizado
            val allPunishments = database.getPlayerPunishments(dbPunishment.playerName).join()
            punishments[dbPunishment.playerUUID] = allPunishments.toMutableList()

            // Remover das punições ativas
            activePunishments[dbPunishment.playerUUID]?.remove(dbPunishment.type)
            if (activePunishments[dbPunishment.playerUUID]?.isEmpty() == true) {
                activePunishments.remove(dbPunishment.playerUUID)
            }

            // Notificar jogador se estiver online
            val player = Bukkit.getPlayer(dbPunishment.playerUUID)
            player?.let {
                when (dbPunishment.type) {
                    PunishType.MUTE -> {
                        it.sendMessage("§aSua punição de silenciamento foi perdoada por ${admin.name}.")
                        it.playSound(it.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    }
                    else -> {}
                }
            }

            return true
        }

        return false
    }

    fun getPlayerPunishments(playerName: String): List<Punishment> {
        // Sempre buscar do banco de dados para garantir que temos todas as punições atualizadas
        val dbPunishments = database.getPlayerPunishments(playerName).join()

        // Atualizar o cache local com os dados mais recentes
        val targetPlayer = Bukkit.getOfflinePlayer(playerName)
        if (targetPlayer.uniqueId != null) {
            punishments[targetPlayer.uniqueId] = dbPunishments.toMutableList()
        }

        return dbPunishments
    }

    fun isPlayerBanned(uuid: UUID): Boolean {
        // Verificar cache primeiro
        if (activePunishments[uuid]?.containsKey(PunishType.BAN) == true) {
            return true
        }

        // Verificar banco de dados
        return database.getActivePunishment(uuid, PunishType.BAN).join() != null
    }

    fun isPlayerMuted(uuid: UUID): Boolean {
        // Verificar cache primeiro
        if (activePunishments[uuid]?.containsKey(PunishType.MUTE) == true) {
            return true
        }

        // Verificar banco de dados
        return database.getActivePunishment(uuid, PunishType.MUTE).join() != null
    }

    fun getActiveBan(uuid: UUID): Punishment? {
        // Verificar cache primeiro
        val cachedBan = activePunishments[uuid]?.get(PunishType.BAN)
        if (cachedBan != null) {
            return cachedBan
        }

        // Verificar banco de dados
        return database.getActivePunishment(uuid, PunishType.BAN).join()
    }

    fun getActiveMute(uuid: UUID): Punishment? {
        // Verificar cache primeiro
        val cachedMute = activePunishments[uuid]?.get(PunishType.MUTE)
        if (cachedMute != null) {
            return cachedMute
        }

        // Verificar banco de dados
        return database.getActivePunishment(uuid, PunishType.MUTE).join()
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