package PUhr.auth

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PinPadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var digitCallback: Char? = null
    private var backspaceCalled = false
    private var submitCalled = false

    private fun setContent(
        dotsCount: Int = 0,
        shake: Boolean = false,
        enabled: Boolean = true,
    ) {
        digitCallback = null
        backspaceCalled = false
        submitCalled = false
        composeTestRule.setContent {
            PinPad(
                dotsCount = dotsCount,
                shake = shake,
                enabled = enabled,
                onDigit = { digitCallback = it },
                onBackspace = { backspaceCalled = true },
                onSubmit = { submitCalled = true },
            )
        }
    }

    @Test
    fun digitButtons_areDisplayed() {
        setContent()
        for (d in 0..9) {
            composeTestRule.onNodeWithText(d.toString()).assertExists()
        }
    }

    @Test
    fun clickingDigit_callsOnDigit() {
        setContent()
        composeTestRule.onNodeWithText("5").performClick()
        assertEquals('5', digitCallback)
    }

    @Test
    fun dotsRow_showsCorrectDotsCount() {
        setContent(dotsCount = 4)
        composeTestRule.onNodeWithTag("DotsRow:4").assertExists()
    }

    @Test
    fun dotsRow_showsZeroDotsInitially() {
        setContent(dotsCount = 0)
        composeTestRule.onNodeWithTag("DotsRow:0").assertExists()
    }

    @Test
    fun clickingBackspace_callsOnBackspace() {
        setContent(dotsCount = 3)
        composeTestRule.onNodeWithTag("Backspace").performClick()
        assert(backspaceCalled)
    }

    @Test
    fun clickingSubmit_callsOnSubmit() {
        setContent(dotsCount = 4)
        composeTestRule.onNodeWithTag("Submit").performClick()
        assert(submitCalled)
    }

    @Test
    fun submitDisabled_whenDotsCountIsZero() {
        setContent(dotsCount = 0)
        composeTestRule.onNodeWithTag("Submit").assertIsNotEnabled()
    }

    @Test
    fun submitEnabled_whenDotsCountIsPositive() {
        setContent(dotsCount = 4)
        composeTestRule.onNodeWithTag("Submit").assertIsEnabled()
    }

    @Test
    fun allKeysDisabled_whenNotEnabled() {
        setContent(enabled = false)
        composeTestRule.onNodeWithText("1").assertHasNoClickAction()
        composeTestRule.onNodeWithTag("Backspace").assertHasNoClickAction()
    }
}
