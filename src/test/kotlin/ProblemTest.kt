import org.junit.jupiter.api.Test


class ProblemTest {
    @Test
    fun testProblem() {
        assert(solveProblem("acb..bab.c. ∗ .ab.ba. + . + ∗a.", 3, 0) == Int.MAX_VALUE)
        assert(solveProblem("ab + c.aba. ∗ .bac. + . + ∗", 3, 1) == 4)
        assert(solveProblem("aab + *.b.", 5, 0) == 5)
        assert(solveProblem("aab.c.d.*.b.", 2, 1) == Int.MAX_VALUE)
        assert(solveProblem("aab.c.d.*.b.", 5, 1) == 6)
        assert(solveProblem("aab.c.d.*.b.", 39, 3) == 42)
    }

}