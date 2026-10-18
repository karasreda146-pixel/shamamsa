package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChurchStats
import com.example.data.model.ClassGroup
import com.example.data.model.Deacon
import com.example.data.model.OperationType
import com.example.data.model.UserProfile
import com.example.data.model.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChurchRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Church data.")
    }

    // --- User Profile & RBAC ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val path = "users/$uid"
        val registration = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun getOrCreateUserProfile(uid: String, email: String, displayName: String): UserProfile {
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val profile = snapshot.toObject(UserProfile::class.java)
            if (profile != null) return profile
        }

        val isSuperAdmin = email.equals("karasreda146@gmail.com", ignoreCase = true)
        val newProfile = UserProfile(
            userId = uid,
            email = email,
            displayName = displayName.ifBlank { "خادم" },
            role = if (isSuperAdmin) "super_admin" else "uncle",
            phone = "",
            assignedClassIds = emptyList(),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        val dataMap = mapOf(
            "userId" to newProfile.userId,
            "email" to newProfile.email,
            "displayName" to newProfile.displayName,
            "role" to newProfile.role,
            "phone" to newProfile.phone,
            "assignedClassIds" to newProfile.assignedClassIds,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(dataMap).await()
            if (isSuperAdmin) {
                // Also create admin doc to satisfy exists(/admins/{uid})
                db.collection("admins").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "email" to email,
                        "role" to "super_admin",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        return newProfile
    }

    fun observeAllUncles(): Flow<List<UserProfile>> = callbackFlow {
        val path = "users"
        val registration = db.collection("users")
            .whereEqualTo("role", "uncle")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addUncleAccount(
        email: String,
        displayName: String,
        phone: String,
        assignedClassIds: List<String>
    ): Result<String> {
        return try {
            val docRef = db.collection("users").document()
            val newUncleId = docRef.id
            val payload = mapOf(
                "userId" to newUncleId,
                "email" to email.trim(),
                "displayName" to displayName.trim(),
                "role" to "uncle",
                "phone" to phone.trim(),
                "assignedClassIds" to assignedClassIds,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(newUncleId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users")
            Result.failure(e)
        }
    }

    suspend fun updateUncleAssignments(
        userId: String,
        assignedClassIds: List<String>,
        phone: String? = null,
        displayName: String? = null
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "assignedClassIds" to assignedClassIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            phone?.let { updates["phone"] = it }
            displayName?.let { updates["displayName"] = it }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Classes / Groups ---

    fun observeClasses(): Flow<List<ClassGroup>> = callbackFlow {
        val path = "classes"
        val registration = db.collection("classes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val classes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ClassGroup::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(classes)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createClass(
        name: String,
        stage: String,
        description: String,
        assignedUncleIds: List<String>,
        assignedUncleNames: List<String>
    ): Result<String> {
        val path = "classes"
        return try {
            val docRef = db.collection("classes").document()
            val payload = mapOf(
                "name" to name.trim(),
                "stage" to stage.trim(),
                "description" to description.trim(),
                "assignedUncleIds" to assignedUncleIds,
                "assignedUncleNames" to assignedUncleNames,
                "studentCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateClass(classGroup: ClassGroup): Result<Unit> {
        val docRef = db.collection("classes").document(classGroup.id)
        return try {
            val payload = mapOf(
                "name" to classGroup.name.trim(),
                "stage" to classGroup.stage.trim(),
                "description" to classGroup.description.trim(),
                "assignedUncleIds" to classGroup.assignedUncleIds,
                "assignedUncleNames" to classGroup.assignedUncleNames,
                "studentCount" to classGroup.studentCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteClass(classId: String): Result<Unit> {
        val docRef = db.collection("classes").document(classId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialClassesIfEmpty() {
        try {
            val existing = db.collection("classes").limit(1).get().await()
            if (existing.isEmpty) {
                createClass(
                    name = "فصل رئيس الملائكة ميخائيل (ابتدائي)",
                    stage = "المرحلة الابتدائية",
                    description = "تعليم ألحان وطقوس الشماسية لمرحلة الابتدائي",
                    assignedUncleIds = emptyList(),
                    assignedUncleNames = emptyList()
                )
                createClass(
                    name = "فصل القديس أثناسيوس الرسولي (إعدادي)",
                    stage = "المرحلة الإعدادية",
                    description = "دراسة طقس القداس الإلهي وقراءات التسبحة",
                    assignedUncleIds = emptyList(),
                    assignedUncleNames = emptyList()
                )
                createClass(
                    name = "فصل القديس اسطفانوس رئيس الشمامسة (ثانوي)",
                    stage = "المرحلة الثانوية",
                    description = "إعداد وتأهيل شمامسة مرحلة ثانوي",
                    assignedUncleIds = emptyList(),
                    assignedUncleNames = emptyList()
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "classes")
        }
    }

    // --- Deacons Database & Search ---

    fun observeDeacons(): Flow<List<Deacon>> = callbackFlow {
        val path = "deacons"
        val registration = db.collection("deacons")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                val deacons = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Deacon::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(deacons)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addDeacon(deacon: Deacon): Result<String> {
        val path = "deacons"
        val uid = requireUserId()
        return try {
            val docRef = db.collection("deacons").document()
            val payload = mapOf(
                "fullName" to deacon.fullName.trim(),
                "phone" to deacon.phone.trim(),
                "parentPhone" to deacon.parentPhone.trim(),
                "rank" to deacon.rank.trim(),
                "classId" to deacon.classId,
                "className" to deacon.className.trim(),
                "grade" to deacon.grade.trim(),
                "birthDate" to deacon.birthDate.trim(),
                "address" to deacon.address.trim(),
                "notes" to deacon.notes.trim(),
                "addedByUid" to uid,
                "assignedUncleIds" to (deacon.assignedUncleIds + listOf(uid)).distinct(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Increment class student count
            if (deacon.classId.isNotBlank()) {
                db.collection("classes").document(deacon.classId)
                    .update("studentCount", FieldValue.increment(1))
                    .await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDeacon(deacon: Deacon): Result<Unit> {
        val docRef = db.collection("deacons").document(deacon.id)
        return try {
            val payload = mapOf(
                "fullName" to deacon.fullName.trim(),
                "phone" to deacon.phone.trim(),
                "parentPhone" to deacon.parentPhone.trim(),
                "rank" to deacon.rank.trim(),
                "classId" to deacon.classId,
                "className" to deacon.className.trim(),
                "grade" to deacon.grade.trim(),
                "birthDate" to deacon.birthDate.trim(),
                "address" to deacon.address.trim(),
                "notes" to deacon.notes.trim(),
                "assignedUncleIds" to deacon.assignedUncleIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeacon(deaconId: String, classId: String?): Result<Unit> {
        val docRef = db.collection("deacons").document(deaconId)
        return try {
            docRef.delete().await()
            if (!classId.isNullOrBlank()) {
                db.collection("classes").document(classId)
                    .update("studentCount", FieldValue.increment(-1))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
