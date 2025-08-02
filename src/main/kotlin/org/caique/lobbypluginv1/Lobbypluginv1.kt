package org.caique.lobbypluginv1

import org.bukkit.plugin.java.JavaPlugin
import org.caique.lobbypluginv1.auth.AuthManager
import org.caique.lobbypluginv1.auth.AuthListener
import org.caique.lobbypluginv1.auth.LoginCommand
import org.caique.lobbypluginv1.auth.RegisterCommand

class Lobbypluginv1 : JavaPlugin() {

    private val pluginName = "LobbyPlugin"
    private val version = "1.0.0"
    private val author = "Caique"

    // Sistema de autenticação
    private lateinit var authManager: AuthManager

    override fun onEnable() {
        logger.info("Iniciando $pluginName v$version...")

        // Inicializa sistema de autenticação
        authManager = AuthManager()
        authManager.initialize()

        // Registra comandos
        registerCommands()

        // Registra eventos
        registerEvents()

        logger.info("$pluginName v$version foi carregado com sucesso!")
        logger.info("Sistema de autenticação ativo!")
        logger.info("Desenvolvido por $author")
    }

    override fun onDisable() {
        logger.info("Desligando $pluginName v$version...")

        // Finaliza sistema de autenticação
        if (::authManager.isInitialized) {
            authManager.shutdown()
        }

        logger.info("$pluginName v$version foi descarregado!")
    }

    private fun registerCommands() {
        // Comandos de autenticação
        getCommand("login")?.setExecutor(LoginCommand(authManager))
        getCommand("register")?.setExecutor(RegisterCommand(authManager))

        logger.info("Comandos de autenticação registrados!")
        logger.info("- /login <senha>")
        logger.info("- /register <senha> <confirmar_senha>")
    }

    private fun registerEvents() {
        // Listener de autenticação
        server.pluginManager.registerEvents(AuthListener(authManager), this)

        logger.info("Sistema de proteção ativado!")
        logger.info("Jogadores precisam se autenticar para interagir")
    }

    // Getters para outras classes
    fun getAuthManager(): AuthManager = authManager

    // Método utilitário para outras classes acessarem a instância
    companion object {
        lateinit var instance: Lobbypluginv1
            private set

        fun getAuthManager(): AuthManager {
            return instance.authManager
        }
    }

    init {
        instance = this
    }
}