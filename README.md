# Coroutine Support For Firebase Services

This repository contains a small Android library plus an example app. The library lets Firebase
Cloud Messaging services handle messages and token refreshes with suspend functions without using
`runBlocking`.

Firebase's `FirebaseMessagingService` API is callback-based: `onMessageReceived` and `onNewToken`
are normal functions, not suspend functions. The purpose of this library is to bridge those
callbacks into a caller-provided coroutine scope so application code can call suspend APIs from FCM
handlers. Hilt support is included as an optional adapter.

## Library

The reusable code lives in the `firebase-messaging-coroutines` Android library module. It provides:

- `CoroutineFirebaseMessagingService`
- `HiltCoroutineFirebaseMessagingService`
- `onMessageReceivedSuspend(message: RemoteMessage)`
- `onNewTokenSuspend(token: String)`
- optional Hilt bindings for a service-scoped coroutine scope
- a coroutine exception handler with pluggable exception loggers

If your app uses Hilt, extend `HiltCoroutineFirebaseMessagingService`:

```kotlin
class MyFirebaseMessagingService : HiltCoroutineFirebaseMessagingService() {

    override suspend fun onMessageReceivedSuspend(message: RemoteMessage) {
        // Call suspend APIs here.
    }

    override suspend fun onNewTokenSuspend(token: String) {
        // Send the token to your backend here.
    }
}
```

If your app uses another dependency injection framework, extend the DI-neutral base class and provide
the scope yourself:

```kotlin
class MyFirebaseMessagingService : CoroutineFirebaseMessagingService() {

    override val firebaseMessagingScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun onMessageReceivedSuspend(message: RemoteMessage) {
        // Call suspend APIs here.
    }

    override suspend fun onNewTokenSuspend(token: String) {
        // Send the token to your backend here.
    }
}
```

Register the service normally:

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Hilt applications also need Hilt enabled:

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

For Hilt users, the coroutine scope is injected using the `@FirebaseMessagingCoroutineScope`
qualifier. The library binds the underlying `@FirebaseMessagingDispatcher` to `Dispatchers.IO` by
default and adds a `CoroutineExceptionHandler` to the scope.

In the Hilt adapter, unhandled coroutine exceptions are sent to every
`FirebaseMessagingExceptionLogger` contributed with Hilt multibindings. The library contributes a
default Logcat logger. Apps can add Firebase Crashlytics logging like this:

```kotlin
@Module
@InstallIn(ServiceComponent::class)
object CrashlyticsFirebaseMessagingLoggerModule {

    @Provides
    @IntoSet
    fun provideCrashlyticsFirebaseMessagingLogger(): FirebaseMessagingExceptionLogger =
        FirebaseMessagingExceptionLogger { throwable ->
            Firebase.crashlytics.recordException(throwable)
        }
}
```

Because the handlers are launched asynchronously, a slow handler does not block the Firebase SDK
callback. The scope is cancelled from `onDestroy`, so any still-running handler is cancelled when
the service is destroyed.

## Example App

The `app` module is only an example consumer. It shows:

- adding a dependency on `firebase-messaging-coroutines`
- enabling Hilt with `@HiltAndroidApp`
- registering the FCM service in the manifest
- overriding the suspend message and token handlers

Run the checks with:

```bash
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :firebase-messaging-coroutines:testDebugUnitTest :app:assembleDebug --console=plain
```
