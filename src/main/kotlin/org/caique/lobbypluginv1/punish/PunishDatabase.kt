package org.caique.lobbypluginv1.punish

import org.caique.lobbypluginv1.Lobbypluginv1
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.CompletableFuture

class PunishDatabase {

    private val plugin = Lobbypluginv1.instance
    private var connection: Connection? = null

    private val host = "localhost"
    private val port = 3306
    private val database = "caiquedb"
    private val username = "redesky"
    private val password = "1234"

    fun connect(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                if (connection != null && !connection!!.isClosed) {
                    return@supplyAsync true
                }

                Class.forName("com.mysql.cj.jdbc.Driver")

                val url = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                connection = DriverManager.getConnection(url, username, password)

                createTables()
                true
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao conectar com MySQL para punições: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private fun createTables() {
        try {
            val statement = connection!!.createStatement()

            val punishmentsTable = """
                CREATE TABLE IF NOT EXISTS player_punishments (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    admin_uuid VARCHAR(36) NOT NULL,
                    admin_name VARCHAR(16) NOT NULL,
                    type ENUM('BAN', 'MUTE') NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at BIGINT,
                    status ENUM('ATIVO', 'EXPIRADO', 'DESBANIDO') DEFAULT 'ATIVO',
                    unbanned_by VARCHAR(36),
                    unbanned_at TIMESTAMP NULL,
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_player_name (player_name),
                    INDEX idx_status (status)
                )
            """.trimIndent()

            statement.executeUpdate(punishmentsTable)
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao criar tabelas de punições: ${e.message}")
            e.printStackTrace()
        }
    }

    fun addPunishment(punishment: Punishment): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync -1

                val statement = conn.prepareStatement(
                    """
                    INSERT INTO player_punishments
                    (player_uuid, player_name, admin_uuid, admin_name, type, reason, expires_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    java.sql.Statement.RETURN_GENERATED_KEYS
                )

                statement.setString(1, punishment.playerUUID.toString())
                statement.setString(2, punishment.playerName)
                statement.setString(3, punishment.adminUUID.toString())
                statement.setString(4, punishment.adminName)
                statement.setString(5, punishment.type.name)
                statement.setString(6, punishment.reason)
                statement.setLong(7, punishment.expiresAt)

                statement.executeUpdate()

                val generatedKeys = statement.generatedKeys
                val id = if (generatedKeys.next()) {
                    generatedKeys.getInt(1)
                } else {
                    -1
                }

                generatedKeys.close()
                statement.close()

                id
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao adicionar punição: ${e.message}")
                e.printStackTrace()
                -1
            }
        }
    }

    fun getPunishmentById(id: Int): CompletableFuture<Punishment?> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync null

                val statement = conn.prepareStatement(
                    """
                    SELECT * FROM player_punishments
                    WHERE id = ?
                    """
                )
                statement.setInt(1, id)

                val resultSet = statement.executeQuery()
                val punishment = if (resultSet.next()) {
                    resultSetToPunishment(resultSet)
                } else {
                    null
                }

                resultSet.close()
                statement.close()

                punishment
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao buscar punição por ID: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    fun getPlayerPunishments(playerName: String): CompletableFuture<List<Punishment>> {
        return CompletableFuture.supplyAsync {
            val punishments = mutableListOf<Punishment>()

            try {
                val conn = getConnection() ?: return@supplyAsync punishments

                val statement = conn.prepareStatement(
                    """
                    SELECT * FROM player_punishments
                    WHERE player_name = ?
                    ORDER BY created_at DESC
                    """
                )
                statement.setString(1, playerName)

                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    punishments.add(resultSetToPunishment(resultSet))
                }

                resultSet.close()
                statement.close()
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao buscar punições: ${e.message}")
                e.printStackTrace()
            }

            punishments
        }
    }

    fun getActivePunishment(playerUUID: UUID, type: PunishType): CompletableFuture<Punishment?> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync null

                val statement = conn.prepareStatement(
                    """
                    SELECT * FROM player_punishments
                    WHERE player_uuid = ? AND type = ? AND status = 'ATIVO'
                    ORDER BY created_at DESC LIMIT 1
                    """
                )
                statement.setString(1, playerUUID.toString())
                statement.setString(2, type.name)

                val resultSet = statement.executeQuery()
                val punishment = if (resultSet.next()) {
                    resultSetToPunishment(resultSet)
                } else {
                    null
                }

                resultSet.close()
                statement.close()

                punishment
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao buscar punição ativa: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    fun updatePunishmentStatus(id: Int, status: PunishStatus, unbannedBy: UUID? = null): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val sql = if (unbannedBy != null) {
                    """
                    UPDATE player_punishments
                    SET status = ?, unbanned_by = ?, unbanned_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """
                } else {
                    """
                    UPDATE player_punishments
                    SET status = ?
                    WHERE id = ?
                    """
                }

                val statement = conn.prepareStatement(sql)
                statement.setString(1, status.name)

                if (unbannedBy != null) {
                    statement.setString(2, unbannedBy.toString())
                    statement.setInt(3, id)
                } else {
                    statement.setInt(2, id)
                }

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao atualizar status da punição: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    fun updateExpiredPunishments(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync 0

                val statement = conn.prepareStatement(
                    """
                    UPDATE player_punishments
                    SET status = 'EXPIRADO'
                    WHERE status = 'ATIVO'
                    AND expires_at > 0
                    AND expires_at < ?
                    """
                )
                statement.setLong(1, System.currentTimeMillis())

                val updatedCount = statement.executeUpdate()
                statement.close()

                updatedCount
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao atualizar punições expiradas: ${e.message}")
                e.printStackTrace()
                0
            }
        }
    }

    private fun resultSetToPunishment(rs: ResultSet): Punishment {
        return Punishment(
            id = rs.getInt("id"),
            playerUUID = UUID.fromString(rs.getString("player_uuid")),
            playerName = rs.getString("player_name"),
            adminUUID = UUID.fromString(rs.getString("admin_uuid")),
            adminName = rs.getString("admin_name"),
            type = PunishType.valueOf(rs.getString("type")),
            reason = rs.getString("reason"),
            createdAt = rs.getTimestamp("created_at").time,
            expiresAt = rs.getLong("expires_at"),
            status = PunishStatus.valueOf(rs.getString("status"))
        )
    }

    private fun getConnection(): Connection? {
        try {
            if (connection == null || connection!!.isClosed) {
                connect().join()
            }
        } catch (e: Exception) {
            plugin.logger.severe("Erro na conexão de punições: ${e.message}")
            e.printStackTrace()
        }
        return connection
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao desconectar database de punições: ${e.message}")
        }
    }
}