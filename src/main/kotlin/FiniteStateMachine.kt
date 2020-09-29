class FiniteStateMachine(val alphabet: String, states: ArrayList<State>) {

    enum class StateType {
        START, FINAL, MEDIATE
    }

    data class State(var transitions: ArrayList<Pair<Char, State>>, var type: StateType)

    var startState = states.find { it.type == StateType.START }
    var finalStates = states.filter { it.type == StateType.FINAL }
}