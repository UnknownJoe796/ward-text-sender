package com.ivieleague.textcannon

import android.location.Criteria
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.lightningkite.kotlin.anko.requestSingleUpdate
import com.lightningkite.kotlin.anko.viewcontrollers.implementations.VCActivity
import kotlinx.coroutines.experimental.*
import org.jetbrains.anko.locationManager
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun <T> suspendCoroutineLambda(action: ((T) -> Unit) -> Unit) = suspendCoroutine<T> { cont -> action { cont.resume(it) } }

suspend fun <T> (() -> T).invokeSuspend() = suspendCoroutine<T> { cont -> async(CommonPool) { cont.resume(this@invokeSuspend.invoke()) } }

fun locationTestCoroutine(activity: VCActivity) {
    launch(UI) {
        val permissionHad = suspendCoroutineLambda<Boolean> { activity.requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, it) }
        if (!permissionHad) return@launch
        val result = suspendCoroutineLambda<Location> { activity.locationManager.requestSingleUpdate(Criteria(), it) }
        println(result.toString())
    }
}

/**
 * Dispatches execution onto Android main UI thread and provides native [delay][Delay.delay] support.
 */
val UI = HandlerContext(Handler(Looper.getMainLooper()), "UI")

/**
 * Represents an arbitrary [Handler] as a implementation of [CoroutineDispatcher].
 */
fun Handler.asCoroutineDispatcher() = HandlerContext(this)

/**
 * Implements [CoroutineDispatcher] on top of an arbitrary Android [Handler].
 * @param handler a handler.
 * @param name an optional name for debugging.
 */
public class HandlerContext(
        private val handler: Handler,
        private val name: String? = null
) : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        handler.post(block)
    }

    override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) {
        handler.postDelayed({
            with(continuation) { resumeUndispatched(Unit) }
        }, unit.toMillis(time))
    }

    override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle {
        handler.postDelayed(block, unit.toMillis(time))
        return object : DisposableHandle {
            override fun dispose() {
                handler.removeCallbacks(block)
            }
        }
    }

    override fun toString() = name ?: handler.toString()
    override fun equals(other: Any?): Boolean = other is HandlerContext && other.handler === handler
    override fun hashCode(): Int = System.identityHashCode(handler)
}