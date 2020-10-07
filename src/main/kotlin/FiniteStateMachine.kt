import StateMachineAlphabetConstants.EPS_SYMBOL
import java.io.OutputStream
import java.io.PrintWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class FiniteStateMachine(
    private val alphabet: String,
    numOfStates: Int,
    transitions: List<Triple<Int, String, Int>>,
    startStateId: Int,
    finalStateIds: List<Int>
) {

    enum class StateType {
        START, FINAL, MEDIATE
    }

    data class State(var transitions: HashSet<Pair<String, State>>, var type: StateType, var id: Int)

    private var states: ArrayList<State> = ArrayList(numOfStates)

    init {
        for (i in 0 until numOfStates) {
            states.add(State(HashSet(), StateType.MEDIATE, i))
        }

        for ((a, word, b) in transitions) {
            states[a].transitions.add(word to states[b])
        }
    }

    var startState = states[startStateId].apply { type = StateType.START }
    var finalStates = finalStateIds.map { id -> states[id].apply { type = StateType.FINAL } }.toList()


    fun getNonDeterministicMachine(): FiniteStateMachine {
        removeEpsilonTransitions()
        val q = ArrayDeque<HashSet<State>>()
        q.push(hashSetOf(startState))

        val transitions = HashMap<HashSet<State>, List<Pair<String, HashSet<State>>>>()

        while (q.isNotEmpty()) {
            val currentSet = q.removeFirst()
            val currentTransitions = HashMap<String, HashSet<State>>()

            for (state in currentSet) {
                for (transition in state.transitions) {
                    currentTransitions.getOrPut(transition.first) { HashSet() }.add(transition.second)
                }
            }
            transitions[currentSet] = currentTransitions.map { it.toPair() }

            currentTransitions.forEach {
                if (!transitions.contains(it.value)) {
                    q.push(it.value)
                }
            }
        }
        val oldToNewStates = HashMap<HashSet<State>, State>()
        val newStates = ArrayList<State>()

        for ((currentId, set) in transitions.keys.withIndex()) {
            val state = State(
                HashSet(),
                if (set.any { it.type == StateType.FINAL }) StateType.FINAL else StateType.MEDIATE,
                currentId
            )
            oldToNewStates[set] = state
            newStates.add(state)
        }
        for (oldState in transitions.keys) {
            val newState: State = oldToNewStates[oldState]!!
            for (oldTransition in transitions[oldState]!!) {
                newState.transitions.add(oldTransition.first to oldToNewStates[oldTransition.second]!!)
            }
        }
        oldToNewStates[hashSetOf(startState)]!!.type = StateType.START

        val newTransitions = ArrayList<Triple<Int, String, Int>>()

        for (state in newStates) {
            for (transition in state.transitions) {
                newTransitions.add(Triple(state.id, transition.first, transition.second.id))
            }
        }

        return FiniteStateMachine(alphabet,
            newStates.size,
            newTransitions,
            oldToNewStates[hashSetOf(startState)]!!.id,
            newStates.filter { it.type == StateType.FINAL }.map { it.id }
        )
    }

    private fun removeEpsilonTransitions() {
        makeTransitiveClosure()
        addFinalStates()
        addEdges()
        for (state in states) {
            state.transitions.removeIf { it.first == EPS_SYMBOL }
        }
    }

    private fun makeTransitiveClosure() {
        for (state in states) {
            val availableByEpsTransitions =
                getAvailableStates(state) { currentState ->
                    val epsTransitions =
                        currentState.transitions.filter { transition -> transition.first == EPS_SYMBOL }
                    epsTransitions.map { it.second }
                }
            for (s in availableByEpsTransitions) {
                state.transitions.add(EPS_SYMBOL to s)
            }
        }
    }

    private fun addFinalStates() {
        for (state in states) {
            val hasEpsTransitionsToFinal =
                state.transitions.any { it.first == EPS_SYMBOL && it.second.type == StateType.FINAL }
            if (hasEpsTransitionsToFinal) {
                state.type = StateType.FINAL
            }
        }
    }

    private fun addEdges() {
        val usedStates = HashSet<State>()
        performDfs(startState) {
            usedStates.add(it)
            var newTransitions = ArrayList<Pair<String, State>>()

//            for (transition in it.transitions) {
//                if (transition.first == EPS_SYMBOL) {
//                    newTransitions.addAll(transition.second.transitions)
//                }
//            }
//
//            it.transitions.addAll(newTransitions)
//            it.transitions.filter { transition -> !usedStates.contains(transition.second) }
//                .map { transition -> transition.second }
            arrayListOf()
        }
    }

    private fun performDfs(v: State, traverseFunction: (State) -> List<State>) {
        var nextStates = traverseFunction(v)
        for (state in nextStates) {
            //println(state)
            //performDfs(state, traverseFunction)
        }
    }

    private fun getAvailableStates(v: State, traverseFunction: (State) -> List<State>): List<State> {
        val q = ArrayDeque<State>()
        val availableStates = HashSet<State>()

        q.push(v)

        while (q.isNotEmpty()) {
            val state = q.removeFirst()
            availableStates.add(state)
            q.addAll(traverseFunction(state).filter { !availableStates.contains(it) })
        }
        return availableStates.toList()
    }

    fun display(outputStream: OutputStream = System.out) {
        val out = PrintWriter(outputStream)

        for (state in states) {
            for (transition in state.transitions) {
                out.print(
                    "${state.id} (${transition.first})---> ${transition.second.id} " +
                            "(${state.type} -> ${transition.second.type})\n"
                )
            }
        }

        out.println("Start state: ${startState.id}")
        out.print("Final states: ")
        finalStates.forEach { out.print("${it.id} ") }

        out.close()
    }
}

object StateMachineAlphabetConstants {
    const val EPS_SYMBOL = "$"
}
