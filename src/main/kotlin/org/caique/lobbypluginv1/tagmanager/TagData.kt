package org.caique.lobbypluginv1.tagmanager

data class Tag(
    val id: String,
    val displayName: String,
    val color: String,
    val permission: String,
    val priority: Int
) {
    fun getFormattedTag(): String {
        return "$color[$displayName]"
    }

    fun getColoredName(): String {
        return "$color$displayName"
    }
}

enum class TagPosition {
    CHAT,
    TABLIST,
    NAMETAG
}

object TagRegistry {

    val tags = mapOf(
        "membro" to Tag("membro", "Membro", "§7", "lobby.tag.membro", 1),
        "ajudante" to Tag("ajudante", "Ajudante", "§e", "lobby.tag.ajudante", 2),
        "moderador" to Tag("moderador", "Moderador", "§a", "lobby.tag.moderador", 3),
        "admin" to Tag("admin", "Admin", "§c", "lobby.tag.admin", 4),
        "gerente" to Tag("gerente", "Gerente", "§4", "lobby.tag.gerente", 5),
        "master" to Tag("master", "Master", "§6", "lobby.tag.master", 6)
    )

    fun getTag(id: String): Tag? {
        return tags[id.lowercase()]
    }

    fun getAllTags(): List<Tag> {
        return tags.values.sortedBy { it.priority }
    }

    fun getDefaultTag(): Tag {
        return tags["membro"]!!
    }

    fun getHighestTag(permissions: List<String>): Tag {
        return tags.values
            .filter { tag -> permissions.any { it == tag.permission || it == "*" } }
            .maxByOrNull { it.priority } ?: getDefaultTag()
    }
}