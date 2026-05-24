package rob.firebaseservices.coroutines

import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineFirebaseMessagingServiceTest {

    @Test
    fun `onMessageReceived launches the suspend message handler`() = runTest {
        val service = RecordingService(testScope = serviceTestScope())
        val message = RemoteMessage.Builder("test").build()

        service.onMessageReceived(message)
        runCurrent()
        advanceTimeBy(1)
        runCurrent()

        assertThat(service.events).containsExactly("message")
    }

    @Test
    fun `onNewToken launches the suspend token handler`() = runTest {
        val service = RecordingService(testScope = serviceTestScope())

        service.onNewToken("token-123")
        runCurrent()
        advanceTimeBy(1)
        runCurrent()

        assertThat(service.events).containsExactly("token:token-123")
    }

    @Test
    fun `handler exception is reported to the injected exception logger`() = runTest {
        val expectedException = IllegalStateException("Token sync failed")
        val logger = RecordingExceptionLogger()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = ThrowingService(
            firebaseMessagingScope = FirebaseMessagingCoroutineModule.provideFirebaseMessagingCoroutineScope(
                dispatcher = dispatcher,
                exceptionLoggers = setOf(logger),
            ),
            throwable = expectedException,
        )

        service.onNewToken("token-123")
        advanceUntilIdle()

        assertThat(logger.exceptions).containsExactly(expectedException)
    }

    @Test
    fun `onDestroy cancels a running suspend handler`() = runTest {
        val service = SlowService(testScope = serviceTestScope())

        service.onMessageReceived(RemoteMessage.Builder("test").build())
        runCurrent()
        assertThat(service.started).isTrue()

        service.onDestroy()
        runCurrent()

        assertThat(service.cancelled).isTrue()
        assertThat(service.completed).isFalse()
    }

    @Test
    fun `slow suspend handler does not block the Firebase callback`() = runTest {
        val service = SlowService(testScope = serviceTestScope())

        service.onNewToken("token-123")
        runCurrent()

        assertThat(service.started).isTrue()
        assertThat(service.completed).isFalse()

        advanceTimeBy(99)
        runCurrent()
        assertThat(service.completed).isFalse()

        advanceTimeBy(1)
        runCurrent()
        assertThat(service.completed).isTrue()
    }

    private fun TestScope.serviceTestScope(): TestScope =
        TestScope(StandardTestDispatcher(testScheduler))

    private class RecordingService(
        testScope: TestScope,
    ) : CoroutineFirebaseMessagingService() {

        override val firebaseMessagingScope: CoroutineScope = testScope

        val events = mutableListOf<String>()

        override suspend fun onMessageReceivedSuspend(message: RemoteMessage) {
            delay(1)
            events += "message"
        }

        override suspend fun onNewTokenSuspend(token: String) {
            delay(1)
            events += "token:$token"
        }
    }

    private class ThrowingService(
        override val firebaseMessagingScope: CoroutineScope,
        private val throwable: Throwable,
    ) : CoroutineFirebaseMessagingService() {

        override suspend fun onNewTokenSuspend(token: String) {
            throw throwable
        }
    }

    private class SlowService(
        testScope: TestScope,
    ) : CoroutineFirebaseMessagingService() {

        override val firebaseMessagingScope: CoroutineScope = testScope

        @Volatile
        var started = false
            private set

        @Volatile
        var completed = false
            private set

        @Volatile
        var cancelled = false
            private set

        override suspend fun onMessageReceivedSuspend(message: RemoteMessage) {
            runSlowHandler()
        }

        override suspend fun onNewTokenSuspend(token: String) {
            runSlowHandler()
        }

        private suspend fun runSlowHandler() {
            started = true
            try {
                delay(100)
                completed = true
            } catch (exception: CancellationException) {
                cancelled = true
                throw exception
            }
        }
    }

    private class RecordingExceptionLogger : FirebaseMessagingExceptionLogger {

        val exceptions = mutableListOf<Throwable>()

        override fun recordException(throwable: Throwable) {
            exceptions += throwable
        }
    }
}
