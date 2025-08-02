package org.caique.lobbypluginv1.friends

import org.caique.lobbypluginv1.Lobbypluginv1
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.CompletableFuture

class FriendsDatabase {

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
                plugin.logger.severe("Erro ao conectar com MySQL para amigos: ${e.message}")
                false
            }
        }
    }

    private fun createTables() {
        try {
            val statement = connection!!.createStatement()

            val friendsTable = """
                CREATE TABLE IF NOT EXISTS player_friends (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    friend_uuid VARCHAR(36) NOT NULL,
                    friend_name VARCHAR(16) NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_friendship (player_uuid, friend_uuid),
                    INDEX idx_player (player_uuid),
                    INDEX idx_friend (friend_uuid)
                )
            """.trimIndent()

            val requestsTable = """
                CREATE TABLE IF NOT EXISTS friend_requests (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender_uuid VARCHAR(36) NOT NULL,
                    sender_name VARCHAR(16) NOT NULL,
                    receiver_uuid VARCHAR(36) NOT NULL,
                    receiver_name VARCHAR(16) NOT NULL,
                    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_request (sender_uuid, receiver_uuid),
                    INDEX idx_receiver (receiver_uuid)
                )
            """.trimIndent()

            statement.executeUpdate(friendsTable)
            statement.executeUpdate(requestsTable)

        } catch (e: Exception) {
            plugin.logger.severe("Erro ao criar tabelas de amigos: ${e.message}")
        }
    }

    fun addFriend(playerUuid: UUID, friendUuid: UUID, friendName: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "INSERT INTO player_friends (player_uuid, friend_uuid, friend_name) VALUES (?, ?, ?)"
                )
                statement.setString(1, playerUuid.toString())
                statement.setString(2, friendUuid.toString())
                statement.setString(3, friendName)

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                false
            }
        }
    }

    fun removeFriend(playerUuid: UUID, friendUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "DELETE FROM player_friends WHERE player_uuid = ? AND friend_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())
                statement.setString(2, friendUuid.toString())

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getFriends(playerUuid: UUID): CompletableFuture<List<FriendData>> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync emptyList()

                val statement = conn.prepareStatement(
                    "SELECT friend_uuid, friend_name, added_at FROM player_friends WHERE player_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())

                val resultSet = statement.executeQuery()
                val friends = mutableListOf<FriendData>()

                while (resultSet.next()) {
                    val friendUuid = UUID.fromString(resultSet.getString("friend_uuid"))
                    val friendName = resultSet.getString("friend_name")
                    val addedAt = resultSet.getTimestamp("added_at")

                    friends.add(FriendData(friendUuid, friendName, addedAt))
                }

                resultSet.close()
                statement.close()

                friends
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun areFriends(playerUuid: UUID, friendUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "SELECT COUNT(*) FROM player_friends WHERE player_uuid = ? AND friend_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())
                statement.setString(2, friendUuid.toString())

                val resultSet = statement.executeQuery()
                resultSet.next()
                val count = resultSet.getInt(1)

                resultSet.close()
                statement.close()

                count > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    fun addFriendRequest(request: FriendRequest): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "INSERT INTO friend_requests (sender_uuid, sender_name, receiver_uuid, receiver_name) VALUES (?, ?, ?, ?)"
                )
                statement.setString(1, request.senderUuid.toString())
                statement.setString(2, request.senderName)
                statement.setString(3, request.receiverUuid.toString())
                statement.setString(4, request.receiverName)

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                false
            }
        }
    }

    fun removeFriendRequest(senderUuid: UUID, receiverUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "DELETE FROM friend_requests WHERE sender_uuid = ? AND receiver_uuid = ?"
                )
                statement.setString(1, senderUuid.toString())
                statement.setString(2, receiverUuid.toString())

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getPendingRequests(playerUuid: UUID): CompletableFuture<List<FriendRequest>> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync emptyList()

                val statement = conn.prepareStatement(
                    "SELECT sender_uuid, sender_name, receiver_name, sent_at FROM friend_requests WHERE receiver_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())

                val resultSet = statement.executeQuery()
                val requests = mutableListOf<FriendRequest>()

                while (resultSet.next()) {
                    val senderUuid = UUID.fromString(resultSet.getString("sender_uuid"))
                    val senderName = resultSet.getString("sender_name")
                    val receiverName = resultSet.getString("receiver_name")
                    val sentAt = resultSet.getTimestamp("sent_at")

                    requests.add(FriendRequest(senderUuid, senderName, playerUuid, receiverName, sentAt))
                }

                resultSet.close()
                statement.close()

                requests
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun hasRequest(senderUuid: UUID, receiverUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "SELECT COUNT(*) FROM friend_requests WHERE sender_uuid = ? AND receiver_uuid = ?"
                )
                statement.setString(1, senderUuid.toString())
                statement.setString(2, receiverUuid.toString())

                val resultSet = statement.executeQuery()
                resultSet.next()
                val count = resultSet.getInt(1)

                resultSet.close()
                statement.close()

                count > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun getConnection(): Connection? {
        try {
            if (connection == null || connection!!.isClosed) {
                connect().join()
            }
        } catch (e: Exception) {
            plugin.logger.severe("Erro na conexão de amigos: ${e.message}")
        }
        return connection
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao desconectar database de amigos: ${e.message}")
        }
    }
}