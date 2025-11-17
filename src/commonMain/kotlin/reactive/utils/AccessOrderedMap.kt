package reactive.utils

/**
 * A simple implementation of what's being achieved using a
 * [LinkedHashMap(accessOrder = true)][LinkedHashMap] just for putting values,
 * as it is not available in Kotlin multiplatform 🤷‍♂️
 *
 * Please note that this implementation is not actually the same,
 * because elements are not reordered on [querying][get]
 */
class AccessOrderedMap<K, V>(
    private val backingMap: MutableMap<K, V> = HashMap()
) : MutableMap<K, V> by backingMap {
    private val order = ArrayDeque<K>()

    override fun put(key: K, value: V): V? =
        backingMap.put(key, value).takeIf { it != null }
            ?.apply { order.remove(key) }
            .apply { order.addLast(key) }

    override fun remove(key: K): V? = backingMap.remove(key).apply { order.remove(key) }

    override fun clear() {
        backingMap.clear()
        order.clear()
    }
}