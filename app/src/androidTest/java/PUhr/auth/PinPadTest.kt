package PUhr.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PinPadTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var digitCallback: Char? = null
    private var backspaceCalled = false
    private var submitCalled = false

    @Test
    fun digitButtons_areDisplayed() {
        composeTestRule.setContent {
            PinPad(
                dotsCount = 0,
                shake = false,
                enabled = true,
                onDigit = { digitCallback = it },
                onBackspace = { backspaceCalled = true },
                onSubmit = { submitCalled = true },
            )
        }

        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("2").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("4").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("6").assertExists()
        composeTestRule.onNodeWithText("7").assertExists()
        composeTestRule.onNodeWithText("8").assertExists()
        composeTestRule.onNodeWithText("9").assertExists()
        composeTestRule.onNodeWithText("0").assertExists()
    }

    @Test
    fun clickingDigit_callsOnDigit() {
        composeTestRule.setContent {
            PinPad(
                dotsCount = 0,
                shake = false,
                enabled = true,
                onDigit = { digitCallback = it },
                onBackspace = { backspaceCalled = true },
                onSubmit = { submitCalled = true },
            )
        }

        composeTestRule.onNodeWithText("5").performClick()
        assert(digitCallback == '5')
    }
}
