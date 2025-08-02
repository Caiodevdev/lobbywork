package org.caique.lobbypluginv1.auth

import org.caique.lobbypluginv1.Lobbypluginv1
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.CompletableFuture

class AuthRepository(private val databaseManager: DatabaseManager) {

    private val plugin = Lobbypluginv1.instance

    fun isPlayerRegistered(uuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val connection = databaseManager.getConnection() ?: return@supplyAsync false

                val statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM player_auth WHERE uuid = ?"
                )
                statement.setString(1, uuid.toString())

                val resultSet = statement.executeQuery()
                resultSet.next()
                val count = resultSet.getInt(1)

                resultSet.close()
                statement.close()

                count > 0
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao verificar registro do jogador: ${e.message}")
                false
            }
        }
    }

    fun registerPlayer(uuid: UUID, username: String, password: String, ipAddress: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val connection = databaseManager.getConnection() ?: return@supplyAsync false

                val hashedPassword = PasswordUtils.hashPasswordWithSalt(password)

                val statement = connection.prepareStatement(
                    "INSERT INTO player_auth (uuid, username, password, ip_address) VALUES (?, ?, ?, ?)"
                )

                statement.setString(1, uuid.toString())
                statement.setString(2, username)
                statement.setString(3, hashedPassword)
                statement.setString(4, ipAddress)

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao registrar jogador: ${e.message}")
                false
            }
        }
    }

    fun authenticatePlayer(uuid: UUID, password: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val connection = databaseManager.getConnection() ?: return@supplyAsync false

                val statement = connection.prepareStatement(
                    "SELECT password FROM player_auth WHERE uuid = ?"
                )
                statement.setString(1, uuid.toString())

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val storedPassword = resultSet.getString("password")
                    val isValid = PasswordUtils.verifyPassword(password, storedPassword)

                    if (isValid) {
                        updateLastLogin(uuid)
                    }

                    resultSet.close()
                    statement.close()

                    isValid
                } else {
                    resultSet.close()
                    statement.close()
                    false
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao autenticar jogador: ${e.message}")
                false
            }
        }
    }

    private fun updateLastLogin(uuid: UUID) {
        try {
            val connection = databaseManager.getConnection() ?: return

            val statement = connection.prepareStatement(
                "UPDATE player_auth SET last_login = ? WHERE uuid = ?"
            )
            statement.setTimestamp(1, Timestamp(System.currentTimeMillis()))
            statement.setString(2, uuid.toString())

            statement.executeUpdate()
            statement.close()
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao atualizar último login: ${e.message}")
        }
    }

    fun getPlayerData(uuid: UUID): CompletableFuture<PlayerData?> {
        return CompletableFuture.supplyAsync {
            try {
                val connection = databaseManager.getConnection() ?: return@supplyAsync null

                val statement = connection.prepareStatement(
                    "SELECT * FROM player_auth WHERE uuid = ?"
                )
                statement.setString(1, uuid.toString())

                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val playerData = PlayerData(
                        uuid = UUID.fromString(resultSet.getString("uuid")),
                        username = resultSet.getString("username"),
                        password = resultSet.getString("password"),
                        registeredAt = resultSet.getTimestamp("registered_at"),
                        lastLogin = resultSet.getTimestamp("last_login"),
                        ipAddress = resultSet.getString("ip_address")
                    )

                    resultSet.close()
                    statement.close()

                    playerData
                } else {
                    resultSet.close()
                    statement.close()
                    null
                }
            } catch (e: Exception) {
                plugin.logger.severe("Erro ao buscar dados do jogador: ${e.message}")
                null
            }
        }
    }
}