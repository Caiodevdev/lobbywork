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
import org.caique.lobbypluginv1.friends.FriendsManager
import org.caique.lobbypluginv1.friends.FriendsListener
import org.caique.lobbypluginv1.friends.FriendsCommand
import org.caique.lobbypluginv1.tagmanager.TagManager
import org.caique.lobbypluginv1.tagmanager.TagListener
import org.caique.lobbypluginv1.tagmanager.TagsCommand
import org.caique.lobbypluginv1.tagmanager.TagCommand
import org.caique.lobbypluginv1.tagmanager.NametagTestCommand


class Lobbypluginv1 : JavaPlugin() {

    private val pluginName = "LobbyPlugin"
    private val version = "1.0.0"
    private val author = "Caique"

    private lateinit var authManager: AuthManager
    private lateinit var scoreboardManager: ScoreboardManager
    private lateinit var chatManager: ChatManager
    private lateinit var tablistManager: TablistManager
    private lateinit var friendsManager: FriendsManager
    private lateinit var tagManager: TagManager

    override fun onEnable() {
        logger.info("Iniciando $pluginName v$version...")

        initializeAuthSystem()
        initializeScoreboardSystem()
        initializeChatSystem()
        initializeTablistSystem()
        initializeFriendsSystem()
        initializeTagSystem()
        registerCommands()
        registerEvents()

        logger.info("$pluginName v$version foi carregado com sucesso!")
        logger.info("Desenvolvido por $author")
    }

    override fun onDisable() {
        logger.info("Desligando $pluginName v$version...")

        if (::tagManager.isInitialized) {
            tagManager.shutdown()
        }

        if (::friendsManager.isInitialized) {
            friendsManager.shutdown()
        }

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

    private fun initializeFriendsSystem() {
        friendsManager = FriendsManager()
        friendsManager.initialize()
    }

    private fun initializeTagSystem() {
        tagManager = TagManager()
        tagManager.initialize()
    }

    private fun registerCommands() {
        getCommand("login")?.setExecutor(LoginCommand(authManager))
        getCommand("register")?.setExecutor(RegisterCommand(authManager))
        getCommand("amigos")?.setExecutor(FriendsCommand(friendsManager))
        getCommand("tags")?.setExecutor(TagsCommand(tagManager))
        getCommand("tag")?.setExecutor(TagCommand(tagManager))
        getCommand("nametagtest")?.setExecutor(NametagTestCommand(tagManager))
    }

    private fun registerEvents() {
        server.pluginManager.registerEvents(AuthListener(authManager), this)
        server.pluginManager.registerEvents(ScoreboardListener(scoreboardManager), this)
        server.pluginManager.registerEvents(ChatListener(chatManager), this)
        server.pluginManager.registerEvents(TablistListener(tablistManager), this)
        server.pluginManager.registerEvents(FriendsListener(friendsManager), this)
        server.pluginManager.registerEvents(TagListener(tagManager), this)
    }

    fun getAuthManager(): AuthManager = authManager
    fun getScoreboardManager(): ScoreboardManager = scoreboardManager
    fun getChatManager(): ChatManager = chatManager
    fun getTablistManager(): TablistManager = tablistManager
    fun getFriendsManager(): FriendsManager = friendsManager
    fun getTagManager(): TagManager = tagManager

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

        fun getFriendsManager(): FriendsManager {
            return instance.friendsManager
        }

        fun getTagManager(): TagManager {
            return instance.tagManager
        }
    }

    init {
        instance = this
    }
}