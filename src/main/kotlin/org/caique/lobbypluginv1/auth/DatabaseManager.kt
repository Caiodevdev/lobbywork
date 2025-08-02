package org.caique.lobbypluginv1.auth

import org.bukkit.Bukkit
import org.caique.lobbypluginv1.Lobbypluginv1
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

class DatabaseManager {

    private var connection: Connection? = null
    private val plugin = Lobbypluginv1.instance

    // Configurações do banco
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

                plugin.logger.info("Conectado ao MySQL com sucesso!")
                createTables()
                true

            } catch (e: Exception) {
                plugin.logger.severe("Erro ao conectar com o MySQL: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private fun createTables() {
        try {
            val statement = connection!!.createStatement()

            val createTable = """
                CREATE TABLE IF NOT EXISTS player_auth (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) UNIQUE NOT NULL,
                    username VARCHAR(16) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    ip_address VARCHAR(45),
                    INDEX idx_uuid (uuid),
                    INDEX idx_username (username)
                )
            """.trimIndent()

            statement.executeUpdate(createTable)
            plugin.logger.info("Tabela 'player_auth' verificada/criada com sucesso!")

        } catch (e: SQLException) {
            plugin.logger.severe("Erro ao criar tabelas: ${e.message}")
            e.printStackTrace()
        }
    }

    fun getConnection(): Connection? {
        try {
            if (connection == null || connection!!.isClosed) {
                connect().join()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Erro ao verificar conexão: ${e.message}")
        }
        return connection
    }

    fun disconnect() {
        try {
            connection?.close()
            plugin.logger.info("Desconectado do MySQL!")
        } catch (e: SQLException) {
            plugin.logger.severe("Erro ao desconectar do MySQL: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return try {
            connection != null && !connection!!.isClosed
        } catch (e: SQLException) {
            false
        }
    }
}