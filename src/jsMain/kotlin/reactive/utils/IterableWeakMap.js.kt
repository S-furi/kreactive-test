package reactive.utils

/**
 * Please note that with JS it's hard to have a behaviour similar
 * to what Java's WeakHashMap is providing, since JS's WeakMap
 * is not iterable by design.
 */
actual typealias IterableWeakMap<K, V> = HashMap<K, V>