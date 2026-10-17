package com.example.sdui.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sdui.MainActivity
import com.example.sdui.R
import com.example.sdui.data.di.AuthModule
import com.example.sdui.data.di.RepositoryModule
import com.example.sdui.domain.model.AuthAccount
import com.example.sdui.domain.model.AuthSession
import com.example.sdui.domain.model.UiComponent
import com.example.sdui.domain.model.UiComponentType
import com.example.sdui.domain.model.UiDensity
import com.example.sdui.domain.model.UiSchema
import com.example.sdui.domain.model.UiSchemaResolution
import com.example.sdui.domain.model.UiTheme
import com.example.sdui.domain.model.value.AccountId
import com.example.sdui.domain.model.value.AccountName
import com.example.sdui.domain.model.value.SurfaceId
import com.example.sdui.domain.model.value.UiComponentId
import com.example.sdui.domain.repository.AuthRepository
import com.example.sdui.domain.repository.UiSchemaRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(AuthModule::class, RepositoryModule::class)
class SdUiAppTest {
  private val auth = FakeAuthRepository()
  private val schemas = FakeUiSchemaRepository()

  @BindValue @JvmField val authRepository: AuthRepository = auth
  @BindValue @JvmField val uiSchemaRepository: UiSchemaRepository = schemas

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun stateTransitionStartupSignOutThenSignInShowsDashboard() {
    // Arrange
    auth.startupSignOut.complete(Result.success(Unit))
    auth.signIns.trySend(Result.success(signedIn("first"))).getOrThrow()

    // Act
    // StateTransition: startup clears any leftover session, so the dashboard needs an explicit sign-in
    onText(R.string.sign_in).performClick()
    completeSchema(0, "First dashboard")

    // Assert
    onText(R.string.signed_in_as_format, "first@example.com").assertIsDisplayed()
    composeTestRule.onNodeWithText("First dashboard").assertIsDisplayed()
    assertEquals(1, auth.signOutCount.get())
    assertEquals(1, auth.signInCount.get())
    assertEquals(1, schemas.requests.size)
  }

  @Test
  fun errorStartupSignOutFailureShowsRetryInsteadOfLogin() {
    // Arrange
    auth.startupSignOut.complete(Result.failure(IOException("offline")))

    // Act
    // Error: a leftover session that cannot be cleared keeps the login screen unreachable
    val failedMessage = onText(R.string.sign_out_failed)

    // Assert
    failedMessage.assertIsDisplayed()
    onText(R.string.sign_out_retry).assertIsDisplayed().assertIsEnabled()
    onText(R.string.sign_in).assertDoesNotExist()
    assertEquals(1, auth.signOutCount.get())
    assertEquals(0, schemas.requests.size)
  }

  @Test
  fun stateTransitionRetryAfterStartupSignOutFailureShowsLogin() {
    // Arrange
    auth.startupSignOut.complete(Result.failure(IOException("offline")))
    onText(R.string.sign_out_failed).assertIsDisplayed()

    // Act
    // StateTransition: a successful retry replaces the failure screen with the login screen
    onText(R.string.sign_out_retry).performClick()

    // Assert
    onText(R.string.sign_in).assertIsDisplayed().assertIsEnabled()
    onText(R.string.sign_out_failed).assertDoesNotExist()
    assertEquals(2, auth.signOutCount.get())
  }

  @Test
  fun stateTransitionSignInResultIsConsumedBeforeReturningToLogin() {
    // Arrange
    auth.startupSignOut.complete(Result.success(Unit))
    auth.signIns.trySend(Result.success(signedIn("first"))).getOrThrow()

    // Act
    // StateTransition: a handled sign-in result cannot sign the user back in after sign-out
    onText(R.string.sign_in).performClick()
    completeSchema(0, "First dashboard")
    onText(R.string.sign_out).performClick()

    // Assert
    onText(R.string.sign_in).assertIsDisplayed().assertIsEnabled()
    composeTestRule.onNodeWithText("First dashboard").assertDoesNotExist()
    assertEquals(1, auth.signInCount.get())
    assertEquals(2, auth.signOutCount.get())
    assertEquals(1, schemas.requests.size)
  }

