package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChurchStats
import com.example.data.model.ClassGroup
import com.example.data.model.Deacon
import com.example.data.model.UserProfile
import com.example.data.repository.ChurchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChurchUiState(
    val currentUserProfile: UserProfile? = null,
    val classes: List<ClassGroup> = emptyList(),
    val uncles: List<UserProfile> = emptyList(),
    val allDeacons: List<Deacon> = emptyList(),
    val selectedClassFilterId: String? = null,
    val selectedRankFilter: String? = null,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userNotification: String? = null
) {
    val isSuperAdmin: Boolean
        get() = currentUserProfile?.isSuperAdmin() == true

    /**
     * Classes accessible to the current user (all classes for Super Admin; assigned classes for Uncle)
     */
    val accessibleClasses: List<ClassGroup>
        get() {
            return if (isSuperAdmin) {
                classes
            } else {
                val assigned = currentUserProfile?.assignedClassIds.orEmpty()
                classes.filter { it.id in assigned }
            }
        }

    /**
     * Deacons visible to the current user according to RBAC and active filters
     */
    val filteredDeacons: List<Deacon>
        get() {
            // First scope by RBAC
            val baseList = if (isSuperAdmin) {
                allDeacons
            } else {
                val assigned = currentUserProfile?.assignedClassIds.orEmpty()
                val currentUid = currentUserProfile?.userId.orEmpty()
                allDeacons.filter { deacon ->
                    deacon.classId in assigned ||
                    deacon.assignedUncleIds.contains(currentUid) ||
                    deacon.addedByUid == currentUid
                }
            }

            return baseList.filter { deacon ->
                // Class filter
                val matchesClass = selectedClassFilterId == null || deacon.classId == selectedClassFilterId

                // Rank filter
                val matchesRank = selectedRankFilter == null || deacon.rank == selectedRankFilter

                // Search query
                val query = searchQuery.trim().lowercase()
                val matchesQuery = query.isBlank() ||
                        deacon.fullName.lowercase().contains(query) ||
                        deacon.phone.contains(query) ||
                        deacon.parentPhone.contains(query) ||
                        deacon.rank.lowercase().contains(query) ||
                        deacon.className.lowercase().contains(query) ||
                        deacon.grade.lowercase().contains(query)

                matchesClass && matchesRank && matchesQuery
            }
        }

    val stats: ChurchStats
        get() {
            val list = if (isSuperAdmin) allDeacons else filteredDeacons
            val rankCounts = list.groupBy { it.rank }.mapValues { it.value.size }
            val stageCounts = list.groupBy { it.grade.ifBlank { "غير محدد" } }.mapValues { it.value.size }
            return ChurchStats(
                totalDeacons = list.size,
                totalClasses = classes.size,
                totalUncles = uncles.size,
                rankCounts = rankCounts,
                stageCounts = stageCounts
            )
        }
}

