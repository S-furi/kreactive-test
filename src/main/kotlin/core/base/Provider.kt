package core.base

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