  @Test
  fun stateTransitionInSessionSignOutFailureShowsRetryWithoutContent() {
    // Arrange
    auth.laterSignOutResults.trySend(Result.failure(IOException("offline"))).getOrThrow()
    signInThroughLogin("first")
    completeSchema(0, "First dashboard")

    // Act
    // StateTransition: a failed sign-out hides the dashboard and never shows the login screen
    onText(R.string.sign_out).performClick()

    // Assert
    onText(R.string.sign_out_failed).assertIsDisplayed()
    composeTestRule.onNodeWithText("First dashboard").assertDoesNotExist()
    onText(R.string.sign_in).assertDoesNotExist()
    assertEquals(2, auth.signOutCount.get())
  }

  @Test
  fun stateTransitionUnauthorizedResponseReturnsToLogin() {
    // Arrange
    signInThroughLogin("first")
    awaitSchema(0)

    // Act
    // StateTransition: a fresh unauthorized response ends the session through the auth ViewModel
    schemas.requests[0].complete(Result.success(UiSchemaResolution.Unauthorized))

    // Assert
    onText(R.string.sign_in).assertIsDisplayed().assertIsEnabled()
    assertEquals(2, auth.signOutCount.get())
  }

  @Test
  fun stateTransitionReloginDoesNotReusePreviousUnauthorizedState() {
    // Arrange
    signInThroughLogin("first")
    awaitSchema(0)
    schemas.requests[0].complete(Result.success(UiSchemaResolution.Unauthorized))
    onText(R.string.sign_in).assertIsDisplayed()
    auth.signIns.trySend(Result.success(signedIn("second"))).getOrThrow()

    // Act
    // StateTransition: the retained main ViewModel starts a fresh request before observing old 401 state
    onText(R.string.sign_in).performClick()
    awaitSchema(1)
    onText(R.string.signed_in_as_format, "second@example.com").assertIsDisplayed()
    onText(R.string.sign_in).assertDoesNotExist()
    completeSchema(1, "Second dashboard")

    // Assert
    composeTestRule.onNodeWithText("Second dashboard").assertIsDisplayed()
    assertEquals(2, auth.signOutCount.get())
    assertEquals(2, schemas.requests.size)
  }

  @Test
  fun stateTransitionReloginHidesPreviousAccountContentWhileLoading() {
    // Arrange
    signInThroughLogin("first")
    completeSchema(0, "First dashboard")
    onText(R.string.sign_out).performClick()
    auth.signIns.trySend(Result.success(signedIn("second"))).getOrThrow()

    // Act
    // StateTransition: previous account content stays hidden until the new schema arrives
    onText(R.string.sign_in).performClick()
    awaitSchema(1)

    // Assert
    onText(R.string.signed_in_as_format, "second@example.com").assertIsDisplayed()
    composeTestRule.onNodeWithText("First dashboard").assertDoesNotExist()
    onText(R.string.load_ui_schema).assertIsNotEnabled()
  }

  @Test
  fun coroutineReloginCancelsThePreviousAccountsRequest() {
    // Arrange
    signInThroughLogin("first")
    awaitSchema(0)
    onText(R.string.sign_out).performClick()
    auth.signIns.trySend(Result.success(signedIn("second"))).getOrThrow()

    // Act
    // Coroutine: an old request completing with 401 cannot end the new account's session
    onText(R.string.sign_in).performClick()
    awaitSchema(1)
    schemas.requests[0].complete(Result.success(UiSchemaResolution.Unauthorized))
    completeSchema(1, "Second dashboard")

    // Assert
    composeTestRule.onNodeWithText("Second dashboard").assertIsDisplayed()
    assertEquals(1, schemas.cancellationCount.get())
    assertEquals(2, auth.signOutCount.get())
  }

