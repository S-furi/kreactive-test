package reactive.core

import reactive.DependencyTracker
import reactive.core.base.Computation
import java.util.UUID
import kotlin.reflect.KProperty

/**
 * A writeable [reactive.core.base.Provider].
 *
 * This is the start of a reactive chain. When its value changes (by
 * means of [set]) it notifies both push and pull subscribers.
 */
class Source<T>(
    initialValue: T,
    private val name: String = UUID.randomUUID().toString(),
) : Computation<T>() {
    @Volatile private var current: T = initialValue

    override fun get(): T {
        DependencyTracker.track(this)
        return current
    }

    fun set(newValue: T) {
        require(!DependencyTracker.isCurrentlyTracking()) {
            "Cannot set Source '$name' (value: $newValue) from within a computed block (Observable or Signal)."
        }

        DependencyTracker.transaction {
            if (current == newValue) return@transaction
            current = newValue
            updateFreshSubscribers()
        }
    }

    fun update(newValueFunction: (T) -> T) {
        set(newValueFunction(current))
    }

    override fun <S> map(transform: (T) -> S): Source<S> = Source(transform(current))

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = get()

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T,
    ) = set(value)

    override fun toString(): String = "SOURCE-$name(value=$current)"
}
