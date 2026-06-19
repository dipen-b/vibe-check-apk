package com.vibecheck.app.core.model

data class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "",
    val avatarUrl: String = "",
    val createdAtMillis: Long = 0,
)

data class FriendRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderFirstName: String = "",
    val senderLastName: String = "",
    val senderAvatarUrl: String = "",
    val receiverId: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAtMillis: Long = 0,
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
}
