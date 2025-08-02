package org.caique.lobbypluginv1.scoreboard

import org.bukkit.Bukkit
import org.bukkit.entity.Player

fun Player.sendActionBar(message: String) {
    try {
        if (hasMethod("sendActionBar", String::class.java)) {
            val method = this.javaClass.getMethod("sendActionBar", String::class.java)
            method.invoke(this, message)
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "title ${this.name} actionbar {\"text\":\"$message\"}")
        }
    } catch (e: Exception) {
        this.sendMessage("§8[§eInfo§8] $message")
    }
}

private fun Player.hasMethod(methodName: String, vararg parameterTypes: Class<*>): Boolean {
    return try {
        this.javaClass.getMethod(methodName, *parameterTypes)
        true
    } catch (e: NoSuchMethodException) {
        false
    }
}