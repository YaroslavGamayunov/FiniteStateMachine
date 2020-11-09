import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min

/**
 * The problem:
 * Given a regex in Reverse Polish Notation and numbers k and l satisfying the property: 0 <= l < k
 * The task is to find minimal n which equals to l modulo k
 * such that language of regex contains n-character long words
 */

fun solveProblem(regex: String, k: Int, l: Int): Int {

    val machine = FiniteStateMachine.buildFromRegex(regex.byteInputStream())

    val dp = HashMap<FiniteStateMachine.State, ArrayList<TreeSet<Int>>>()

    for (state in machine.states) {
        fillDpCell(dp, state, k)
    }

    dp[machine.startState]!![0].add(0)

    for (i in 0..(k * k)) {
        for (state in machine.states) {
            if (dp[state]!![i % k].contains(i)) {
                for ((_, nextState) in state.transitions) {
                    dp[nextState]!![(i + 1) % k].add(i + 1)
                }
            }
        }
    }

    var minimalAnswer = Int.MAX_VALUE
    for (state in machine.finalStates) {
        dp[state]!![l].min()?.let {
            minimalAnswer = min(minimalAnswer, it)
        }
    }

    return minimalAnswer
}

fun fillDpCell(
    dp: HashMap<FiniteStateMachine.State, ArrayList<TreeSet<Int>>>,
    state: FiniteStateMachine.State,
    k: Int
) {
    dp[state] = ArrayList(k)
    for (i in 1..k) {
        dp[state]!!.add(TreeSet())
    }
}

fun main(args: Array<String>) {
    val regex: String
    val k: Int
    val l: Int

    if (args.isEmpty()) {
        println("Put regex on the first line, then k and l on the second line:")
        val scanner = Scanner(System.`in`)
        regex = scanner.nextLine()
        k = scanner.nextInt()
        l = scanner.nextInt()
    } else {
        if (args.size < 3) {
            println("Too few arguments, expected 3 but got ${args.size}")
            return
        }
        regex = args[0]
        k = args[1].toInt()
        l = args[2].toInt()
    }

    val result = solveProblem(regex, k, l)
    println("The answer is ${if (result != Int.MAX_VALUE) result.toString() else "INF"}")
}