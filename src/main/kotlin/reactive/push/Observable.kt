package reactive.push

interface Observable<T> {
    val current: T
    val observers: List<Any>

    fun onChange(registrant: Any, callback: (T) -> Unit)

    fun stopWatching(registrant: Any)
    fun <S> map(transform: (T) -> S): Observable<S> = object : Observable<S> {
        override var current: S = transform(this@Observable.current)
        override var observers: List<Any> = listOf()

        override fun onChange(registrant: Any, callback: (S) -> Unit) {
            observers += registrant
            this@Observable.onChange(this to registrant) { newValue ->
                val transformed = transform(newValue)
                if (transformed != current) {
                    current = transformed
                    callback(transformed)
                }
            }
        }

        override fun stopWatching(registrant: Any) {
            observers -= registrant
            this@Observable.stopWatching(this to registrant)
        }

        override fun toString(): String =
            "MapObservable($current)[from: ${this@Observable}]"
    }

    fun <O, R> mergeWith(other: Observable<O>, merge: (T, O) -> R): Observable<R> = object : Observable<R> {
        override var current: R = merge(this@Observable.current, other.current)
        override var observers: List<Any> = emptyList()

        override fun onChange(registrant: Any, callback: (R) -> Unit) {
            observers += registrant
            listOf(this@Observable, other).forEach { obs ->
                obs.onChange(this to registrant) { nextItem ->
                    val newValue = when (obs) {
                        this@Observable -> merge(nextItem as T, other.current)
                        else -> merge(this@Observable.current, nextItem as O)
                    }
                    if (newValue != current) {
                        current = newValue
                        callback(newValue)
                    }
                }
            }
        }

        override fun stopWatching(registrant: Any) {
            observers -= registrant
            this@Observable.stopWatching(this to registrant)
            other.stopWatching(this to registrant)
        }

        override fun toString() = "MergeObservable($current)[from: ${this@Observable}, other: $other]"
    }
}

interface MutableObservable<T> : Observable<T> {
    override var current: T

    fun update(computeNewValue: (T) -> T): T = current.also {
        current = computeNewValue(current)
    }

    companion object {
        fun <T> observe(initial: T): MutableObservable<T> = object : MutableObservable<T> {
            private val observingCallbacks: MutableMap<Any, List<(T) -> Unit>> = linkedMapOf()

            override var current: T = initial
                set(value) {
                    if (value != field) {
                        field = value
                        observingCallbacks.values.forEach { callbacks -> callbacks.forEach { it(value) } }
                    }
                }

            override val observers: List<Any> get() = observingCallbacks.keys.toList()

            override fun onChange(registrant: Any, callback: (T) -> Unit) {
                callback(current)
                observingCallbacks[registrant] = observingCallbacks[registrant]?.let {
                    it + callback
                } ?: listOf(callback)
            }

            override fun stopWatching(registrant: Any) {
                observingCallbacks.remove(registrant)
            }
        }

        fun <T> observeLazy(initial: T) : MutableObservable<T> = LazyMutableObservable(initial)
    }
}

internal class LazyMutableObservable<T>(initial: T): MutableObservable<T> {
    private var isStale: Boolean = true
    private var cachedValue: T = initial
    private var currentUpdate: ((T) -> T)? = null

    private val observersCallbacks: MutableMap<Any, List<(T) -> Unit>> = linkedMapOf()

    override var current: T = cachedValue
        get() {
            if (isStale) {
                val newValue = currentUpdate?.invoke(cachedValue) ?: cachedValue
                if (newValue != cachedValue) {
                    cachedValue = newValue
                    observersCallbacks.values.forEach { callbacks -> callbacks.forEach { it(cachedValue) } }
                }
                isStale = false
            }
            return cachedValue
        }

    override val observers: List<Any> = observersCallbacks.keys.toList()

    override fun update(computeNewValue: (T) -> T): T = cachedValue.apply {
        isStale = true
        currentUpdate = computeNewValue
    }

    override fun onChange(registrant: Any, callback: (T) -> Unit) {
        observersCallbacks[registrant] = observersCallbacks[registrant].orEmpty() + callback
    }

    override fun stopWatching(registrant: Any) {
        observersCallbacks.remove(registrant)
    }
}
