import StateMachineAlphabetConstants.EPS_SYMBOL
import kotlinx.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@Serializable
data class MachineConfiguration(
    val alphabet: String,
    val numOfStates: Int,
    val transitions: List<Triple<Int, String, Int>>,
    val startStateId: Int,
    val finalStateIds: List<Int>
)

class FiniteStateMachine(
    private var alphabet: String,
    numOfStates: Int,
    transitions: List<Triple<Int, String, Int>>,
    startStateId: Int,
    finalStateIds: List<Int>
) {

    constructor(config: MachineConfiguration) : this(
        config.alphabet,
        config.numOfStates,
        config.transitions,
        config.startStateId,
        config.finalStateIds
    )

    companion object {
        fun buildFromJson(inputStream: InputStream): FiniteStateMachine {
            val json = Json(JsonConfiguration(isLenient = true))

            val reader = BufferedReader(InputStreamReader(inputStream))

            val machineConfig = json.parse(
                MachineConfiguration.serializer(),
                reader.readLines().joinToString("\n")
            )

            return FiniteStateMachine(machineConfig)
        }

        // Accepts regular expression in Reverse Polish Notation
        fun buildFromRegex(inputStream: InputStream = System.`in`): FiniteStateMachine {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val regularExpression = reader.readLine()

            val deque = ArrayDeque<FiniteStateMachine>()

            for (c in regularExpression) {

                if (c.isLetterOrDigit()) {
                    deque.addLast(
                        FiniteStateMachine(
                            c.toString(), 2,
                            listOf(Triple(0, c.toString(), 1)),
                            0,
                            listOf(1)
                        )
                    )
                }

                when (c) {
                    '.' -> {
                        if (deque.size < 2) {
                            throw IllegalArgumentException("Illegal number of arguments in regex '.' operation")
                        }

                        val b = deque.removeLast()
                        val a = deque.removeLast()

                        deque.addLast(a * b)
                    }

                    '+' -> {
                        if (deque.size < 2) {
                            throw IllegalArgumentException("Illegal number of arguments in regex '+' operation")
                        }

                        val b = deque.removeLast()
                        val a = deque.removeLast()

                        deque.addLast(a + b)
                    }

                    '*' -> {
                        deque.addLast(deque.removeLast().kleeneStar())
                    }
                }
            }
            return deque.last
        }
    }


    enum class StateType {
        START, FINAL
    }

    class State(var transitions: HashSet<Pair<String, State>>, var type: EnumSet<StateType>, var id: Int)

    var states: ArrayList<State> = ArrayList(numOfStates)

    init {
        for (i in 0 until numOfStates) {
            states.add(State(HashSet(), EnumSet.noneOf(StateType::class.java), i))
        }

        for ((a, word, b) in transitions) {
            states[a].transitions.add(word to states[b])
        }
    }

    var startState: State = states[startStateId].apply { type.contains(StateType.START) }
    var finalStates: MutableList<State> =
        finalStateIds.map { id -> states[id].apply { type.add(StateType.FINAL) } }.toMutableList()


    fun buildDeterministicMachine(): FiniteStateMachine {
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
                if (set.any { it.type.contains(StateType.FINAL) }) EnumSet.of(StateType.FINAL)
                else EnumSet.noneOf(StateType::class.java),
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
        oldToNewStates[hashSetOf(startState)]!!.type.add(StateType.START)

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
            newStates.filter { it.type.contains(StateType.FINAL) }.map { it.id }
        )
    }

    fun removeEpsilonTransitions() {
        makeTransitiveClosure()
        addFinalStates()
        addEdges()
        for (state in states) {
            state.transitions.removeIf { it.first == EPS_SYMBOL }
        }
        finalStates = states.filter { it.type.contains(StateType.FINAL) }.toMutableList()
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
                state.transitions.any { it.first == EPS_SYMBOL && it.second.type.contains(StateType.FINAL) }
            if (hasEpsTransitionsToFinal) {
                state.type.add(StateType.FINAL)
            }
        }
    }

    private fun addEdges() {
        val usedStates = HashSet<State>()
        performDfs(startState) traverseFun@{
            usedStates.add(it)

            val newTransitions = ArrayList<Pair<String, State>>()

            for (transition in it.transitions) {
                if (transition.first == EPS_SYMBOL) {
                    newTransitions.addAll(transition.second.transitions)
                }
            }

            it.transitions.addAll(newTransitions)

            return@traverseFun it.transitions.filter { transition -> !usedStates.contains(transition.second) }
                .map { transition -> transition.second }
        }
    }

    private fun performDfs(v: State, traverseFunction: (State) -> List<State>) {
        val nextStates = traverseFunction(v)
        for (state in nextStates) {
            performDfs(state, traverseFunction)
        }
    }

    private fun getAvailableStates(startState: State, traverseFunction: (State) -> List<State>): List<State> {
        val q = ArrayDeque<State>()
        val availableStates = HashSet<State>()

        q.push(startState)

        while (q.isNotEmpty()) {
            val state = q.removeFirst()
            availableStates.add(state)
            q.addAll(traverseFunction(state).filter { !availableStates.contains(it) })
        }
        return availableStates.apply { remove(startState) }.toList()
    }

    // minimization
    fun buildTable(): HashSet<Pair<State, State>> {
        val table = HashSet<Pair<State, State>>()
        val q = ArrayDeque<Pair<State, State>>()

        val backEdges = HashMap<Pair<State, String>, HashSet<State>>()

        val usedStates = HashSet<State>()

        performDfs(startState) traverseFun@{
            usedStates.add(it)
            it.transitions.forEach { (word, state) -> backEdges.getOrPut(state to word) { HashSet() }.add(it) }
            return@traverseFun it.transitions.filter { (_, state) -> !usedStates.contains(state) }
                .map { transition -> transition.second }
        }

        for (i in states) {
            for (j in states) {
                if (!table.contains(i to j) && i.type.contains(StateType.FINAL) != j.type.contains(StateType.FINAL)) {
                    table.apply {
                        add(i to j)
                        add(j to i)
                    }
                    q.push(i to j)
                }
            }
        }

        while (q.isNotEmpty()) {
            val (u, v) = q.removeFirst()
            for (c in alphabet) {
                for (r in backEdges[u to c.toString()] ?: hashSetOf()) {
                    for (s in backEdges[v to c.toString()] ?: hashSetOf()) {
                        if (!table.contains(r to s)) {
                            table.apply {
                                add(r to s)
                                add(s to r)
                            }
                            q.push(r to s)
                        }
                    }
                }
            }
        }
        return table
    }

    fun makeComplete() {
        states.add(0, State(HashSet(), EnumSet.noneOf(StateType::class.java), 0))
        for (i in 0 until states.size) {
            states[i].id = i
        }
        for (state in states) {
            val existingWords = HashSet<String>()
            for ((word, _) in state.transitions) {
                existingWords.add(word)
            }
            for (c in alphabet) {
                if (!existingWords.contains(c.toString())) {
                    state.transitions.add(c.toString() to states[0])
                }
            }
        }
    }

    fun buildMinimalMachine(isDeterministic: Boolean = false): FiniteStateMachine {
        if (!isDeterministic) {
            return buildDeterministicMachine().buildMinimalMachine(true)
        }

        makeComplete()
        val table = buildTable()
        val component = HashMap<State, Int>()
        val reachableFromStart = HashSet<State>()

        performDfs(startState) traverseFun@{
            reachableFromStart.add(it)
            return@traverseFun it.transitions.filter { (_, state) -> !reachableFromStart.contains(state) }
                .map { (_, state) -> state }
        }

        for (state in states) {
            if (!table.contains(states[0] to state)) {
                component[state] = 0
            }
        }

        var numOfComponents = 0

        for (i in 1 until states.size) {
            if (!reachableFromStart.contains(states[i])) {
                continue
            }
            if (!component.contains(states[i])) {
                component[states[i]] = ++numOfComponents
                for (j in i + 1 until states.size) {
                    if (!table.contains(states[i] to states[j])) {
                        component[states[j]] = numOfComponents
                    }
                }
            }
        }
        return buildStateMachineOnComponents(component, numOfComponents)
    }

    private fun buildStateMachineOnComponents(
        component: HashMap<State, Int>,
        numOfComponents: Int
    ): FiniteStateMachine {
        val transitions = HashSet<Triple<Int, String, Int>>()
        for (state in states) {
            state.transitions.forEach { (word, nextState) ->
                val c1: Int? = component[state]
                val c2: Int? = component[nextState]

                if (c1 != null && c2 != null) {
                    transitions.add(Triple(c1, word, c2))
                }
            }
        }

        transitions.removeIf { it.third == component[states[0]] }

        return FiniteStateMachine(
            alphabet,
            numOfComponents,
            transitions.map { (a, word, b) ->
                Triple(a - 1, word, b - 1)
            },
            component[startState]!! - 1,
            finalStates.map { component[it]!! - 1 }.distinctBy { it })

    }

    fun accept(word: String): Boolean {
        var currentState = startState
        for (c in word) {
            val transition: Pair<String, State> =
                currentState.transitions.find { it.first == c.toString() } ?: return false
            currentState = transition.second
        }
        return currentState.type.contains(StateType.FINAL)
    }

    // Creates copies of this and other and merges their states so that they dont intersect and puts result in a
    private fun mergeCopies(other: FiniteStateMachine): Pair<FiniteStateMachine, FiniteStateMachine> {
        val a = this.copy()
        val b = other.copy()
        b.states.forEach { it.id += a.states.size }
        a.alphabet += b.alphabet
        a.alphabet = a.alphabet.toCharArray().distinct().joinToString("")
        return a to b
    }

    operator fun plus(other: FiniteStateMachine): FiniteStateMachine {
        val (a, b) = mergeCopies(other)

        val startState = State(
            hashSetOf(EPS_SYMBOL to a.startState, EPS_SYMBOL to b.startState),
            EnumSet.of(StateType.START),
            a.states.size + b.states.size
        )

        a.startState.type.remove(StateType.START)
        b.startState.type.remove(StateType.START)

        a.startState = startState
        a.states.addAll(b.states)
        a.states.add(startState)

        a.finalStates.addAll(b.finalStates)
        return a.buildMinimalMachine()
    }

    operator fun times(other: FiniteStateMachine): FiniteStateMachine {
        val (a, b) = mergeCopies(other)

        b.startState.type.remove(StateType.START)
        a.finalStates.forEach {
            it.transitions.add(EPS_SYMBOL to b.startState)
            it.type.remove(StateType.FINAL)
        }

        a.finalStates = b.finalStates
        a.states.addAll(b.states)

        return a.buildMinimalMachine()
    }

    operator fun times(str: String): FiniteStateMachine {
        val a = this.copy()


        val transitions = ArrayList<Triple<Int, String, Int>>()

        for (i in str.indices) {
            transitions.add(Triple(i, str[i].toString(), i + 1))
        }

        alphabet = str.toCharArray().distinct().joinToString("")
        val machine = FiniteStateMachine(alphabet, str.length + 1, transitions, 0, listOf(str.length))

        return a * machine
    }

    fun kleeneStar(): FiniteStateMachine {
        val a = this.copy()

        val startState = State(
            hashSetOf(EPS_SYMBOL to a.startState),
            EnumSet.of(StateType.START, StateType.FINAL),
            a.states.size
        )

        // TODO Create a function for adding a new state
        a.states.add(startState)
        a.finalStates.add(startState)

        a.startState.type.remove(StateType.START)
        a.startState = startState
        a.finalStates.forEach { it.transitions.add(EPS_SYMBOL to startState) }
        return a.buildMinimalMachine()
    }

    fun display(outputStream: OutputStream = System.out) {
        val out = PrintWriter(outputStream)

        for (state in states) {
            for (transition in state.transitions) {
                out.print(
                    "${state.id} ---(${transition.first})---> ${transition.second.id} " +
                            "(${state.type} -> ${transition.second.type})\n"
                )
            }
        }

        out.println("Start state: ${startState.id}")
        out.print("Final states: ")
        finalStates.forEach { out.print("${it.id} ") }

        out.close()
    }

    fun printEdges(outputStream: OutputStream = System.out) {
        val out = PrintWriter(outputStream)

        for (state in states) {
            for (transition in state.transitions) {
                out.println("${state.id} ${transition.second.id} ${transition.first}")
            }
        }

        out.close()
    }

    fun dumpAsJson(outputStream: OutputStream = System.out) {
        val json = Json(JsonConfiguration(prettyPrint = true))
        val out = PrintWriter(outputStream)

        val transitionList = ArrayList<Triple<Int, String, Int>>()

        for (state in states) {
            for ((word, nextState) in state.transitions) {
                transitionList.add(Triple(state.id, word, nextState.id))
            }
        }

        out.write(
            json.stringify(
                MachineConfiguration.serializer(),
                MachineConfiguration(
                    alphabet,
                    states.size,
                    transitionList,
                    startState.id,
                    finalStates.map { it.id })
            )
        )
        out.close()
    }


    fun copy(): FiniteStateMachine {
        val baos = ByteArrayOutputStream()
        dumpAsJson(baos)
        return buildFromJson(ByteArrayInputStream(baos.toByteArray()))
    }
}

object StateMachineAlphabetConstants {
    const val EPS_SYMBOL = "$"
}