  @Test
  fun stateTransitionActivityRecreationRetainsSessionAndReloadsSchema() {
    // Arrange
    signInThroughLogin("restored")
    completeSchema(0, "Before recreation")

    // Act
    // StateTransition: Activity recreation retains authentication and refreshes the displayed schema
    composeTestRule.activityRule.scenario.recreate()
    completeSchema(1, "After recreation")

    // Assert
    onText(R.string.signed_in_as_format, "restored@example.com").assertIsDisplayed()
    composeTestRule.onNodeWithText("After recreation").assertIsDisplayed()
    composeTestRule.onNodeWithText("Before recreation").assertDoesNotExist()
    assertEquals(1, auth.signOutCount.get())
    assertEquals(2, schemas.requests.size)
  }

  @Test
  fun stateTransitionSignInCompletesAfterActivityRecreation() {
    // Arrange
    auth.startupSignOut.complete(Result.success(Unit))
    onText(R.string.sign_in).performClick()
    composeTestRule.waitUntil { auth.signInCount.get() == 1 }

    // Act
    // StateTransition: the retained login ViewModel delivers its pending result to the recreated route
    composeTestRule.activityRule.scenario.recreate()
    auth.signIns.trySend(Result.success(signedIn("recreated"))).getOrThrow()
    completeSchema(0, "Recreated dashboard")

    // Assert
    onText(R.string.signed_in_as_format, "recreated@example.com").assertIsDisplayed()
    composeTestRule.onNodeWithText("Recreated dashboard").assertIsDisplayed()
    assertEquals(1, auth.signInCount.get())
    assertEquals(1, schemas.requests.size)
  }

  private fun signInThroughLogin(name: String) {
    auth.startupSignOut.complete(Result.success(Unit))
    auth.signIns.trySend(Result.success(signedIn(name))).getOrThrow()
    onText(R.string.sign_in).performClick()
  }

  private fun awaitSchema(index: Int) {
    composeTestRule.waitUntil { schemas.requests.size > index }
  }

  private fun completeSchema(index: Int, label: String) {
    awaitSchema(index)
    schemas.requests[index].complete(
      Result.success(
        UiSchemaResolution.Resolved(
          UiSchema(
            surface = SurfaceId.DASHBOARD,
            version = "2026-06-01",
            revision = "revision-$index",
            theme = UiTheme.LIGHT,
            density = UiDensity.COMFORTABLE,
            components = listOf(
              UiComponent(
                id = UiComponentId("summary"),
                type = UiComponentType.CARD,
                props = mapOf("title" to label),
                children = emptyList(),
              ),
            ),
          ),
        ),
      ),
    )
    composeTestRule.onNodeWithText(label).assertIsDisplayed()
  }

  private fun signedIn(name: String): AuthSession =
    AuthSession.SignedIn(
      AuthAccount(id = AccountId(name), name = AccountName("$name@example.com")),
    )

  private fun onText(resourceId: Int, vararg formatArgs: Any) =
    composeTestRule.onNodeWithText(composeTestRule.activity.getString(resourceId, *formatArgs))
}

private class FakeAuthRepository : AuthRepository {
  val startupSignOut = CompletableDeferred<Result<Unit>>()
  val laterSignOutResults = Channel<Result<Unit>>(Channel.UNLIMITED)
  val signIns = Channel<Result<AuthSession>>(Channel.UNLIMITED)
  val signInCount = AtomicInteger()
  val signOutCount = AtomicInteger()

  override suspend fun signIn(): Result<AuthSession> {
    signInCount.incrementAndGet()
    return signIns.receive()
  }

  override suspend fun signOut(): Result<Unit> {
    if (signOutCount.incrementAndGet() == 1) return startupSignOut.await()
    return laterSignOutResults.tryReceive().getOrNull() ?: Result.success(Unit)
  }
}

private class FakeUiSchemaRepository : UiSchemaRepository {
  val requests = CopyOnWriteArrayList<CompletableDeferred<Result<UiSchemaResolution>>>()
  val cancellationCount = AtomicInteger()

  override suspend fun getUiSchema(surface: SurfaceId): Result<UiSchemaResolution> {
    val response = CompletableDeferred<Result<UiSchemaResolution>>()
    requests += response
    return try {
      response.await()
    } catch (error: CancellationException) {
      cancellationCount.incrementAndGet()
      throw error
    }
  }

  override fun clearSchema() = Unit
}
