package core

/**
 * The common, read-only interface for any reactive value.
 * Consumers do not need to know if it's push or pull.
 */
interface Provider<out T> {

    /**
     * Gets the current value.
     * - For Observables, this is a cheap, cached property read.
     * - For Signals, this *may* trigger a re-computation if the value is stale.
     *
     * This function also performs dependency tracking.
     *
     * @return the current value
     */
    fun get(): T

    fun <S> map(transform: (T) -> S): Provider<S>
}

/**
 * A subscriber is any computed node (Observable or Signal)
 *  that needs to be notified of changes.
 */
interface Subscriber {

    /**
     * Stores the epoch of the last time this [Subscriber]
     * started a re-computation. This value is managed by the [DependencyTracker].
     */
    var lastRunEpoch: Long

    /**
     * Notification for **PUSH**-based subscribers (Observables).
     */
    fun notifyUpdate()

    /**
     * Notification for **PULL**-based subscribers (Signals).
     */
    fun notifyStale()
}