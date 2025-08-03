package org.caique.lobbypluginv1.tagmanager

import org.caique.lobbypluginv1.Lobbypluginv1
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.CompletableFuture

class TagDatabase {

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
                plugin.logger.severe("Erro ao conectar com MySQL para tags: ${e.message}")
                false
            }
        }
    }

    private fun createTables() {
        try {
            val statement = connection!!.createStatement()

            val tagsTable = """
                CREATE TABLE IF NOT EXISTS player_tags (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) UNIQUE NOT NULL,
                    tag_id VARCHAR(32) NOT NULL DEFAULT 'membro',
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_uuid (player_uuid),
                    INDEX idx_tag (tag_id)
                )
            """.trimIndent()

            statement.executeUpdate(tagsTable)

        } catch (e: Exception) {
            plugin.logger.severe("Erro ao criar tabelas de tags: ${e.message}")
        }
    }

    fun getPlayerTag(playerUuid: UUID): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync "membro"

                val statement = conn.prepareStatement(
                    "SELECT tag_id FROM player_tags WHERE player_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())

                val resultSet = statement.executeQuery()
                val tagId = if (resultSet.next()) {
                    resultSet.getString("tag_id")
                } else {
                    "membro"
                }

                resultSet.close()
                statement.close()

                tagId
            } catch (e: Exception) {
                "membro"
            }
        }
    }

    fun setPlayerTag(playerUuid: UUID, tagId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "INSERT INTO player_tags (player_uuid, tag_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE tag_id = ?"
                )
                statement.setString(1, playerUuid.toString())
                statement.setString(2, tagId)
                statement.setString(3, tagId)

                val result = statement.executeUpdate() > 0
                statement.close()

                result
            } catch (e: Exception) {
                false
            }
        }
    }

    fun hasPlayerRecord(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val conn = getConnection() ?: return@supplyAsync false

                val statement = conn.prepareStatement(
                    "SELECT COUNT(*) FROM player_tags WHERE player_uuid = ?"
                )
                statement.setString(1, playerUuid.toString())

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
            plugin.logger.severe("Erro na conexão de tags: ${e.message}")
        }
        return connection
    }

    fun disconnect() {
        try {
            connection?.close()
        } catch (e: Exception) {
            plugin.logger.severe("Erro ao desconectar database de tags: ${e.message}")
        }
    }
}