import java.util.*
import kotlin.collections.ArrayList

fun main() {
    val reader = Scanner(System.`in`)

    println("Alphabet:")
    val alphabet = reader.next()

    println("Number of states:")
    val numOfStates: Int = reader.nextInt()
    println("Number of transitions:")
    val numOfTransitions: Int = reader.nextInt()

    val transitions = ArrayList<Triple<Int, String, Int>>()

    println("Transitions:")
    for (i in 1..numOfTransitions) {
        val a = reader.nextInt()
        val word = reader.next()
        val b = reader.nextInt()
        transitions.add(Triple(a, word, b))
    }

    println("Start state:")
    val startState = reader.nextInt()

    println("Number of final states:")
    val numOfFinalStates = reader.nextInt()
    val finalStates = ArrayList<Int>()

    println("Final states:")
    for (i in 1..numOfFinalStates) {
        finalStates.add(reader.nextInt())
    }

    val machine =
        FiniteStateMachine(alphabet, numOfStates, transitions, startState, finalStates)
            .getNonDeterministicMachine()
            .getMinimalStateMachine()

    machine.display()
}