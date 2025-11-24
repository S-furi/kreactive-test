package perf

import org.junit.jupiter.api.Test
import reactive.DependencyTracker
import reactive.core.Observer
import reactive.core.Signal
import reactive.core.Source
import reactive.push.MutableObservable.Companion.observe
import reactive.push.Observable
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

object PerformanceBenchmark {
    const val CHAIN_DEPTH = 100
    const val ITERATIONS = 100_000
    const val FANOUT_DEPTH = 1_000

    fun `Benchmarking push (Fan-Out)`(): Duration {
        val source = observe(0)
        var count = 0L

        repeat(FANOUT_DEPTH) {
            val registrant = Any()
            source.onChange(registrant) { item -> count += item}
        }

        val time = measureNanoTime {
            repeat(ITERATIONS) {
                source.update { it + 1}
            }
        }.nanoseconds

        check(count > 0)
        return time
    }

    fun `Benchmarking eager computation (Fan-Out)`(): Duration {
        var source = Source(0)
        var count = 0
        val refs = ArrayList<Observer<Unit>>(FANOUT_DEPTH)
        repeat(FANOUT_DEPTH) {
            refs.add(Observer {
                count += source.get()
            })
        }

        val time = measureNanoTime {
            repeat(ITERATIONS) {
                source.update { it + 1 }
            }
        }.nanoseconds

        check(count > 0)
        return time
    }

    fun `Benchmarking push (Chain)`(): Duration {
        val root = observe(0)
        var tail: Observable<Int> = root
        repeat(CHAIN_DEPTH) {
            tail = tail.map { it + 1}
        }
        var count = 0L
        tail.onChange("final_observer") { count += it }

        val time = measureNanoTime {
            repeat(ITERATIONS) { root.update { it + 1 } }
        }.nanoseconds
        check(count > 0)
        return time
    }

    fun `Benchmarking eager computation (Chain)`(): Duration {
        var root = Source(0)
        var tail = Observer { root.get() }
        val chainRefs = ArrayList<Observer<Int>>(CHAIN_DEPTH)
        chainRefs.add(tail)
        repeat(CHAIN_DEPTH) {
            val next = tail.map { it + 1 }
            tail = next
            chainRefs.add(next)
        }

        var count = 0L
        val end = Observer { count += tail.get() }

        val time = measureNanoTime {
            repeat(ITERATIONS) { root.update { it + 1} }
        }.nanoseconds
        check(count > 0)
        return time
    }

    fun `Benchmarking lazy computation (Chain)`(): Duration {
        var root = Source(0)
        var tail = Signal { root.get() }
        val chainRefs = ArrayList<Signal<Int>>(CHAIN_DEPTH)
        chainRefs.add(tail)
        repeat(CHAIN_DEPTH) {
            val next = tail.map { it + 1 }
            tail = next
            chainRefs.add(next)
        }

        var count = 0L
        val end = Signal { count += tail.get() }

        val time = measureNanoTime {
            repeat(ITERATIONS) {
                root.update { it + 1}
                end.get() // FIX: Pull value here to force computation!
            }
        }.nanoseconds
        check(count > 0)
        return time
    }
}

class BenchmarkRunner {
    @Test
    fun `source with 1000 subscribers update medium test (100000 updates)`() {
        println("Warming up...")
        PerformanceBenchmark.`Benchmarking push (Fan-Out)`()
        PerformanceBenchmark.`Benchmarking eager computation (Fan-Out)`()
        PerformanceBenchmark.`Benchmarking push (Chain)`()
        PerformanceBenchmark.`Benchmarking eager computation (Chain)`()

        DependencyTracker.reset()

        println("--- Fan-Out Results (1 Source -> ${PerformanceBenchmark.FANOUT_DEPTH} Observers) ---")
        println("Push  : ${PerformanceBenchmark.`Benchmarking push (Fan-Out)`()}")
        println("Eager : ${PerformanceBenchmark.`Benchmarking eager computation (Fan-Out)`()}")

        println("\n--- Chain Results (Depth ${PerformanceBenchmark.CHAIN_DEPTH}) ---")
        println("Push  : ${PerformanceBenchmark.`Benchmarking push (Chain)`()}")
        println("Eager : ${PerformanceBenchmark.`Benchmarking eager computation (Chain)`()}")
        println("Signal: ${PerformanceBenchmark.`Benchmarking lazy computation (Chain)`()}")
    }
}