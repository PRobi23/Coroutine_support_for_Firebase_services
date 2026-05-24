package rob.firebaseservices.coroutines

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Firebase messaging service base class that lets applications handle FCM callbacks with
 * suspending functions.
 *
 * Callback work is launched asynchronously, and [firebaseMessagingScope] is canceled in [onDestroy].
 * Hilt users can extend [HiltCoroutineFirebaseMessagingService]. Apps using another dependency
 * The injection framework can extend this class directly and provide the scope, however, they prefer.
 */
abstract class CoroutineFirebaseMessagingService : FirebaseMessagingService() {

    protected abstract val firebaseMessagingScope: CoroutineScope

    final override fun onMessageReceived(message: RemoteMessage) {
        firebaseMessagingScope.launch {
            onMessageReceivedSuspend(message)
        }
    }

    final override fun onNewToken(token: String) {
        firebaseMessagingScope.launch {
            onNewTokenSuspend(token)
        }
    }

    public override fun onDestroy() {
        firebaseMessagingScope.cancel()
        super.onDestroy()
    }

    protected open suspend fun onMessageReceivedSuspend(message: RemoteMessage) = Unit

    protected open suspend fun onNewTokenSuspend(token: String) = Unit
}
