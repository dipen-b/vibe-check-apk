package com.vibecheck.app.data.real

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vibecheck.app.core.model.FriendRequest
import com.vibecheck.app.core.model.FriendRequestStatus
import com.vibecheck.app.core.model.User
import com.vibecheck.app.data.FriendshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RealFriendshipRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : FriendshipRepository {

    private val verificationIdState = MutableStateFlow<String?>(null)
    private val currentUserIdState = MutableStateFlow<String?>(null)

    init {
        currentUserIdState.value = auth.currentUser?.uid
    }

    override suspend fun sendOTP(phoneNumber: String, countryCode: String): Result<String> =
        runCatching {
            val fullPhoneNumber = "$countryCode$phoneNumber"
            val activity = context as? Activity ?: throw IllegalStateException("Context is not an Activity")

            val verificationId = suspendCancellableCoroutine<String> { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification on some devices (e.g., Google Play services)
                        // We'll let this be handled by the OTP step
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onCodeSent(
                        id: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        continuation.resume(id)
                    }
                }

                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(fullPhoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            }

            verificationIdState.value = verificationId
            verificationId
        }

    override suspend fun verifyOTP(phoneNumber: String, otp: String): Result<String> =
        runCatching {
            val verificationId = verificationIdState.value ?: throw IllegalStateException("OTP not sent. Please send OTP first.")
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val task = auth.signInWithCredential(credential).await()
            val uid = task.user?.uid ?: throw IllegalStateException("Authentication failed")
            currentUserIdState.value = uid
            uid
        }

    override suspend fun createUserProfile(
        userId: String,
        firstName: String,
        lastName: String,
        avatarUri: String?,
    ): Result<User> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val phoneNumber = user.phoneNumber ?: ""
        val countryCode = phoneNumber.takeWhile { it.isDigit() || it == '+' }

        var avatarUrl = ""
        if (avatarUri != null) {
            avatarUrl = uploadAvatar(userId, avatarUri)
        }

        val userProfile = User(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber.drop(countryCode.length),
            countryCode = countryCode,
            avatarUrl = avatarUrl,
            createdAtMillis = System.currentTimeMillis(),
        )

        firestore.collection("users").document(userId).set(userProfile).await()
        userProfile
    }

    override suspend fun getUserProfile(userId: String): Result<User> = runCatching {
        val doc = firestore.collection("users").document(userId).get().await()
        doc.toObject(User::class.java) ?: throw IllegalStateException("User not found")
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val snapshot = firestore.collection("users").get().await()
        snapshot.documents.mapNotNull { doc ->
            try {
                val user = doc.toObject(User::class.java) ?: return@mapNotNull null
                if (user.firstName.contains(query, ignoreCase = true) ||
                    user.lastName.contains(query, ignoreCase = true) ||
                    user.phoneNumber.contains(query, ignoreCase = true)
                ) {
                    user
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun sendFriendRequest(receiverId: String): Result<Unit> = runCatching {
        val senderId = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        val senderProfile = firestore.collection("users").document(senderId).get().await()
            .toObject(User::class.java) ?: throw IllegalStateException("Sender profile not found")

        val requestId = UUID.randomUUID().toString()
        val friendRequest = FriendRequest(
            requestId = requestId,
            senderId = senderId,
            senderFirstName = senderProfile.firstName,
            senderLastName = senderProfile.lastName,
            senderAvatarUrl = senderProfile.avatarUrl,
            receiverId = receiverId,
            status = FriendRequestStatus.PENDING,
            createdAtMillis = System.currentTimeMillis(),
        )

        firestore.collection("friend_requests").document(requestId).set(friendRequest).await()
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Unit> = runCatching {
        val requestDoc = firestore.collection("friend_requests").document(requestId).get().await()
        val request = requestDoc.toObject(FriendRequest::class.java) ?: throw IllegalStateException("Request not found")

        firestore.collection("friend_requests").document(requestId)
            .update("status", FriendRequestStatus.ACCEPTED).await()

        firestore.collection("users").document(request.senderId).collection("friends")
            .document(request.receiverId).set(mapOf("addedAtMillis" to System.currentTimeMillis())).await()

        firestore.collection("users").document(request.receiverId).collection("friends")
            .document(request.senderId).set(mapOf("addedAtMillis" to System.currentTimeMillis())).await()
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> = runCatching {
        firestore.collection("friend_requests").document(requestId)
            .update("status", FriendRequestStatus.REJECTED).await()
    }

    override fun getFriendsList(): Flow<List<User>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            val snapshot = try {
                firestore.collection("users").document(userId).collection("friends")
                    .get().await()
            } catch (e: Exception) {
                return@map emptyList()
            }
            snapshot.documents.mapNotNull { doc ->
                val friendId = doc.id
                try {
                    firestore.collection("users").document(friendId).get().await()
                        .toObject(User::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun getPendingRequests(): Flow<List<FriendRequest>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            try {
                val snapshot = firestore.collection("friend_requests")
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("status", "PENDING")
                    .get().await()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(FriendRequest::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getSentRequests(): Flow<List<FriendRequest>> {
        return currentUserIdState.map { userId ->
            if (userId == null) return@map emptyList()
            try {
                val snapshot = firestore.collection("friend_requests")
                    .whereEqualTo("senderId", userId)
                    .whereEqualTo("status", "PENDING")
                    .get().await()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(FriendRequest::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getCurrentUserId(): Flow<String?> = currentUserIdState

    private suspend fun uploadAvatar(userId: String, avatarUri: String): String {
        val uri = Uri.parse(avatarUri)
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Cannot open image")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val imageData = baos.toByteArray()

        val ref = storage.reference.child("avatars/$userId.jpg")
        ref.putBytes(imageData).await()
        return ref.downloadUrl.await().toString()
    }
}
