package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "uncle", // "super_admin" or "uncle"
    val phone: String = "",
    val assignedClassIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isSuperAdmin(): Boolean {
        return role == "super_admin" || email.equals("karasreda146@gmail.com", ignoreCase = true)
    }

    fun canManageClass(classId: String): Boolean {
        return isSuperAdmin() || assignedClassIds.contains(classId)
    }
}

@IgnoreExtraProperties
data class ClassGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val stage: String = "",
    val description: String = "",
    val assignedUncleIds: List<String> = emptyList(),
    val assignedUncleNames: List<String> = emptyList(),
    val studentCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Deacon(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val parentPhone: String = "",
    val rank: String = "إبصالتس (مرتل)",
    val classId: String = "",
    val className: String = "",
    val grade: String = "",
    val birthDate: String = "",
    val address: String = "",
    val notes: String = "",
    val addedByUid: String = "",
    val assignedUncleIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ChurchStats(
    val totalDeacons: Int = 0,
    val totalClasses: Int = 0,
    val totalUncles: Int = 0,
    val rankCounts: Map<String, Int> = emptyMap(),
    val stageCounts: Map<String, Int> = emptyMap()
)

object DeaconRanks {
    const val EPSALTOS = "إبصالتس (مرتل)"
    const val AGNOSTOS = "أغنسطس (قارئ)"
    const val EPIDIACON = "إيبودياكون (مساعد شماس)"
    const val DEACON = "دياكون (شماس كامل)"
    const val PREPARATORY = "تمهيدي / مستمع"

    val ALL = listOf(EPSALTOS, AGNOSTOS, EPIDIACON, DEACON, PREPARATORY)
}

object ChurchStages {
    const val PRIMARY = "المرحلة الابتدائية"
    const val PREPARATORY = "المرحلة الإعدادية"
    const val SECONDARY = "المرحلة الثانوية"
    const val UNIVERSITY = "الجامعيين والخريجين"
    const val GENERAL = "عام / كل المراحل"

    val ALL = listOf(PRIMARY, PREPARATORY, SECONDARY, UNIVERSITY, GENERAL)
}

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
