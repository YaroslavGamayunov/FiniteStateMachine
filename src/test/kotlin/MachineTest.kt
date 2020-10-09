import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder
import java.net.URL
import java.nio.file.Paths
import java.util.ArrayDeque
import FiniteStateMachine.State

class MachineTest {
    @Test
    fun testDeterminationWithEpsilonTransitions() {
        var numOfTests = 2
        for (i in 1..numOfTests) {
            val currentMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("machines/machine_with_eps_transitions_${i}.json"))
                    .getDeterministicMachine()
            val rightMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("answers/machine_with_eps_transitions_${i}_ans_determined.json"))

            assertMachinesEqual(currentMachine, rightMachine)
        }
    }

    @Test
    fun testDeterminationWithoutEpsTransitions() {
        var numOfTests = 2
        for (i in 1..numOfTests) {
            val currentMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("machines/machine_without_eps_transitions_${i}.json"))
                    .getDeterministicMachine()
            val rightMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("answers/machine_without_eps_transitions_${i}_ans_determined.json"))

            assertMachinesEqual(currentMachine, rightMachine)
        }
    }

    @Test
    fun testMinimizationWithoutEpsTransitions() {
        var numOfTests = 2
        for (i in 1..numOfTests) {
            val currentMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("machines/machine_without_eps_transitions_${i}.json"))
                    .getDeterministicMachine().getMinimalStateMachine()
            val rightMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("answers/machine_without_eps_transitions_${i}_ans_minimized.json"))

            assertMachinesEqual(currentMachine, rightMachine)
        }
    }

    @Test
    fun testMinimizationWithEpsTransitions() {
        var numOfTests = 2
        for (i in 1..numOfTests) {
            val currentMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("machines/machine_with_eps_transitions_${i}.json"))
                    .getDeterministicMachine().getMinimalStateMachine()
            val rightMachine =
                FiniteStateMachine.buildFromJson(getFullResourcePath("answers/machine_with_eps_transitions_${i}_ans_minimized.json"))

            assertMachinesEqual(currentMachine, rightMachine)
        }
    }


    private fun assertMachinesEqual(firstMachine: FiniteStateMachine, secondMachine: FiniteStateMachine) {
        assert(firstMachine.states.size == secondMachine.states.size)
        assert(firstMachine.finalStates.size == secondMachine.finalStates.size)
        assert(checkIsomorphism(firstMachine, secondMachine))
    }

    private fun checkIsomorphism(firstMachine: FiniteStateMachine, secondMachine: FiniteStateMachine): Boolean {
        var q = ArrayDeque<Pair<State, State>>()
        var used = HashSet<Pair<State, State>>()
        var stateMap = HashMap<State, State>()

        q.push(firstMachine.startState to secondMachine.startState)

        while (q.isNotEmpty()) {
            var (a, b) = q.removeFirst()
            used.add(a to b)
            stateMap[a] = b
            var equalStates = HashMap<String, Pair<Int, Int>>()

            for ((word, nextState) in a.transitions) {
                equalStates.getOrPut(word) { -1 to -1 }
                if (equalStates[word]!!.first != -1) {
                    return false
                }
                equalStates[word] = equalStates[word]!!.copy(first = nextState.id)
            }

            for ((word, nextState) in b.transitions) {
                equalStates.getOrPut(word) { -1 to -1 }
                if (equalStates[word]!!.second != -1) {
                    return false
                }
                equalStates[word] = equalStates[word]!!.copy(second = nextState.id)
            }

            if (equalStates.any { (_, p) -> p.first == -1 || p.second == -1 }) {
                return false
            }

            equalStates.forEach { (_, p) ->
                var state1 = firstMachine.states[p.first]
                var state2 = secondMachine.states[p.second]
                if (stateMap.contains(state1) && stateMap[state1] != state2) {
                    return false
                }
                stateMap[state1] = state2
                if (!used.contains(state1 to state2)) {
                    q.push(state1 to state2)
                }
            }
        }
        return true
    }

    private fun getFullResourcePath(pathToResource: String): String {
        val res = this::class.java.classLoader.getResource(pathToResource)
        val file = Paths.get(res.toURI()).toFile()
        return file.absolutePath
    }
}
