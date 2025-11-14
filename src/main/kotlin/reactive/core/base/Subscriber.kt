package reactive.core.base

/**
 * A subscriber is any computed node (Observable or Signal)
 *  that needs to be notified of changes.
 */
interface Subscriber {
    /**
     * Stores the epoch of the last time this [Subscriber]
     * started a re-computation. This value is managed by the [reactive.DependencyTracker].
     */
    var lastRunEpoch: ULong

    /**
     * Represent the depth of the execution graph when executing a [transaction][reactive.DependencyTracker.transaction].
     *
     * A [reactive.core.Source] is at depth 0 of the graph, while [observables][reactive.core.Observable] or [signals][reactive.core.Signal] directly dependent
     * on it are one level deeper, and so on for further dependencies chains.
     * The levels are used to topologically-sort the computations when [reactive.DependencyTracker] scheduler's starts picking
     * and executing actions.
     *
     * @see [reactive.DependencyTracker.runTransactions]
     */
    val level: Int

    /**
     * Notification for **PUSH**-based subscribers (Observables).
     */
    fun notifyUpdate()

    /**
     * Notification for **PULL**-based subscribers (Signals).
     */
    fun notifyStale()

    /**
     * Update the depth of this computation in the execution graph.
     *
     * @param newLevel the newLevel mapping function
     */
    fun updateLevel(newLevel: (Int) -> Int)
}
