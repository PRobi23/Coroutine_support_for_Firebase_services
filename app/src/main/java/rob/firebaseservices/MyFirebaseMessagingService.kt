package rob.firebaseservices

import com.google.firebase.messaging.RemoteMessage
import rob.firebaseservices.coroutines.HiltCoroutineFirebaseMessagingService

class MyFirebaseMessagingService : HiltCoroutineFirebaseMessagingService() {

    override suspend fun onMessageReceivedSuspend(message: RemoteMessage) {
        // Handle the message with suspend APIs here.
    }

    override suspend fun onNewTokenSuspend(token: String) {
        // Send the token to your backend with suspend APIs here.
    }
}
