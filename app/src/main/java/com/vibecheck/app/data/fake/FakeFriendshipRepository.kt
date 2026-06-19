package com.vibecheck.app.data.fake

import com.vibecheck.app.core.model.FriendRequest
import com.vibecheck.app.core.model.FriendRequestStatus
import com.vibecheck.app.core.model.User
import com.vibecheck.app.data.FriendshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FakeFriendshipRepository : FriendshipRepository {
    private val currentUserIdState = MutableStateFlow("user-123")
    private val usersMap = mutableMapOf(
        "user-123" to User(
            userId = "user-123",
            firstName = "John",
            lastName = "Doe",
            phoneNumber = "9876543210",
            countryCode = "+1",
            avatarUrl = "",
            createdAtMillis = System.currentTimeMillis(),
        ),
        "user-456" to User(
            userId = "user-456",
            firstName = "Jane",
            lastName = "Smith",
            phoneNumber = "8765432109",
            countryCode = "+1",
            avatarUrl = "",
            createdAtMillis = System.currentTimeMillis(),
        ),
        "user-789" to User(
            userId = "user-789",
            firstName = "Alex",
            lastName = "Johnson",
            phoneNumber = "7654321098",
            countryCode = "+44",
            avatarUrl = "",
            createdAtMillis = System.currentTimeMillis(),
        ),
    )

    private val friendRequestsMap = mutableMapOf<String, FriendRequest>()
    private val friendsListMap = mutableMapOf(
        "user-123" to mutableSetOf("user-456"),
    )

    override suspend fun sendOTP(phoneNumber: String, countryCode: String): Result<String> =
        Result.success(UUID.randomUUID().toString())

    override suspend fun verifyOTP(phoneNumber: String, otp: String): Result<String> =
        Result.success("user-123")

    override suspend fun createUserProfile(
        userId: String,
        firstName: String,
        lastName: String,
        avatarUri: String?,
    ): Result<User> {
        val user = User(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = "9876543210",
            countryCode = "+1",
            avatarUrl = avatarUri ?: "",
            createdAtMillis = System.currentTimeMillis(),
        )
        usersMap[userId] = user
        currentUserIdState.value = userId
        return Result.success(user)
    }

    override suspend fun getUserProfile(userId: String): Result<User> {
        val user = usersMap[userId] ?: return Result.failure(IllegalStateException("User not found"))
        return Result.success(user)
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        val results = usersMap.values.filter { user ->
            user.firstName.contains(query, ignoreCase = true) ||
                    user.lastName.contains(query, ignoreCase = true) ||
                    user.phoneNumber.contains(query, ignoreCase = true)
        }
        return Result.success(results)
    }

    override suspend fun sendFriendRequest(receiverId: String): Result<Unit> {
        val senderId = currentUserIdState.value ?: return Result.failure(IllegalStateException("Not authenticated"))
        val sender = usersMap[senderId] ?: return Result.failure(IllegalStateException("Sender not found"))

        val requestId = UUID.randomUUID().toString()
        val request = FriendRequest(
            requestId = requestId,
            senderId = senderId,
            senderFirstName = sender.firstName,
            senderLastName = sender.lastName,
            senderAvatarUrl = sender.avatarUrl,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAtMillis = System.currentTimeMillis(),
        )
        friendRequestsMap[requestId] = request
        return Result.success(Unit)
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        val request = friendRequestsMap[requestId] ?: return Result.failure(IllegalStateException("Request not found"))

        friendRequestsMap[requestId] = request.copy(status = FriendRequestStatus.ACCEPTED)

        friendsListMap.getOrPut(request.senderId) { mutableSetOf() }.add(request.receiverId)
        friendsListMap.getOrPut(request.receiverId) { mutableSetOf() }.add(request.senderId)

        return Result.success(Unit)
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        val request = friendRequestsMap[requestId] ?: return Result.failure(IllegalStateException("Request not found"))
        friendRequestsMap[requestId] = request.copy(status = FriendRequestStatus.REJECTED)
        return Result.success(Unit)
    }

    override fun getFriendsList(): Flow<List<User>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            friendsListMap[userId]?.mapNotNull { friendId ->
                usersMap[friendId]
            } ?: emptyList()
        }
    }

    override fun getPendingRequests(): Flow<List<FriendRequest>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            friendRequestsMap.values.filter { req ->
                req.receiverId == userId && req.status == FriendRequestStatus.PENDING
            }
        }
    }

    override fun getSentRequests(): Flow<List<FriendRequest>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            friendRequestsMap.values.filter { req ->
                req.senderId == userId && req.status == FriendRequestStatus.PENDING
            }
        }
    }

    override fun getCurrentUserId(): Flow<String?> = currentUserIdState
}
