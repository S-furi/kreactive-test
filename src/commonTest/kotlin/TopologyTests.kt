@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import reactive.DependencyTracker
import reactive.core.Signal
import reactive.dsl.eagerObserving
import reactive.dsl.lazyObserving
import reactive.dsl.source

@Suppress("UNUSED")
class TopologyTests : StringSpec({
    beforeTest {
        DependencyTracker.reset()
    }

    "testing topology 1 (eager)" {
        // ┌───────►B───────┐
        // │                │
        // │                ▼
        // A ─────────────► C

        var cComputeCount = -1

        var a by source("Hello")
        val b by eagerObserving { a.uppercase() }
        val c by eagerObserving {
            cComputeCount++
            "$a, $b"
        }

        a = "Hello World!"
        cComputeCount shouldBe 1
    }


    "testing topology 1 (lazy)" {
        // ┌───────►B───────┐
        // │                │
        // │                ▼
        // A ─────────────► C

        var cComputeCount = 0

        var a by source("Hello")
        val b by lazyObserving { a.uppercase() }
        val c by lazyObserving {
            cComputeCount++
            "$a, $b"
        }

        a = "Hello World!"
        c
        b
        cComputeCount shouldBe 1

        cComputeCount = 0
        a = "Hello"
        b
        c
        cComputeCount shouldBe 1
    }

    "test Signal diamond dependency is not called twice" {
        // ┌──────►B────┐
        // │            │
        // │            │
        // │            ▼
        // A             D
        // │            ▲
        // │            │
        // └─────►C─────┘

        var a by source(10)
        var bComputeCount = 0
        var cComputeCount = 0
        var dComputeCount = 0

        val b by lazyObserving {
            bComputeCount++
            a * 2
        }
        val c by lazyObserving {
            cComputeCount++
            a + 5
        }
        val d by lazyObserving {
            dComputeCount++
            b + c
        }

        d shouldBe 35
        bComputeCount shouldBe 1
        cComputeCount shouldBe 1
        dComputeCount shouldBe 1

        a = 20
        bComputeCount shouldBe 1
        cComputeCount shouldBe 1
        dComputeCount shouldBe 1

        d shouldBe 65
        bComputeCount shouldBe 2
        cComputeCount shouldBe 2
        dComputeCount shouldBe 2
    }


    "test Observable diamond dependency is not called twice" {
        // ┌──────►B────┐
        // │            │
        // │            │
        // │            ▼
        // A             D
        // │            ▲
        // │            │
        // └─────►C─────┘

        var a by source(10)
        // starting from -1 because eager stuff triggers recomputation right away
        var dComputeCount = -1

        val b by eagerObserving { a * 2 }
        val c by eagerObserving { a + 5 }

        val d by eagerObserving {
            dComputeCount++
            b + c
        }

        a = 20

        dComputeCount shouldBe 1
    }

    "circular dependencies should be detected and fail" {
        lateinit var a: Signal<Int>
        lateinit var b: Signal<Int>

        a = Signal("A") { b.get() + 1 }

        b = Signal("B") { a.get() + 2 }

        shouldThrow<IllegalArgumentException> { a.get() }
    }
})