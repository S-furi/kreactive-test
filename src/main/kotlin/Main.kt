import core.Observable
import dsl.eagerObserving
import dsl.getAccessibleDelegates
import core.Source

data class Molecule(val name: String)

class Node<T> {
    private val contents = mutableMapOf<Molecule, Source<T>>()

    fun upsert(molecule: Molecule, newValue: T) {
        println("Upserting mol=$molecule, value=$newValue")
        contents[molecule] = Source(newValue)
    }

    fun get(molecule: Molecule): Source<T>? = contents[molecule]
}

object Main {
    val molA = Molecule("Birbo")
    val node = Node<Double>()

    val timeDistribution by eagerObserving {
        val percentage = node.get(molA)?.map { it * 100 }?.get() ?: 0.0


        if (percentage > 50) {
            println("triggering stuff!")
            percentage / 100 * 0.1
        } else {
            println("I'm not gonna do something, percentage=$percentage")
            null
        }
    }

    fun ciccione() {
        node.upsert(molA, 10.0)

        val timeDistributionAsMinutes = (::timeDistribution).getAccessibleDelegates<Observable<Double?>>()?.map {
            (it ?: 0.0) / 60
        } ?: error("cannot retrieve backing observable")

        println(timeDistributionAsMinutes)
    }
}

fun main() {
    Main.ciccione()
}