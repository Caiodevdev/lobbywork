package org.caique.lobbypluginv1.auth

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.caique.lobbypluginv1.Lobbypluginv1
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class AuthManager {

    private val plugin = Lobbypluginv1.instance
    private val databaseManager = DatabaseManager()
    private val authRepository = AuthRepository(databaseManager)

    // Sessões ativas dos jogadores
    private val activeSessions = ConcurrentHashMap<UUID, AuthSession>()

    // Timers de logout automático
    private val authTimers = ConcurrentHashMap<UUID, BukkitRunnable>()

    fun initialize() {
        databaseManager.connect().thenAccept { success ->
            if (success) {
                plugin.logger.info("Sistema de autenticação inicializado com sucesso!")
            } else {
                plugin.logger.severe("Falha ao inicializar sistema de autenticação!")
            }
        }
    }

    fun shutdown() {
        authTimers.values.forEach { it.cancel() }
        authTimers.clear()
        activeSessions.clear()
        databaseManager.disconnect()
    }

    fun handlePlayerJoin(player: Player) {
        val uuid = player.uniqueId

        // Aplica efeitos visuais
        applyLoginEffects(player)

        // Verifica se o jogador está registrado
        authRepository.isPlayerRegistered(uuid).thenAccept { isRegistered ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (isRegistered) {
                    startLoginProcess(player)
                } else {
                    startRegistrationProcess(player)
                }
            })
        }
    }

    private fun startLoginProcess(player: Player) {
        val session = AuthSession(player.uniqueId, false)
        activeSessions[player.uniqueId] = session

        // Envia mensagens de boas-vindas
        sendLoginMessages(player)

        // Inicia timer de 60 segundos
        startAuthTimer(player)
    }

    private fun startRegistrationProcess(player: Player) {
        val session = AuthSession(player.uniqueId, false)
        activeSessions[player.uniqueId] = session

        // Envia mensagens de registro
        sendRegistrationMessages(player)

        // Inicia timer de 60 segundos
        startAuthTimer(player)
    }

    private fun applyLoginEffects(player: Player) {
        // Efeito de cegueira
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 20 * 60, 1, false, false))

        // Modo de jogo para espectador temporário
        player.gameMode = GameMode.ADVENTURE

        // Limpa inventário
        player.inventory.clear()

        // Remove experiência
        player.exp = 0f
        player.level = 0

        // Som de entrada
        player.playSound(player.location, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.0f)
    }

    private fun sendLoginMessages(player: Player) {
        player.sendMessage("")
        player.sendMessage("                    §6§lBEM-VINDO DE VOLTA!")
        player.sendMessage("")
        player.sendMessage("        §7Olá §f${player.name}§7, você precisa fazer login")
        player.sendMessage("        §7para continuar jogando no servidor.")
        player.sendMessage("")
        player.sendMessage("        §e⚡ Use: §f/login <sua_senha>")
        player.sendMessage("        §e⏰ Tempo limite: §c60 segundos")
        player.sendMessage("")

        // Title
        player.sendTitle("§6§lAUTENTICAÇÃO", "§7Digite /login <senha>", 10, 70, 20)
    }

    private fun sendRegistrationMessages(player: Player) {
        player.sendMessage("")
        player.sendMessage("                    §a§lPRIMEIRO ACESSO!")
        player.sendMessage("")
        player.sendMessage("        §7Olá §f${player.name}§7, seja bem-vindo!")
        player.sendMessage("        §7Você precisa criar uma conta para jogar.")
        player.sendMessage("")
        player.sendMessage("        §e⚡ Use: §f/register <senha> <confirmar_senha>")
        player.sendMessage("        §e⏰ Tempo limite: §c60 segundos")
        player.sendMessage("")
        player.sendMessage("        §c⚠ §7Sua senha deve ter pelo menos §f6 caracteres")
        player.sendMessage("        §c⚠ §7e conter §fletras §7e §fnúmeros§7!")
        player.sendMessage("")

        // Title
        player.sendTitle("§a§lREGISTRO", "§7Digite /register <senha> <senha>", 10, 70, 20)
    }

    private fun startAuthTimer(player: Player) {
        val timer = object : BukkitRunnable() {
            var seconds = 60

            override fun run() {
                if (!player.isOnline) {
                    cancel()
                    return
                }

                if (isPlayerAuthenticated(player.uniqueId)) {
                    cancel()
                    return
                }

                if (seconds <= 0) {
                    kickPlayerTimeout(player)
                    cancel()
                    return
                }

                // Envia actionbar com countdown
                when {
                    seconds > 30 -> player.sendActionBar("§e⏰ Tempo para autenticação: §f${seconds}s")
                    seconds > 10 -> player.sendActionBar("§6⏰ Tempo para autenticação: §f${seconds}s")
                    else -> player.sendActionBar("§c⏰ URGENTE! Tempo para autenticação: §f${seconds}s")
                }

                // Som de alerta nos últimos 10 segundos
                if (seconds <= 10) {
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 2.0f)
                }

                seconds--
            }
        }

        timer.runTaskTimer(plugin, 0L, 20L)
        authTimers[player.uniqueId] = timer
    }

    private fun kickPlayerTimeout(player: Player) {
        player.kickPlayer(
            "\n§c§lTEMPO ESGOTADO!\n\n" +
                    "§7Você não conseguiu se autenticar\n" +
                    "§7dentro do tempo limite de §f60 segundos§7.\n\n" +
                    "§7Tente novamente em alguns segundos.\n"
        )
        cleanup(player.uniqueId)
    }

    fun attemptLogin(player: Player, password: String): Boolean {
        val session = activeSessions[player.uniqueId] ?: return false

        if (session.isAuthenticated) {
            player.sendMessage("§c✗ Você já está autenticado!")
            return false
        }

        // Incrementa tentativas
        val updatedSession = session.copy(
            loginAttempts = session.loginAttempts + 1,
            lastAttempt = System.currentTimeMillis()
        )
        activeSessions[player.uniqueId] = updatedSession

        if (updatedSession.loginAttempts > 3) {
            player.kickPlayer(
                "\n§c§lMUITAS TENTATIVAS!\n\n" +
                        "§7Você excedeu o limite de tentativas\n" +
                        "§7de login. Tente novamente mais tarde.\n"
            )
            cleanup(player.uniqueId)
            return false
        }

        authRepository.authenticatePlayer(player.uniqueId, password).thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    onSuccessfulLogin(player)
                } else {
                    onFailedLogin(player, updatedSession.loginAttempts)
                }
            })
        }

        return true
    }

    fun attemptRegister(player: Player, password: String, confirmPassword: String): Boolean {
        val session = activeSessions[player.uniqueId] ?: return false

        if (session.isAuthenticated) {
            player.sendMessage("§c✗ Você já está autenticado!")
            return false
        }

        // Validações
        if (password != confirmPassword) {
            player.sendMessage("§c✗ As senhas não coincidem!")
            player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f)
            return false
        }

        if (!PasswordUtils.isPasswordStrong(password)) {
            val strength = PasswordUtils.getPasswordStrength(password)
            player.sendMessage("§c✗ Sua senha é muito ${strength.color}${strength.displayName.lowercase()}!")
            player.sendMessage("§7Use pelo menos §f6 caracteres§7, com §fletras §7e §fnúmeros§7.")
            player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f)
            return false
        }

        val ipAddress = player.address?.address?.hostAddress ?: "unknown"

        authRepository.registerPlayer(player.uniqueId, player.name, password, ipAddress).thenAccept { success ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (success) {
                    onSuccessfulRegistration(player)
                } else {
                    onFailedRegistration(player)
                }
            })
        }

        return true
    }

    private fun onSuccessfulLogin(player: Player) {
        // Atualiza sessão
        activeSessions[player.uniqueId] = activeSessions[player.uniqueId]!!.copy(isAuthenticated = true)

        // Cancela timer
        authTimers[player.uniqueId]?.cancel()
        authTimers.remove(player.uniqueId)

        // Remove efeitos
        removeLoginEffects(player)

        // Mensagens de sucesso
        player.sendMessage("")
        player.sendMessage("§a✓ Login realizado com sucesso!")
        player.sendMessage("§7Bem-vindo de volta, §f${player.name}§7!")
        player.sendMessage("")

        // Title de sucesso
        player.sendTitle("§a§lSUCESSO!", "§7Login realizado com sucesso", 10, 40, 10)

        // Som de sucesso
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)

        // ActionBar
        player.sendActionBar("§a✓ Autenticado com sucesso!")
    }

    private fun onSuccessfulRegistration(player: Player) {
        // Atualiza sessão
        activeSessions[player.uniqueId] = activeSessions[player.uniqueId]!!.copy(isAuthenticated = true)

        // Cancela timer
        authTimers[player.uniqueId]?.cancel()
        authTimers.remove(player.uniqueId)

        // Remove efeitos
        removeLoginEffects(player)

        // Mensagens de sucesso
        player.sendMessage("")
        player.sendMessage("§a✓ Conta criada com sucesso!")
        player.sendMessage("§7Sua conta foi registrada e você já está logado!")
        player.sendMessage("§7Guarde bem sua senha para próximos acessos.")
        player.sendMessage("")

        // Title de sucesso
        player.sendTitle("§a§lBEM-VINDO!", "§7Conta criada com sucesso", 10, 40, 10)

        // Som de sucesso
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)

        // ActionBar
        player.sendActionBar("§a✓ Conta criada e autenticado!")
    }

    private fun onFailedLogin(player: Player, attempts: Int) {
        val remaining = 3 - attempts

        player.sendMessage("§c✗ Senha incorreta!")
        if (remaining > 0) {
            player.sendMessage("§7Tentativas restantes: §f$remaining")
        }

        player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f)
        player.sendActionBar("§c✗ Senha incorreta! Tentativas restantes: $remaining")
    }

    private fun onFailedRegistration(player: Player) {
        player.sendMessage("§c✗ Erro ao criar conta! Tente novamente.")
        player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f)
    }

    private fun removeLoginEffects(player: Player) {
        // Remove cegueira
        player.removePotionEffect(PotionEffectType.BLINDNESS)

        // Restaura modo de jogo
        player.gameMode = GameMode.SURVIVAL
    }

    fun isPlayerAuthenticated(uuid: UUID): Boolean {
        return activeSessions[uuid]?.isAuthenticated == true
    }

    fun cleanup(uuid: UUID) {
        activeSessions.remove(uuid)
        authTimers[uuid]?.cancel()
        authTimers.remove(uuid)
    }
}