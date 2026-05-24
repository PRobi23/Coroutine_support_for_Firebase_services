package rob.firebaseservices.coroutines

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(ServiceComponent::class)
public object FirebaseMessagingCoroutineModule {

    @Provides
    @FirebaseMessagingDispatcher
    public fun provideFirebaseMessagingDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @IntoSet
    public fun provideFirebaseMessagingExceptionLogger(): FirebaseMessagingExceptionLogger =
        LogcatFirebaseMessagingExceptionLogger

    @Provides
    @ServiceScoped
    @FirebaseMessagingCoroutineScope
    public fun provideFirebaseMessagingCoroutineScope(
        @FirebaseMessagingDispatcher dispatcher: CoroutineDispatcher,
        exceptionLoggers: Set<@JvmSuppressWildcards FirebaseMessagingExceptionLogger>,
    ): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            exceptionLoggers.forEach { logger ->
                logger.recordException(throwable)
            }
        }
        return CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)
    }
}

public fun interface FirebaseMessagingExceptionLogger {
    public fun recordException(throwable: Throwable)
}

private object LogcatFirebaseMessagingExceptionLogger : FirebaseMessagingExceptionLogger {
    override fun recordException(throwable: Throwable) {
        Log.e("FirebaseMessagingCoroutines", "Unhandled Firebase messaging coroutine exception", throwable)
    }
}
