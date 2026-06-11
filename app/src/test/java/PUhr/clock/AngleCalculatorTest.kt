package PUhr.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AngleCalculatorTest(
    private val value: Float,
    private val totalUnits: Int,
    private val expectedDegrees: Float,
) {
    @Test
    fun toHandDegrees_returnsCorrectAngle() {
        assertEquals(expectedDegrees, value.toHandDegrees(totalUnits), 0.001f)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}/{1} → {2}°")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(3f, 12, 90f),
            arrayOf(0f, 60, 0f),
            arrayOf(0f, 60, 0f),
            arrayOf(6f, 12, 180f),
            arrayOf(15f, 60, 90f),
            arrayOf(30f, 60, 180f),
            arrayOf(45f, 60, 270f),
        )
    }
}
