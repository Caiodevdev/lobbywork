package org.caique.lobbypluginv1.friends

import java.util.*

data class FriendData(
    val uuid: UUID,
    val username: String,
    val addedAt: Date,
    var isOnline: Boolean = false,
    var server: String = "Offline"
)

data class FriendRequest(
    val senderUuid: UUID,
    val senderName: String,
    val receiverUuid: UUID,
    val receiverName: String,
    val sentAt: Date = Date()
)

enum class FriendStatus {
    ONLINE,
    OFFLINE,
    SAME_SERVER,
    DIFFERENT_SERVER
}