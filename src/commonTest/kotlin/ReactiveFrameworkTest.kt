@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import reactive.DependencyTracker
import reactive.core.Observer
import reactive.core.Signal
import reactive.core.Source
import reactive.dsl.eagerObserving
import reactive.dsl.lazyObserving
import reactive.dsl.source


class ReactiveFrameworkTest : StringSpec({
    beforeTest {
        DependencyTracker.reset()
    }

    "it should be possibile to create a source and observe it eagerly" {
        var observerComputedCounter = 0

        val source = Source(10.0)
        val observer = Observer {
            observerComputedCounter++
            source.get() * 20
        }

        observerComputedCounter shouldBe 1
        source.update { it + 10.0 }
        observerComputedCounter shouldBe 2
        observer.get() shouldBe (400.0 plusOrMinus 1e-4)
    }

    "it should be possibile to create a source and observe it lazily" {
        var signalComputeCount = 0

        val source = Source(10.0)
        val observer = Signal {
            signalComputeCount++
            source.get() * 20
        }

        signalComputeCount shouldBe 0
        source.update { it + 10.0 }
        signalComputeCount shouldBe 0
        observer.get() shouldBe (400.0 plusOrMinus 1e-4)
        signalComputeCount shouldBe 1
        observer.get() shouldBe (400.0 plusOrMinus 1e-4)
        signalComputeCount shouldBe 1
    }

    "testing sources setting idempotency" {
        var a by source(10)
        var computeCount = 0
        val b by eagerObserving {
            computeCount++
            a
        }

        a = 10

        computeCount shouldBe 1
    }

    "an observer can depend on another observer" {
        var a by source("Hello")
        val b by eagerObserving {
            println(a)
            a.uppercase()
        }
        val c by eagerObserving {
            println("B computed from B: $b")
            "$b, World!"
        }

        a = "hello"
        c shouldBe "HELLO, World!"
    }


    "signals (lazy-pull) should not compute if value is not requested through 'get()'" {
        var a by source(20)
        var pullComputeCount = 0

        val signal by lazyObserving {
            pullComputeCount++
            a * 10
        }

        a = 20
        a = 30

        pullComputeCount shouldBe 0
    }

    "an observable that depends on a signal should make it act as a push dependency" {
        var a by source(10)
        var signalComputeCount = 0

        val pullSignal by lazyObserving {
            signalComputeCount++
            a * 2
        }

        val pushObservable by eagerObserving { pullSignal }

        a = 30

        signalComputeCount shouldBe 2
        pushObservable shouldBe 60
    }

    "exception in Observable is not swallowed" {
        var a by source(10)
        val b by eagerObserving {
            require(a < 15) { "Test Exception" }
            a
        }

        shouldThrowMessage("Test Exception") {
            a = 20
        }
    }

    "exception in Signals is not swallowed" {
        var a by source(10)
        val b by lazyObserving{
            require(a < 15) { "Test Exception" }
            a
        }

        a = 20

        shouldThrowMessage("Test Exception") {
            val value = b
        }
    }

    "test Push (Observable) vs Pull (Signal) behavior()" {
        var simulationTime by source(0.0)
        var eventQueueCount by source(0)
        var pushComputeCount = 0
        var pullComputeCount = 0

        val canRunNextEvent by eagerObserving {
            pushComputeCount++
            val time = simulationTime
            val count = eventQueueCount
            time < 100.0 && count > 0
        }

        val expensiveStatistic by lazyObserving {
            pullComputeCount++
            val time = simulationTime
            (1..100).fold(time) { acc, i -> acc + (i % 10) * 0.01 }
        }

        pushComputeCount shouldBe 1
        pullComputeCount shouldBe 0

        canRunNextEvent shouldBe false

        simulationTime = 1.0

        pushComputeCount shouldBe 2
        pullComputeCount shouldBe 0

        eventQueueCount = 5

        pushComputeCount shouldBe 3
        canRunNextEvent shouldBe true
        pullComputeCount shouldBe 0

        val stat1 = expensiveStatistic

        pullComputeCount shouldBe 1

        stat1 shouldBeGreaterThan 1.0

        val stat2 = expensiveStatistic

        pullComputeCount shouldBe 1
        stat2 shouldBe stat1

        simulationTime = 2.0

        pushComputeCount shouldBe 4
        pullComputeCount shouldBe 1

        val stat3 = expensiveStatistic

        pullComputeCount shouldBe 2
        stat1 shouldNotBe stat3
    }

    "test dynamic dependencies and lazy unlinking" {
        var cond by source(true)
        var a by source("A")
        var b by source("B")
        var computeCount = 0

        val computed by eagerObserving {
            computeCount++
            if (cond) a else b
        }

        computeCount shouldBe 1
        computed shouldBe a

        a = "A2"

        computeCount shouldBe 2
        computed shouldBe a

        b = "B2"

        computeCount shouldBe 2
        computed shouldBe a

        cond = false

        computeCount shouldBe 3
        computed shouldBe b

        a = "A3"

        computeCount shouldBe 3
        computed shouldBe b

        b = "B3"

        computeCount shouldBe 4
        computed shouldBe b
    }

    "guardrail should prevent mutation inside a computed block (observable or signal)" {
        var a by source(10.0)
        var b by source(false)

        val comp by eagerObserving {
            if (b) {
                a += 10
            }
        }

        val lazyComp by lazyObserving {
            if (b) {
                a += 10
            }
        }

        val exception = shouldThrow<IllegalStateException> {
                b = true
            }

        shouldThrow<IllegalStateException> {
            println(lazyComp)
        }

        exception.message shouldContain "Cannot set Source"
    }
})
