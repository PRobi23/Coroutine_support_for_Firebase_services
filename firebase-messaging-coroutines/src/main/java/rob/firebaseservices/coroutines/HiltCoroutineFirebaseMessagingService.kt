package rob.firebaseservices.coroutines

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Hilt integration for [CoroutineFirebaseMessagingService].
 *
 * Use this subclass when the app uses Hilt. Apps using another dependency injection framework can
 * extend [CoroutineFirebaseMessagingService] directly and provide [firebaseMessagingScope]
 * themselves.
 */
@AndroidEntryPoint
public abstract class HiltCoroutineFirebaseMessagingService : CoroutineFirebaseMessagingService() {

    @Inject
    @FirebaseMessagingCoroutineScope
    public lateinit var injectedFirebaseMessagingScope: CoroutineScope

    final override val firebaseMessagingScope: CoroutineScope
        get() = injectedFirebaseMessagingScope
}
