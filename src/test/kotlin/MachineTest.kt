import FiniteStateMachine.State
import LoadingTools.Companion.getFullResourcePath
import LoadingTools.Companion.readLines
import TestConfigInfo.PATH_TO_TESTS_CONFIG
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.parseMap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object TestConfigInfo {
    const val PATH_TO_TESTS_CONFIG = "test_paths_config.json"
}

class MachineTest {

    data class TestDataEntry(
        val input: FiniteStateMachine,
        val determined: FiniteStateMachine,
        val minimized: FiniteStateMachine
    )

    lateinit var withEpsilonTransitions: List<TestDataEntry>
    lateinit var withoutEpsilonTransitions: List<TestDataEntry>

    @Serializable
    data class TestInfoEntry(
        @SerialName("input") var pathToInput: String,
        @SerialName("determined") var pathToDetermined: String,
        @SerialName("minimized") var pathToMinimized: String
    )

    @ImplicitReflectionSerializer
    @BeforeEach
    fun setupTestData() {
        val testsConfigurationRawData =
            Json.parseMap<String, JsonArray>(readLines(PATH_TO_TESTS_CONFIG).joinToString("\n"))


        val testsConfiguration =
            testsConfigurationRawData.mapValues { (_, testCategory) ->
                testCategory.jsonArray.map {
                    Json.fromJson(
                        TestInfoEntry.serializer(),
                        it
                    )
                }
            }

        val machineLoader: ((TestInfoEntry) -> TestDataEntry) = { testEntry ->
            TestDataEntry(
                FiniteStateMachine.buildFromJson(FileInputStream(getFullResourcePath(testEntry.pathToInput))),
                FiniteStateMachine.buildFromJson(FileInputStream(getFullResourcePath(testEntry.pathToDetermined))),
                FiniteStateMachine.buildFromJson(FileInputStream(getFullResourcePath(testEntry.pathToMinimized)))
            )
        }

        withEpsilonTransitions = testsConfiguration["withEpsilonTransitions"]?.map(machineLoader)!!
        withoutEpsilonTransitions = testsConfiguration["withoutEpsilonTransitions"]?.map(machineLoader)!!
    }

    private fun generalDeterminationTest(data: List<TestDataEntry>) {
        data.forEach {
            assertMachinesEqual(it.input.buildDeterministicMachine(), it.determined)
        }
    }

    private fun generalMinimizationTest(data: List<TestDataEntry>) {
        data.forEach {
            assertMachinesEqual(it.input.buildMinimalMachine(), it.minimized)
        }
    }

    @Test
    fun testDeterminationWithEpsilonTransitions() {
        generalDeterminationTest(withEpsilonTransitions)
    }

    @Test
    fun testDeterminationWithoutEpsTransitions() {
        generalDeterminationTest(withoutEpsilonTransitions)
    }

    @Test
    fun testMinimizationWithEpsTransitions() {
        generalMinimizationTest(withEpsilonTransitions)
    }

    @Test
    fun testMinimizationWithoutEpsTransitions() {
        generalMinimizationTest(withoutEpsilonTransitions)
    }


    private fun assertMachinesEqual(firstMachine: FiniteStateMachine, secondMachine: FiniteStateMachine) {
        assert(firstMachine.states.size == secondMachine.states.size)
        assert(firstMachine.finalStates.size == secondMachine.finalStates.size)
        assert(checkIsomorphism(firstMachine, secondMachine))
    }

    private fun checkIsomorphism(firstMachine: FiniteStateMachine, secondMachine: FiniteStateMachine): Boolean {
        val q = ArrayDeque<Pair<State, State>>()
        val used = HashSet<Pair<State, State>>()
        val stateMap = HashMap<State, State>()

        q.push(firstMachine.startState to secondMachine.startState)

        while (q.isNotEmpty()) {
            val (a, b) = q.removeFirst()
            used.add(a to b)
            stateMap[a] = b
            val equalStates = HashMap<String, Pair<Int, Int>>()

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
                val state1 = firstMachine.states[p.first]
                val state2 = secondMachine.states[p.second]
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
}
