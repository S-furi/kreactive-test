package reactive.core

import reactive.DependencyTracker
import reactive.DependencyTracker.appendToCurrentTransaction
import reactive.DependencyTracker.runAndTrack
import reactive.core.base.Computation
import reactive.core.base.Subscriber
import kotlin.concurrent.Volatile
import kotlin.reflect.KProperty
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A PULL-based (lazy) computed value.
 * It only recomputes when its value is requested *and* it has
 * been marked as stale.
 */
class Signal<T> @OptIn(ExperimentalUuidApi::class) constructor(
    private val name: String = Uuid.random().toString(),
    private val compute: () -> T,
) : Computation<T>(),
    Subscriber {
    @Volatile private var _value: T? = null

    @Volatile private var isStale: Boolean = true

    @Volatile override var lastRunEpoch: ULong = 0u

    @Volatile
    override var level: Int = 0
        private set

    override fun get(): T {
        DependencyTracker.track(this)
        if (isStale) {
            _value = runAndTrack(compute)
            isStale = false
        }
        return _value!!
    }

    override fun <S> map(transform: (T) -> S): Signal<S> = Signal("[mapped]-$name") { transform(compute()) }

    override fun notifyUpdate() {
        // no-op for PULL-based computations
    }

    override fun notifyStale() =
        appendToCurrentTransaction {
            if (!isStale) {
                isStale = true
                updateFreshSubscribers()
            }
        }

    override fun updateLevel(newLevel: (Int) -> Int) {
        level = newLevel(level)
    }

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = get()

    override fun toString(): String = "Signal-$name(value=$_value, epoch=$lastRunEpoch)"
}
