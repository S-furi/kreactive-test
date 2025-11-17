package reactive.dsl

import reactive.core.Observer
import reactive.core.Signal
import reactive.core.Source
import kotlin.reflect.KProperty

class SourceDelegateProvider<T>(
    private val initialValue: T,
) {
    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
    ): Source<T> =
        Source(name = property.name.toKebab(), initialValue = initialValue)
}

class ObservableDelegateProvider<T>(
    private val compute: () -> T,
) {
    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
    ): Observer<T> =
        Observer(name = property.name.toKebab(), compute = compute)
}

class SignalDelegateProvider<T>(
    private val compute: () -> T,
) {
    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
    ): Signal<T> =
        Signal(name = property.name.toKebab(), compute = compute)
}

private fun String.toKebab() = map { if (it.isUpperCase()) "-${it.lowercase()}" else it }.joinToString("")

fun <T> source(initialValue: T): SourceDelegateProvider<T> = SourceDelegateProvider(initialValue)

fun <T> eagerObserving(compute: () -> T): ObservableDelegateProvider<T> = ObservableDelegateProvider(compute)

fun <T> lazyObserving(compute: () -> T): SignalDelegateProvider<T> = SignalDelegateProvider(compute)