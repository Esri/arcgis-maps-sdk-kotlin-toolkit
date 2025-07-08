/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DateTimeFieldTests : FeatureFormTestRunner(
    uri = "https://www.arcgis.com/home/item.html?id=ec09090060664cbda8d814e017337837",
    objectId = 1
) {
    private lateinit var context: Context

    @get:Rule
    val composeTestRule = createComposeRule()


    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Before
    fun setContent() {
        composeTestRule.setContent {
            FeatureForm(
                featureForm = featureForm
            )
        }
    }

    @After
    fun closeDialog() {
        try {
            // this is needed for running successive tests that open the date time picker dialog.
            val dialogSurface =
                composeTestRule.onNodeWithContentDescription(
                    "DateTimePickerDialogSurface",
                    useUnmergedTree = true
                )
            dialogSurface.assertIsDisplayed()
            val cancel = dialogSurface.onChildWithContentDescription("cancel")
            cancel.performClick()
        } catch (t: Throwable) {
            // dialog wasn't open.
        }
    }

    /**
     * Given a required datetime field with a value
     * when it is not focused
     * the text values are as expected
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-22-focused-and-unfocused-state-with-value-populated
     * (the test is a hybrid of 2 tests because the field has a value and the table cannot be edited)
     */
    @Test
    fun testRequiredUnfocusedValue() {
        val formElement = featureForm.getFieldFormElementWithLabel("Required Date")
        assertThat(formElement).isNotNull()
        // find the field with the the label
        val col = composeTestRule.onNodeWithContentDescription("lazy column")
        col.performScrollToIndex(8)
        val dateTimeField = composeTestRule.onNodeWithText("${formElement!!.label} *")

        val textMatcher = hasText("No value")
        assert(textMatcher.matches(dateTimeField.fetchSemanticsNode())) {
            "expected text: No value."
        }
        dateTimeField.assertIsDisplayed()
        val helper = dateTimeField.onChildWithContentDescription("supporting text")
        val helperMatcher = hasText("Date Entry is Required")
        assert(helperMatcher.matches(helper.fetchSemanticsNode())) {
            "expected helper text: Date Entry is Required"

        }
        val iconMatcher = hasContentDescription("date time picker button")
        assert(iconMatcher.matches(dateTimeField.fetchSemanticsNode()))
    }

    /**
     * Given a required datetime field with a value
     * when it is not focused
     * the text values are as expected
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-21-unfocused-and-focused-state-no-value-date-required
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-22-focused-and-unfocused-state-with-value-populated
     * (the test is a hybrid of 2 tests because the field has a value and the table cannot be edited)
     */
    @Test
    fun testRequiredFocusedValue() {
        val formElement = featureForm.getFieldFormElementWithLabel("Required Date")
        assertThat(formElement).isNotNull()
        // find the field with the the label
        val col = composeTestRule.onNodeWithContentDescription("lazy column")
        col.performScrollToIndex(8)
        val dateTimeField = composeTestRule.onNodeWithText("${formElement!!.label} *")
        val iconMatcher = hasContentDescription("date time picker button")
        assert(iconMatcher.matches(dateTimeField.fetchSemanticsNode()))
        dateTimeField.assertHasClickAction()
        dateTimeField.performClick()

        val dialogSurface =
            composeTestRule.onNodeWithContentDescription(
                "DateTimePickerDialogSurface",
                useUnmergedTree = true
            )
        dialogSurface.assertIsDisplayed()
        val today = dialogSurface.onChildWithContentDescription("current date or time button")
        today.assertIsDisplayed()
        val helperTextInDialog = dialogSurface.onChildWithText("Date Entry is Required", true)
        helperTextInDialog.assertIsDisplayed()
    }

    /**
     * Given a FieldFormElement with an editable datetime input
     * When the date value is displayed
     * Then it has a clear button, and tapping it clears the field
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-26-clear-date
     */
    @Test
    fun testClearEditableDateTime() = runTest {
        val formElement = featureForm.getFieldFormElementWithLabel("Launch Date and Time for Apollo 11")
        assertThat(formElement).isNotNull()
        val dateTimeField = composeTestRule.onNodeWithText(formElement!!.label)
        dateTimeField.assertIsDisplayed()

        // assert the text is non empty before asserting the clear button.
        assert(
            try {
                dateTimeField.assertEditableTextEquals("No value")
                false
            } catch (t: Throwable) {
                true
            }
        )
        val clearButton = dateTimeField.onChildWithContentDescription("Clear text button", true)
        clearButton.assertIsDisplayed()
        clearButton.performClick()

        // now assert the field has the placeholder text because it is empty.
        dateTimeField.assertEditableTextEquals("No value")
    }

    /**
     * Given a FieldFormElement with a date time input, and dateOnly set to true
     * When the date value is displayed
     * Then it has no time component
     * https://devtopia.esri.com/runtime/common-toolkit/blob/main/designs/Forms/FormsTestDesign.md#test-case-23-date-only-no-time
     */
    @Test
    fun testDateOnly() = runTest {
        val formElement = featureForm.getFieldFormElementWithLabel("Launch Date for Apollo 11")
        assertThat(formElement).isNotNull()
        // find the field with the the label
        val dateTimeField = composeTestRule.onNodeWithText(formElement!!.label)
        dateTimeField.assertIsDisplayed()

        val helper = dateTimeField.onChildWithContentDescription("supporting text")
        val helperMatcher = hasText("Enter the Date for the Apollo 11 launch")
        assert(helperMatcher.matches(helper.fetchSemanticsNode())) {
            "expected helper text: Enter the Date for the Apollo 11 launch"
        }
        val clearButton = dateTimeField.onChildWithContentDescription("Clear text button", true)
        clearButton.assertIsDisplayed()

        dateTimeField.performClick()

        val dialogSurface =
            composeTestRule.onNodeWithContentDescription(
                "DateTimePickerDialogSurface",
                useUnmergedTree = true
            )
        dialogSurface.assertIsDisplayed()
        val today = dialogSurface.onChildWithContentDescription("current date or time button")
        today.assertIsDisplayed()
        val helperTextInDialog =
            dialogSurface.onChildWithText("Enter the Date for the Apollo 11 launch", true)
        helperTextInDialog.assertIsDisplayed()

        assert(
            try {
                dialogSurface.onChildWithContentDescription("toggle date and time")
                false
            } catch (e: Throwable) {
                true
            }
        )
    }
}
