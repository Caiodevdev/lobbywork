package org.caique.lobbypluginv1

import org.bukkit.plugin.java.JavaPlugin
import org.caique.lobbypluginv1.auth.AuthManager
import org.caique.lobbypluginv1.auth.AuthListener
import org.caique.lobbypluginv1.auth.LoginCommand
import org.caique.lobbypluginv1.auth.RegisterCommand
import org.caique.lobbypluginv1.scoreboard.ScoreboardManager
import org.caique.lobbypluginv1.scoreboard.ScoreboardListener
import org.caique.lobbypluginv1.chatmanager.ChatManager
import org.caique.lobbypluginv1.chatmanager.ChatListener
import org.caique.lobbypluginv1.tablist.TablistManager
import org.caique.lobbypluginv1.tablist.TablistListener

class Lobbypluginv1 : JavaPlugin() {

    private val pluginName = "LobbyPlugin"
    private val version = "1.0.0"
    private val author = "Caique"

    private lateinit var authManager: AuthManager
    private lateinit var scoreboardManager: ScoreboardManager
    private lateinit var chatManager: ChatManager
    private lateinit var tablistManager: TablistManager

    override fun onEnable() {
        logger.info("Iniciando $pluginName v$version...")

        initializeAuthSystem()
        initializeScoreboardSystem()
        initializeChatSystem()
        initializeTablistSystem()
        registerCommands()
        registerEvents()

        logger.info("$pluginName v$version foi carregado com sucesso!")
        logger.info("Sistema de autenticação ativo!")
        logger.info("Sistema de scoreboard ativo!")
        logger.info("Sistema de chat personalizado ativo!")
        logger.info("Sistema de tablist personalizada ativo!")
        logger.info("Desenvolvido por $author")
    }

    override fun onDisable() {
        logger.info("Desligando $pluginName v$version...")

        if (::tablistManager.isInitialized) {
            tablistManager.shutdown()
        }

        if (::chatManager.isInitialized) {
            chatManager.shutdown()
        }

        if (::scoreboardManager.isInitialized) {
            scoreboardManager.shutdown()
        }

        if (::authManager.isInitialized) {
            authManager.shutdown()
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

    private fun initializeChatSystem() {
        chatManager = ChatManager()
        chatManager.initialize()
    }

    private fun initializeTablistSystem() {
        tablistManager = TablistManager()
        tablistManager.initialize()
    }

    private fun registerCommands() {
        getCommand("login")?.setExecutor(LoginCommand(authManager))
        getCommand("register")?.setExecutor(RegisterCommand(authManager))
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(AuthListener(authManager), this)
        server.pluginManager.registerEvents(ScoreboardListener(scoreboardManager), this)
        server.pluginManager.registerEvents(ChatListener(chatManager), this)
        server.pluginManager.registerEvents(TablistListener(tablistManager), this)
    }

    fun getAuthManager(): AuthManager = authManager
    fun getScoreboardManager(): ScoreboardManager = scoreboardManager
    fun getChatManager(): ChatManager = chatManager
    fun getTablistManager(): TablistManager = tablistManager

    companion object {
        lateinit var instance: Lobbypluginv1
            private set

        fun getAuthManager(): AuthManager {
            return instance.authManager
        }

        fun getScoreboardManager(): ScoreboardManager {
            return instance.scoreboardManager
        }

        fun getChatManager(): ChatManager {
            return instance.chatManager
        }

        fun getTablistManager(): TablistManager {
            return instance.tablistManager
        }
    }

    init {
        instance = this
    }
}