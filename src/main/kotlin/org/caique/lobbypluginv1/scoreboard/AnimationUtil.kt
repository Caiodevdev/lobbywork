package org.caique.lobbypluginv1.scoreboard

object AnimationUtil {

    fun getAnimatedTitle(ticks: Int): String {
        val text = "MineFrex"
        val baseColor = "§6§l"
        val highlightColor = "§e§l"
        val speed = 8

        val position = (ticks / speed) % (text.length * 2)
        val result = StringBuilder()

        for (i in text.indices) {
            val char = text[i]
            result.append(
                if (position == i || position == i + text.length) {
                    "$highlightColor$char"
                } else {
                    "$baseColor$char"
                }
            )
        }

        return result.toString()
    }

    fun getAnimatedWebsite(ticks: Int): String {
        val text = "minefrex.com.br"
        val baseColor = "§6§l"
        val highlightColor = "§e§l"
        val speed = 6

        val position = (ticks / speed) % (text.length * 2)
        val result = StringBuilder()

        for (i in text.indices) {
            val char = text[i]
            result.append(
                if (position == i || position == i + text.length) {
                    "$highlightColor$char"
                } else {
                    "$baseColor$char"
                }
            )
        }

        return result.toString()
    }

    fun getPlayerCount(): Int {
        return org.bukkit.Bukkit.getOnlinePlayers().size
    }

    fun getAuthStatus(playerUuid: java.util.UUID): String {
        val authManager = org.caique.lobbypluginv1.Lobbypluginv1.getAuthManager()
        return if (authManager.isPlayerAuthenticated(playerUuid)) {
            "§aAutenticado"
        } else {
            "§cNão autenticado"
        }
    }
}