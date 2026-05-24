package rob.firebaseservices.coroutines

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class FirebaseMessagingDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class FirebaseMessagingCoroutineScope