class ChurchViewModel(private val repository: ChurchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChurchUiState())
    val uiState: StateFlow<ChurchUiState> = _uiState.asStateFlow()

    fun initializeUser(uid: String, email: String, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val profile = repository.getOrCreateUserProfile(uid, email, displayName)
                _uiState.update { it.copy(currentUserProfile = profile) }
                repository.seedInitialClassesIfEmpty()
                startObservingData(uid)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "تعذر تحميل بيانات المستخدم: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun startObservingData(uid: String) {
        // Observe profile updates (e.g. when admin changes class assignments)
        viewModelScope.launch {
            repository.observeUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(currentUserProfile = profile) }
                }
            }
        }

        // Observe classes
        viewModelScope.launch {
            repository.observeClasses().collect { classList ->
                _uiState.update { it.copy(classes = classList, isLoading = false) }
            }
        }

        // Observe uncles
        viewModelScope.launch {
            repository.observeAllUncles().collect { uncleList ->
                _uiState.update { it.copy(uncles = uncleList) }
            }
        }

        // Observe deacons
        viewModelScope.launch {
            repository.observeDeacons().collect { deaconsList ->
                _uiState.update { it.copy(allDeacons = deaconsList) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectClassFilter(classId: String?) {
        _uiState.update { it.copy(selectedClassFilterId = classId) }
    }

    fun selectRankFilter(rank: String?) {
        _uiState.update { it.copy(selectedRankFilter = rank) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null, errorMessage = null) }
    }

    // --- Deacon CRUD ---

    fun saveDeacon(deacon: Deacon, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (deacon.id.isBlank()) {
                repository.addDeacon(deacon)
            } else {
                repository.updateDeacon(deacon)
            }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNotification = if (deacon.id.isBlank()) "تمت إضافة الشماس بنجاح" else "تم تحديث بيانات الشماس بنجاح"
                    )
                }
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "تعذر حفظ بيانات الشماس: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
                onComplete(false)
            }
        }
    }

    fun deleteDeacon(deacon: Deacon) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteDeacon(deacon.id, deacon.classId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, userNotification = "تم حذف الشماس بنجاح")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل حذف الشماس: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    // --- Classes CRUD (Super Admin) ---

    fun saveClass(
        classId: String,
        name: String,
        stage: String,
        description: String,
        assignedUncleIds: List<String>,
        assignedUncleNames: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val actualClassId: String
            val isSuccess: Boolean
            val errorMessage: String?

            if (classId.isBlank()) {
                val createResult = repository.createClass(name, stage, description, assignedUncleIds, assignedUncleNames)
                isSuccess = createResult.isSuccess
                actualClassId = createResult.getOrNull().orEmpty()
                errorMessage = createResult.exceptionOrNull()?.localizedMessage
            } else {
                val current = _uiState.value.classes.find { it.id == classId }
                val updated = ClassGroup(
                    id = classId,
                    name = name,
                    stage = stage,
                    description = description,
                    assignedUncleIds = assignedUncleIds,
                    assignedUncleNames = assignedUncleNames,
                    studentCount = current?.studentCount ?: 0L
                )
                val updateResult = repository.updateClass(updated)
                isSuccess = updateResult.isSuccess
                actualClassId = classId
                errorMessage = updateResult.exceptionOrNull()?.localizedMessage
            }

            if (isSuccess) {
                // If uncles were assigned, update their assignedClassIds
                if (actualClassId.isNotBlank()) {
                    assignedUncleIds.forEach { uncleId ->
                        val currentUncle = _uiState.value.uncles.find { it.userId == uncleId }
                        if (currentUncle != null && !currentUncle.assignedClassIds.contains(actualClassId)) {
                            repository.updateUncleAssignments(
                                uncleId,
                                (currentUncle.assignedClassIds + listOf(actualClassId)).distinct()
                            )
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNotification = if (classId.isBlank()) "تم إنشاء الفصل بنجاح" else "تم تحديث الفصل بنجاح"
                    )
                }
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل حفظ بيانات الفصل: $errorMessage"
                    )
                }
                onComplete(false)
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteClass(classId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, userNotification = "تم حذف الفصل بنجاح")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل حذف الفصل: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            }
        }
    }

    // --- Uncle Management (Super Admin) ---

    fun createUncleAccount(
        email: String,
        displayName: String,
        phone: String,
        assignedClassIds: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.addUncleAccount(email, displayName, phone, assignedClassIds)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, userNotification = "تمت إضافة حساب أنكل بنجاح")
                }
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل إنشاء حساب أنكل: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
                onComplete(false)
            }
        }
    }

    fun updateUncleClassAssignments(
        uncleId: String,
        assignedClassIds: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.updateUncleAssignments(uncleId, assignedClassIds)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, userNotification = "تم تحديث فصول أنكل بنجاح")
                }
                onComplete(true)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "فشل تحديث التعيين: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
                onComplete(false)
            }
        }
    }
}
