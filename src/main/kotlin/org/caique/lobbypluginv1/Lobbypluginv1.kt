package org.caique.lobbypluginv1

import org.bukkit.plugin.java.JavaPlugin
import org.caique.lobbypluginv1.auth.AuthManager
import org.caique.lobbypluginv1.auth.AuthListener
import org.caique.lobbypluginv1.auth.LoginCommand
import org.caique.lobbypluginv1.auth.RegisterCommand
import org.caique.lobbypluginv1.scoreboard.ScoreboardManager
import org.caique.lobbypluginv1.scoreboard.ScoreboardListener

class Lobbypluginv1 : JavaPlugin() {

    private val pluginName = "LobbyPlugin"
    private val version = "1.0.0"
    private val author = "Caique"

    private lateinit var authManager: AuthManager
    private lateinit var scoreboardManager: ScoreboardManager

    override fun onEnable() {
        logger.info("Iniciando $pluginName v$version...")

        initializeAuthSystem()
        initializeScoreboardSystem()
        registerCommands()
        registerEvents()

        logger.info("$pluginName v$version foi carregado com sucesso!")
        logger.info("Sistema de autenticação ativo!")
        logger.info("Sistema de scoreboard ativo!")
        logger.info("Desenvolvido por $author")
    }

    override fun onDisable() {
        logger.info("Desligando $pluginName v$version...")

        if (::scoreboardManager.isInitialized) {
            scoreboardManager.shutdown()
            logger.info("Sistema de scoreboard finalizado!")
        }

        if (::authManager.isInitialized) {
            authManager.shutdown()
            logger.info("Sistema de autenticação finalizado!")
        }

        logger.info("$pluginName v$version foi descarregado!")
    }

    private fun initializeAuthSystem() {
        authManager = AuthManager()
        authManager.initialize()
    }

    private fun initializeScoreboardSystem() {
        scoreboardManager = ScoreboardManager()
        scoreboardManager.initialize()
    }

    private fun registerCommands() {
        getCommand("login")?.setExecutor(LoginCommand(authManager))
        getCommand("register")?.setExecutor(RegisterCommand(authManager))

        logger.info("Comandos registrados:")
        logger.info("- /login <senha>")
        logger.info("- /register <senha> <confirmar_senha>")
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(AuthListener(authManager), this)
        server.pluginManager.registerEvents(ScoreboardListener(scoreboardManager), this)

        logger.info("Sistemas ativos:")
        logger.info("- Proteção de autenticação")
        logger.info("- Scoreboard animada")
        logger.info("- Integração auth + scoreboard")
    }

    fun getAuthManager(): AuthManager = authManager
    fun getScoreboardManager(): ScoreboardManager = scoreboardManager

    companion object {
        lateinit var instance: Lobbypluginv1
            private set

        fun getAuthManager(): AuthManager {
            return instance.authManager
        }

        fun getScoreboardManager(): ScoreboardManager {
            return instance.scoreboardManager
        }
    }

    init {
        instance = this
    }
}