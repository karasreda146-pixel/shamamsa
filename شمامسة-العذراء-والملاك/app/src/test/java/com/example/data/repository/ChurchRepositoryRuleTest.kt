package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChurchStages
import com.example.data.model.Deacon
import com.example.data.model.DeaconRanks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChurchRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun testSuperAdmin_canCreateClass() = runBlocking {
    val uid = signInTestUser("karasreda146@gmail.com")
    println("DEBUG TEST USER: uid=$uid, email=${auth.currentUser?.email}")
    val repository = ChurchRepository(firestore, auth)

    val result = withTimeout(5000L) {
      repository.createClass(
        name = "فصل الملاك ميخائيل للاختبار",
        stage = ChurchStages.PRIMARY,
        description = "فصل تجريبي",
        assignedUncleIds = emptyList(),
        assignedUncleNames = emptyList()
      )
    }
    assertTrue("Super admin should create class successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)
    assertNotNull(result.getOrNull())
  }

  @Test
  fun testUncle_cannotCreateClass() = runBlocking {
    signInTestUser("test.uncle@example.com")
    val repository = ChurchRepository(firestore, auth)

    val result = withTimeout(5000L) {
      repository.createClass(
        name = "فصل تجريبي من أنكل",
        stage = ChurchStages.PREPARATORY,
        description = "غير مصرح",
        assignedUncleIds = emptyList(),
        assignedUncleNames = emptyList()
      )
    }
    assertFalse("Uncle should be denied from creating class", result.isSuccess)
  }

  @Test
  fun testUnauthenticated_cannotCreateDeacon() = runBlocking {
    auth.signOut()
    val repository = ChurchRepository(firestore, auth)

    val deacon = Deacon(
      fullName = "مينا عادل",
      phone = "01234567890",
      rank = DeaconRanks.EPSALTOS,
      classId = "class_dummy"
    )

    var caught = false
    try {
      repository.addDeacon(deacon)
    } catch (e: Exception) {
      caught = true
    }
    assertTrue("Unauthenticated addDeacon must fail", caught)
  }
}
