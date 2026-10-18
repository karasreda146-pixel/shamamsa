package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChurchStages
import com.example.data.model.ClassGroup
import com.example.data.model.Deacon
import com.example.data.model.DeaconRanks
import com.example.data.model.UserProfile
import com.example.ui.theme.ChurchNavyPrimary

@Composable
fun AddEditDeaconDialog(
    initialDeacon: Deacon? = null,
    availableClasses: List<ClassGroup>,
    onDismiss: () -> Unit,
    onConfirm: (Deacon) -> Unit
) {
    var fullName by remember { mutableStateOf(initialDeacon?.fullName.orEmpty()) }
    var phone by remember { mutableStateOf(initialDeacon?.phone.orEmpty()) }
    var parentPhone by remember { mutableStateOf(initialDeacon?.parentPhone.orEmpty()) }
    var rank by remember { mutableStateOf(initialDeacon?.rank ?: DeaconRanks.EPSALTOS) }
    var selectedClassId by remember {
        mutableStateOf(initialDeacon?.classId ?: availableClasses.firstOrNull()?.id.orEmpty())
    }
    var grade by remember { mutableStateOf(initialDeacon?.grade.orEmpty()) }
    var birthDate by remember { mutableStateOf(initialDeacon?.birthDate.orEmpty()) }
    var address by remember { mutableStateOf(initialDeacon?.address.orEmpty()) }
    var notes by remember { mutableStateOf(initialDeacon?.notes.orEmpty()) }

    var rankDropdownExpanded by remember { mutableStateOf(false) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val selectedClassName = availableClasses.find { it.id == selectedClassId }?.name.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDeacon == null) "إضافة شماس جديد" else "تعديل بيانات الشماس",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم ثلاثي أو رباعي *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deacon_fullname_input")
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف الشماس (للواتساب والاتصال) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deacon_phone_input")
                )

                // Parent Phone
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم هاتف ولي الأمر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Deacon Rank Dropdown
                Column {
                    Text("رتبة الشماسية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rankDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = rank, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = rankDropdownExpanded,
                        onDismissRequest = { rankDropdownExpanded = false }
                    ) {
                        DeaconRanks.ALL.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    rank = r
                                    rankDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Class Dropdown
                Column {
                    Text("الفصل التابع له:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { classDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedClassName.ifBlank { "اختر الفصل" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        availableClasses.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.name) },
                                onClick = {
                                    selectedClassId = cls.id
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Grade / School year
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("المرحلة الدراسية (مثلاً: ثانية إعدادي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Birth Date
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("تاريخ الميلاد (مثال: 2012-04-15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان بالتفصيل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات روحية وافتقاد (أب الاعتراف، الألحان)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        validationError = "يرجى كتابة اسم الشماس"
                        return@Button
                    }
                    if (phone.isBlank()) {
                        validationError = "يرجى إدخال رقم الهاتف للتواصل"
                        return@Button
                    }
                    if (selectedClassId.isBlank()) {
                        validationError = "يرجى تحديد الفصل التابع له الشماس"
                        return@Button
                    }
                    validationError = null

                    val updated = (initialDeacon ?: Deacon()).copy(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        parentPhone = parentPhone.trim(),
                        rank = rank,
                        classId = selectedClassId,
                        className = selectedClassName,
                        grade = grade.trim(),
                        birthDate = birthDate.trim(),
                        address = address.trim(),
                        notes = notes.trim()
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary),
                modifier = Modifier.testTag("save_deacon_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddEditClassDialog(
    initialClass: ClassGroup? = null,
    allUncles: List<UserProfile>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, stage: String, description: String, assignedUncleIds: List<String>, assignedUncleNames: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialClass?.name.orEmpty()) }
    var stage by remember { mutableStateOf(initialClass?.stage ?: ChurchStages.PRIMARY) }
    var description by remember { mutableStateOf(initialClass?.description.orEmpty()) }
    val selectedUncleIds = remember {
        mutableStateListOf<String>().apply {
            addAll(initialClass?.assignedUncleIds.orEmpty())
        }
    }

    var stageDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClass == null) "إنشاء فصل دراسي جديد" else "تعديل الفصل الدراسي",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفصل (مثال: فصل الملاك ميخائيل) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_name_input")
                )

                // Stage Dropdown
                Column {
                    Text("المرحلة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stageDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stage, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = stageDropdownExpanded,
                        onDismissRequest = { stageDropdownExpanded = false }
                    ) {
                        ChurchStages.ALL.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    stage = s
                                    stageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف الفصل أو المنهج المقرر") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Assign Uncles
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "تعيين خدام الفصل (أنكل):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ChurchNavyPrimary
                )

                if (allUncles.isEmpty()) {
                    Text(
                        text = "لا يوجد حسابات أنكل مسجلة بعد. يمكنك إضافة أنكل من تبويب خدام الفصول.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allUncles.forEach { uncle ->
                        val isChecked = selectedUncleIds.contains(uncle.userId)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedUncleIds.remove(uncle.userId)
                                    } else {
                                        selectedUncleIds.add(uncle.userId)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedUncleIds.add(uncle.userId)
                                    else selectedUncleIds.remove(uncle.userId)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = uncle.displayName.ifBlank { uncle.email },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (uncle.phone.isNotBlank()) {
                                    Text(text = uncle.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "يرجى إدخال اسم الفصل"
                        return@Button
                    }
                    val assignedNames = selectedUncleIds.mapNotNull { uid ->
                        allUncles.find { it.userId == uid }?.displayName
                    }
                    onConfirm(name.trim(), stage, description.trim(), selectedUncleIds.toList(), assignedNames)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary),
                modifier = Modifier.testTag("save_class_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddUncleDialog(
    availableClasses: List<ClassGroup>,
    onDismiss: () -> Unit,
    onConfirm: (email: String, displayName: String, phone: String, assignedClassIds: List<String>) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val selectedClassIds = remember { mutableStateListOf<String>() }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة حساب أنكل جديد",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "الآدمن الأكبر هو المسؤول حصرياً عن تسجيل حساب أنكل وتحديد فصوله.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("اسم أنكل بالكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("uncle_name_input")
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني لحساب Google *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("uncle_email_input")
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف أنكل (واتساب واتصال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Class assignments
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الفصول المحددة لهذا الأنكل:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ChurchNavyPrimary
                )

                if (availableClasses.isEmpty()) {
                    Text("لا توجد فصول حالياً. يمكنك إنشاء فصول أولاً.", fontSize = 12.sp)
                } else {
                    availableClasses.forEach { cls ->
                        val isChecked = selectedClassIds.contains(cls.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedClassIds.remove(cls.id)
                                    else selectedClassIds.add(cls.id)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedClassIds.add(cls.id)
                                    else selectedClassIds.remove(cls.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = cls.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        validationError = "يرجى كتابة اسم أنكل"
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        validationError = "يرجى كتابة بريد إلكتروني صالح لحساب جوجل"
                        return@Button
                    }
                    onConfirm(email.trim(), displayName.trim(), phone.trim(), selectedClassIds.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary),
                modifier = Modifier.testTag("save_uncle_button")
            ) {
                Text("إضافة أنكل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
