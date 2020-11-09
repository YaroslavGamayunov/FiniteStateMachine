package cli

import solveProblem
import java.util.*

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