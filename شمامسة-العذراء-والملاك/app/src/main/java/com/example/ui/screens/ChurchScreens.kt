package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassGroup
import com.example.data.model.Deacon
import com.example.data.model.DeaconRanks
import com.example.data.model.UserProfile
import com.example.ui.components.AddEditClassDialog
import com.example.ui.components.AddEditDeaconDialog
import com.example.ui.components.AddUncleDialog
import com.example.ui.components.ChurchHeader
import com.example.ui.components.DeaconCard
import com.example.ui.components.QuickCommunication
import com.example.ui.theme.CallBlue
import com.example.ui.theme.ChurchNavyPrimary
import com.example.ui.theme.CopticGoldSecondary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.ChurchUiState
import com.example.ui.viewmodel.ChurchViewModel

@Composable
fun MainChurchScreen(
    viewModel: ChurchViewModel,
    uiState: ChurchUiState,
    onSignOut: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDeaconDialog by remember { mutableStateOf(false) }
    var editingDeacon by remember { mutableStateOf<Deacon?>(null) }
    var deletingDeacon by remember { mutableStateOf<Deacon?>(null) }

    var showAddClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<ClassGroup?>(null) }
    var deletingClass by remember { mutableStateOf<ClassGroup?>(null) }

    var showAddUncleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userNotification, uiState.errorMessage) {
        val msg = uiState.userNotification ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            ChurchHeader(
                userProfile = uiState.currentUserProfile,
                onSignOut = onSignOut
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedTab == 0) {
                // Add Deacon FAB
                FloatingActionButton(
                    onClick = { showAddDeaconDialog = true },
                    containerColor = ChurchNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_deacon")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "إضافة شماس")
                }
            } else if (uiState.selectedTab == 1 && uiState.isSuperAdmin) {
                // Add Class FAB (Admin only)
                FloatingActionButton(
                    onClick = { showAddClassDialog = true },
                    containerColor = ChurchNavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_class")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إنشاء فصل")
                }
            } else if (uiState.selectedTab == 2 && uiState.isSuperAdmin) {
                // Add Uncle FAB (Admin only)
                FloatingActionButton(
                    onClick = { showAddUncleDialog = true },
                    containerColor = CopticGoldSecondary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_uncle")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "إضافة أنكل")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row
            val tabs = mutableListOf("الشمامسة", "الفصول")
            if (uiState.isSuperAdmin) {
                tabs.add("خدام الفصول (أنكل)")
            }
            tabs.add("لوحة التحكم والإحصاء")

            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.coerceAtMost(tabs.lastIndex),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ChurchNavyPrimary,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        icon = {
                            val icon = when (index) {
                                0 -> Icons.Default.Group
                                1 -> Icons.Default.School
                                2 -> if (uiState.isSuperAdmin) Icons.Default.SupervisedUserCircle else Icons.Default.BarChart
                                else -> Icons.Default.BarChart
                            }
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            if (uiState.isLoading && uiState.allDeacons.isEmpty() && uiState.classes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ChurchNavyPrimary)
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> DeaconsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onEditDeacon = { editingDeacon = it },
                        onDeleteDeacon = { deletingDeacon = it }
                    )
                    1 -> ClassesTabContent(
                        uiState = uiState,
                        onEditClass = { editingClass = it },
                        onDeleteClass = { deletingClass = it },
                        onSelectClassFilter = { classId ->
                            viewModel.selectClassFilter(classId)
                            viewModel.selectTab(0)
                        }
                    )
                    2 -> {
                        if (uiState.isSuperAdmin) {
                            UnclesTabContent(
                                uiState = uiState,
                                onAddUncle = { showAddUncleDialog = true },
                                onUpdateUncleClasses = { uncle, classIds ->
                                    viewModel.updateUncleClassAssignments(uncle.userId, classIds)
                                }
                            )
                        } else {
                            DashboardTabContent(uiState = uiState)
                        }
                    }
                    3 -> DashboardTabContent(uiState = uiState)
                }
            }
        }
    }

    // --- Dialogs ---

    if (showAddDeaconDialog) {
        AddEditDeaconDialog(
            availableClasses = uiState.accessibleClasses,
            onDismiss = { showAddDeaconDialog = false },
            onConfirm = { deacon ->
                viewModel.saveDeacon(deacon) { success ->
                    if (success) showAddDeaconDialog = false
                }
            }
        )
    }

    editingDeacon?.let { deacon ->
        AddEditDeaconDialog(
            initialDeacon = deacon,
            availableClasses = uiState.accessibleClasses,
            onDismiss = { editingDeacon = null },
            onConfirm = { updated ->
                viewModel.saveDeacon(updated) { success ->
                    if (success) editingDeacon = null
                }
            }
        )
    }

    deletingDeacon?.let { deacon ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletingDeacon = null },
            title = { Text("تأكيد حذف الشماس") },
            text = { Text("هل أنت متأكد من حذف الشماس (${deacon.fullName})؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDeacon(deacon)
                        deletingDeacon = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingDeacon = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showAddClassDialog) {
        AddEditClassDialog(
            allUncles = uiState.uncles,
            onDismiss = { showAddClassDialog = false },
            onConfirm = { name, stage, description, assignedUncleIds, assignedUncleNames ->
                viewModel.saveClass("", name, stage, description, assignedUncleIds, assignedUncleNames) { success ->
                    if (success) showAddClassDialog = false
                }
            }
        )
    }

    editingClass?.let { cls ->
        AddEditClassDialog(
            initialClass = cls,
            allUncles = uiState.uncles,
            onDismiss = { editingClass = null },
            onConfirm = { name, stage, description, assignedUncleIds, assignedUncleNames ->
                viewModel.saveClass(cls.id, name, stage, description, assignedUncleIds, assignedUncleNames) { success ->
                    if (success) editingClass = null
                }
            }
        )
    }

    deletingClass?.let { cls ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletingClass = null },
            title = { Text("تأكيد حذف الفصل") },
            text = { Text("هل أنت متأكد من حذف (${cls.name})؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClass(cls.id)
                        deletingClass = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingClass = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showAddUncleDialog) {
        AddUncleDialog(
            availableClasses = uiState.classes,
            onDismiss = { showAddUncleDialog = false },
            onConfirm = { email, displayName, phone, assignedClassIds ->
                viewModel.createUncleAccount(email, displayName, phone, assignedClassIds) { success ->
                    if (success) showAddUncleDialog = false
                }
            }
        )
    }
}

@Composable
fun DeaconsTabContent(
    uiState: ChurchUiState,
    viewModel: ChurchViewModel,
    onEditDeacon: (Deacon) -> Unit,
    onDeleteDeacon: (Deacon) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search Field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث باسم الشماس، الهاتف، الرتبة، أو الفصل...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deacon_search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Classes Filter Chips
                Text("تصفية حسب الفصل:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedClassFilterId == null,
                            onClick = { viewModel.selectClassFilter(null) },
                            label = { Text("الكل (${uiState.filteredDeacons.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChurchNavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    items(uiState.accessibleClasses) { cls ->
                        FilterChip(
                            selected = uiState.selectedClassFilterId == cls.id,
                            onClick = {
                                if (uiState.selectedClassFilterId == cls.id) {
                                    viewModel.selectClassFilter(null)
                                } else {
                                    viewModel.selectClassFilter(cls.id)
                                }
                            },
                            label = { Text(cls.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChurchNavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Rank Filter Chips
                Text("تصفية حسب الرتبة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedRankFilter == null,
                            onClick = { viewModel.selectRankFilter(null) },
                            label = { Text("كل الرتب") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CopticGoldSecondary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(DeaconRanks.ALL) { rank ->
                        FilterChip(
                            selected = uiState.selectedRankFilter == rank,
                            onClick = {
                                if (uiState.selectedRankFilter == rank) {
                                    viewModel.selectRankFilter(null)
                                } else {
                                    viewModel.selectRankFilter(rank)
                                }
                            },
                            label = { Text(rank) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CopticGoldSecondary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Deacons List
        val deacons = uiState.filteredDeacons
        if (deacons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotBlank() || uiState.selectedClassFilterId != null) {
                            "لا توجد نتائج تطابق معايير البحث."
                        } else {
                            "لم يتم تسجيل شمامسة في هذا القسم بعد. اضغط على زر (+) لإضافة أول شماس."
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(deacons, key = { it.id }) { deacon ->
                    val canEdit = uiState.isSuperAdmin || uiState.currentUserProfile?.canManageClass(deacon.classId) == true
                    val canDelete = uiState.isSuperAdmin
                    DeaconCard(
                        deacon = deacon,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        onEdit = { onEditDeacon(deacon) },
                        onDelete = { onDeleteDeacon(deacon) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClassesTabContent(
    uiState: ChurchUiState,
    onEditClass: (ClassGroup) -> Unit,
    onDeleteClass: (ClassGroup) -> Unit,
    onSelectClassFilter: (String) -> Unit
) {
    val classes = uiState.classes
    if (classes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد فصول دراسية حالياً.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "فصول ومجموعات خدمة الشمامسة (${classes.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ChurchNavyPrimary
                )
            }

            items(classes, key = { it.id }) { cls ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cls.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = ChurchNavyPrimary
                                )
                                Surface(
                                    color = CopticGoldSecondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = cls.stage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CopticGoldSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (uiState.isSuperAdmin) {
                                Row {
                                    IconButton(onClick = { onEditClass(cls) }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = ChurchNavyPrimary)
                                    }
                                    IconButton(onClick = { onDeleteClass(cls) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                                    }
                                }
                            }
                        }

                        if (cls.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cls.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Assigned Uncles
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (cls.assignedUncleNames.isNotEmpty()) {
                                    "خدام الفصل: " + cls.assignedUncleNames.joinToString("، ")
                                } else {
                                    "لم يتم تعيين أنكل لهذا الفصل بعد"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // View Deacons in this class button
                        Button(
                            onClick = { onSelectClassFilter(cls.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "عرض شمامسة هذا الفصل",
                                color = ChurchNavyPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnclesTabContent(
    uiState: ChurchUiState,
    onAddUncle: () -> Unit,
    onUpdateUncleClasses: (UserProfile, List<String>) -> Unit
) {
    val uncles = uiState.uncles
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "إدارة حسابات خدام الفصول (أنكل):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ChurchNavyPrimary
                )
                Text(
                    text = "الآدمن الأكبر فقط يمنح الصلاحيات ويوزع كل أنكل في فصله",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddUncle,
                colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ إضافة أنكل")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uncles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم إضافة أي حسابات أنكل بعد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uncles, key = { it.userId }) { uncle ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(ChurchNavyPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = ChurchNavyPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = uncle.displayName.ifBlank { uncle.email },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = uncle.email,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick communication with Uncle
                                if (uncle.phone.isNotBlank()) {
                                    Row {
                                        IconButton(onClick = {
                                            QuickCommunication.openWhatsAppChat(context, uncle.phone, uncle.displayName)
                                        }) {
                                            Text("💬", fontSize = 18.sp)
                                        }
                                        IconButton(onClick = {
                                            QuickCommunication.dialPhoneNumber(context, uncle.phone)
                                        }) {
                                            Icon(imageVector = Icons.Default.Call, contentDescription = "اتصال", tint = CallBlue)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Assigned Classes for this Uncle
                            Text(
                                text = "الفصول المحددة له من الآدمن:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChurchNavyPrimary
                            )

                            val assignedClasses = uiState.classes.filter { it.id in uncle.assignedClassIds }
                            if (assignedClasses.isEmpty()) {
                                Text(
                                    text = "لم يتم تحديد أي فصل له بعد.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    assignedClasses.forEach { cls ->
                                        Surface(
                                            color = ChurchNavyPrimary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = cls.name,
                                                fontSize = 11.sp,
                                                color = ChurchNavyPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTabContent(uiState: ChurchUiState) {
    val stats = uiState.stats

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "الإحصائيات العامة لخدمة الشمامسة:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = ChurchNavyPrimary
            )
        }

        // Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "إجمالي الشمامسة",
                    count = stats.totalDeacons.toString(),
                    icon = Icons.Default.Group,
                    color = ChurchNavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "إجمالي الفصول",
                    count = stats.totalClasses.toString(),
                    icon = Icons.Default.School,
                    color = CopticGoldSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "خدام الفصول (أنكل)",
                    count = stats.totalUncles.toString(),
                    icon = Icons.Default.SupervisedUserCircle,
                    color = Color(0xFF2E5B88),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Breakdown by Deacon Rank
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "توزيع الشمامسة حسب الرتبة الكنسية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ChurchNavyPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DeaconRanks.ALL.forEach { rank ->
                        val count = stats.rankCounts[rank] ?: 0
                        val fraction = if (stats.totalDeacons > 0) count.toFloat() / stats.totalDeacons else 0f

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rank, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "$count شماس", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                color = CopticGoldSecondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Church Contact & Info Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchNavyPrimary.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "كنيسة السيدة العذراء ورئيس الملائكة ميخائيل - الأخلاص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ChurchNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تطبيق إدارة خدمة الشمامسة والفصول وتسهيل التواصل السريع عبر الواتساب والمكالمات ومتابعة نمو الشمامسة الروحي والطقسي والألحان.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
