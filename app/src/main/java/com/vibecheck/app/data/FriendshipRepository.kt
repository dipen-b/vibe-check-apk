package com.vibecheck.app.data

import com.vibecheck.app.core.model.FriendRequest
import com.vibecheck.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface FriendshipRepository {
    // Phone & Auth
    suspend fun sendOTP(phoneNumber: String, countryCode: String): Result<String>
    suspend fun verifyOTP(phoneNumber: String, otp: String): Result<String> // Returns userId

    // Profile
    suspend fun createUserProfile(userId: String, firstName: String, lastName: String, avatarUri: String? = null): Result<User>
    suspend fun getUserProfile(userId: String): Result<User>

    // Friend Search
    suspend fun searchUsers(query: String): Result<List<User>>

    // Friend Requests
    suspend fun sendFriendRequest(receiverId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>

    // Friends List
    fun getFriendsList(): Flow<List<User>>
    fun getPendingRequests(): Flow<List<FriendRequest>>
    fun getSentRequests(): Flow<List<FriendRequest>>

    // Current User
    fun getCurrentUserId(): Flow<String?>
}
