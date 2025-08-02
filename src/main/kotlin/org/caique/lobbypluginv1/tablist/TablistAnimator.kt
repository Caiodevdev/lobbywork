package org.caique.lobbypluginv1.tablist

object TablistAnimator {

    fun getAnimatedTitle(ticks: Int): String {
        val text = "MineFrex"
        val baseColor = "§6"
        val highlightColor = "§e"
        val speed = 10

        val position = (ticks / speed) % (text.length + 2)
        val result = StringBuilder()

        for (i in text.indices) {
            val char = text[i]
            result.append(
                if (position == i) {
                    "$highlightColor$char"
                } else {
                    "$baseColor$char"
                }
            )
        }

        return result.toString()
    }

    fun getHeaderContent(animatedTitle: String): String {
        return "\n$animatedTitle\n\n§fWebsite: §bminefrex.com.br\n§fDiscord: §bdiscord.gg/minefrex\n"
    }

    fun getFooterContent(): String {
        return "\n§7Servidor em desenvolvimento.\n"
    }
}