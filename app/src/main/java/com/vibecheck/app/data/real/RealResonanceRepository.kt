package com.vibecheck.app.data.real

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.ResonancePost
import com.vibecheck.app.data.ResonanceRepository
import com.vibecheck.app.data.ResonanceScope
import com.vibecheck.app.domain.chat.ProfanityFilter
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class RealResonanceRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val context: Context? = null,
) : ResonanceRepository {

    override suspend fun feed(
        regionId: String,
        scope: ResonanceScope,
        limit: Int,
    ): Result<List<ResonancePost>> = runCatching {
        val collection = firestore.collection("resonance_posts")
        val query = when (scope) {
            ResonanceScope.MY_CITY -> collection.whereEqualTo("regionId", regionId)
            ResonanceScope.GLOBAL -> collection
        }
        val docs = query
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        docs.documents.mapNotNull { doc ->
            try {
                ResonancePost(
                    id = doc.id,
                    mood = Mood.valueOf(doc.getString("mood") ?: "NEUTRAL"),
                    text = doc.getString("text") ?: "",
                    regionId = doc.getString("regionId") ?: regionId,
                    createdAtMillis = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    resonateCount = doc.getLong("resonateCount")?.toInt() ?: 0,
                    authorId = doc.getString("authorId") ?: "anon",
                    imageUrl = doc.getString("imageUrl"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun submitPost(
        mood: Mood,
        text: String,
        regionId: String,
        imageUri: String?,
    ): Result<ResonancePost> = runCatching {
        val cleaned = ProfanityFilter.clean(text.trim())
        require(cleaned.isNotBlank()) { "Post cannot be empty." }
        require(cleaned.length <= 100) { "Post is too long (max 100 chars)." }
        val words = cleaned.trim().split(Regex("\\s+"))
        require(words.size in 1..5) { "Post must be 1-5 words." }

        val uid = auth.currentUser?.uid ?: "anon"
        val postId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        var imageUrl: String? = null
        // Upload image if provided
        if (!imageUri.isNullOrBlank()) {
            try {
                imageUrl = uploadResonanceImage(postId, imageUri)
            } catch (e: Exception) {
                // Log but don't fail the post if image upload fails
                e.printStackTrace()
            }
        }

        val post = ResonancePost(
            id = postId,
            mood = mood,
            text = cleaned,
            regionId = regionId,
            createdAtMillis = now,
            resonateCount = 0,
            authorId = uid,
            imageUrl = imageUrl,
        )

        firestore.collection("resonance_posts").document(postId).set(
            mapOf(
                "mood" to mood.name,
                "text" to cleaned,
                "regionId" to regionId,
                "createdAt" to now,
                "resonateCount" to 0,
                "authorId" to uid,
                "imageUrl" to imageUrl,
            )
        ).await()

        post
    }

    private suspend fun uploadResonanceImage(postId: String, imageUri: String): String? {
        if (context == null) return null

        return try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Decode and resize image to 500x500
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val resizedBitmap = resizeBitmap(originalBitmap, 500, 500)

            // Compress as JPEG and get bytes
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Upload to Firebase Storage
            val storageRef = storage.reference.child("resonance_images/$postId.jpg")
            storageRef.putBytes(imageBytes).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        when {
            width > height -> {
                targetWidth = maxWidth
                targetHeight = (maxWidth / aspectRatio).toInt()
            }
            else -> {
                targetHeight = maxHeight
                targetWidth = (maxHeight * aspectRatio).toInt()
            }
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    override suspend fun resonate(postId: String): Result<Unit> = runCatching {
        val docRef = firestore.collection("resonance_posts").document(postId)
        // Atomic increment (best-effort; doesn't fail if doc missing)
        docRef.update("resonateCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }
}
