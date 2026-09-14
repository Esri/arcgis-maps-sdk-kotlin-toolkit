/*
 *
 *  Copyright 2026 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.basemapgallery

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class BasemapGalleryAccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testHarness = BasemapGalleryTestHarness(composeTestRule)

    /**
     * Given a populated [BasemapGallery]
     * Then the rendered component should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testBasemapGalleryAccessibility() {
        testHarness.render()

        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
     * Given a selected gallery item in a light theme
     * Then the selected gallery should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSelectedBasemapGalleryAccessibilityInLightTheme() {
        testHarness.render(scenario = BasemapGalleryTestScenarios.Device)
        testHarness.item(BasemapGalleryTestData.ImageryTitle).performClick()

        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
     * Given a selected gallery item in a dark theme
     * Then the selected gallery should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testSelectedBasemapGalleryAccessibilityInDarkTheme() {
        testHarness.render(scenario = BasemapGalleryTestScenarios.DarkDevice)
        testHarness.item(BasemapGalleryTestData.ImageryTitle).performClick()

        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
    * Given a gallery at 200% font scale
     * Then the rendered component should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testBasemapGalleryAccessibilityAtTwoHundredPercentFontScale() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.LargeText
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
    * Given a gallery in a landscape window
    * Then long content and every interactive item should remain available
     */
    @Test
    fun testLandscapeGalleryReflows() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.Landscape
        )

        testHarness.assertMinimumInteractiveSize(
            items = BasemapGalleryTestData.items,
            minimumSize = MINIMUM_INTERACTIVE_SIZE
        )
        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
    }

    /**
    * Given an expanded gallery
    * Then every interactive item should retain its minimum size
     */
    @Test
    fun testExpandedGalleryMaintainsMinimumInteractiveSize() {
        testHarness.render(
            items = BasemapGalleryTestData.items,
            scenario = BasemapGalleryTestScenarios.Expanded
        )

        testHarness.assertMinimumInteractiveSize(
            items = BasemapGalleryTestData.items,
            minimumSize = MINIMUM_INTERACTIVE_SIZE
        )
    }

    /**
     * Given a right-to-left gallery
     * Then the rendered component should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testRightToLeftBasemapGalleryAccessibility() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.RightToLeftDevice
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
    * Given a gallery with large bold text
     * Then the rendered component should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testLargeBoldTextBasemapGalleryAccessibility() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.largeBoldText()
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
        testHarness.assertAutomatedAccessibilityChecks()
    }

    /**
     * Given a gallery item with a visible title
     * Then it should expose one useful clickable node named by that title
     */
    @Test
    fun testGalleryItemsHaveOneAccessibleName() {
        testHarness.render()

        BasemapGalleryTestData.items.forEach { galleryItem ->
            composeTestRule
                .onAllNodesWithText(galleryItem.title)
                .assertCountEquals(1)

            val item = testHarness.item(galleryItem.title)
                .assertTextContains(galleryItem.title)
            val contentDescriptions = item.fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
            assertThat(contentDescriptions).isNull()
        }
    }

    /**
     * Given a populated gallery
     * Then the gallery should expose one selectable group of radio-button items
     */
    @Test
    fun testGalleryExposesSingleSelectionSemantics() {
        testHarness.render()

        testHarness.gallery().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.SelectableGroup, Unit)
        )
        composeTestRule
            .onAllNodes(isSelectable())
            .assertCountEquals(BasemapGalleryTestData.items.size)
            .assertAll(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    /**
     * Given a gallery item before and after activation
     * Then it should expose selectable and selected semantics
     */
    @Test
    fun testGalleryItemExposesSelectableState() {
        testHarness.render()

        testHarness.item(BasemapGalleryTestData.ImageryTitle)
            .assertIsSelectable()
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
    }

    /**
     * Given a three-dimensional basemap
     * Then its item should expose both its title and the 3D designation
     */
    @Test
    fun testThreeDimensionalItemMeaningIsExposed() {
        testHarness.render()

        testHarness.item(BasemapGalleryTestData.ImageryTitle)
            .assertTextContains(BasemapGalleryTestData.ImageryTitle)
            .assertTextContains(BasemapGalleryTestData.ThreeDimensionalLabel)
    }

    /**
     * Given an unselected gallery item
     * When it is activated
     * Then it should expose its selected state and invoke the callback with that item
     */
    @Test
    fun testGalleryItemSelectionIsExposed() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(onItemClick = { selectedItem = it })

        testHarness.item(BasemapGalleryTestData.ImageryTitle).performClick()

        composeTestRule.runOnIdle {
            assertThat(selectedItem).isSameInstanceAs(BasemapGalleryTestData.items[1])
        }
        testHarness.item(BasemapGalleryTestData.ImageryTitle).assertIsSelected()
    }

    /**
     * Given a selected gallery item
     * When another item is selected
     * Then exactly one item should expose the selected state
     */
    @Test
    fun testSelectingAnotherItemReplacesSelection() {
        testHarness.render()
        val topographicItem = testHarness.item(BasemapGalleryTestData.TopographicTitle)
        val imageryItem = testHarness.item(BasemapGalleryTestData.ImageryTitle)

        topographicItem.performClick().assertIsSelected()
        imageryItem.assertIsNotSelected().performClick().assertIsSelected()
        topographicItem.assertIsNotSelected()
        composeTestRule.onAllNodes(isSelected()).assertCountEquals(1)
    }

    /**
     * Given a selected gallery item
     * When saved state is restored
     * Then the item should remain selected
     */
    @Test
    fun testSelectionSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            BasemapGalleryTestContent()
        }
        testHarness.item(BasemapGalleryTestData.ImageryTitle).performClick().assertIsSelected()

        restorationTester.emulateSavedInstanceStateRestore()

        testHarness.item(BasemapGalleryTestData.ImageryTitle).assertIsSelected()
    }

    /**
     * Given a gallery item
     * Then its interactive layout should meet the Material 48dp baseline
     */
    @Test
    fun testGalleryItemsMeetMinimumInteractiveSize() {
        testHarness.render()

        testHarness.assertMinimumInteractiveSize(
            items = BasemapGalleryTestData.items,
            minimumSize = MINIMUM_INTERACTIVE_SIZE
        )
    }

    /**
     * Given adjacent gallery items
     * Then their interactive layouts should not overlap
     */
    @Test
    fun testAdjacentGalleryItemsDoNotOverlap() {
        testHarness.render()

        val firstBounds = testHarness.item(BasemapGalleryTestData.TopographicTitle).getUnclippedBoundsInRoot()
        val secondBounds = testHarness.item(BasemapGalleryTestData.ImageryTitle).getUnclippedBoundsInRoot()
        val doNotOverlap = firstBounds.right <= secondBounds.left ||
            secondBounds.right <= firstBounds.left ||
            firstBounds.bottom <= secondBounds.top ||
            secondBounds.bottom <= firstBounds.top
        assertThat(doNotOverlap).isTrue()
    }

    /**
     * Given a pointer press on a gallery item
     * When the pointer is released
     * Then activation should occur only on release
     */
    @Test
    fun testGalleryItemActivatesOnPointerRelease() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(onItemClick = { selectedItem = it })
        val item = testHarness.item(BasemapGalleryTestData.TopographicTitle)

        item.performTouchInput { down(center) }
        composeTestRule.runOnIdle {
            assertThat(selectedItem).isNull()
        }
        item.performTouchInput { up() }
        composeTestRule.runOnIdle {
            assertThat(selectedItem).isSameInstanceAs(BasemapGalleryTestData.items[0])
        }
    }

    /**
     * Given a pointer press on a gallery item
     * When the pointer input is cancelled
     * Then the item should not be activated
     */
    @Test
    fun testGalleryItemActivationCanBeCancelled() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(onItemClick = { selectedItem = it })
        val item = testHarness.item(BasemapGalleryTestData.TopographicTitle)

        item.performTouchInput { down(center) }
        item.performTouchInput { cancel() }

        composeTestRule.runOnIdle {
            assertThat(selectedItem).isNull()
        }
    }

    /**
     * Given a compact gallery at 200% font scale
     * Then long content should remain reachable without root clipping
     */
    @Test
    fun testCompactGalleryReflowsAtTwoHundredPercentFontScale() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.CompactLargeText
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
    }

    /**
     * Given increased font scale and font weight
     * Then long content should remain reachable without root clipping
     */
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testGalleryReflowsWithLargeBoldText() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.compactLargeBoldText()
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
    }

    /**
     * Given a right-to-left gallery layout
     * Then long content should remain reachable without root clipping
     */
    @Test
    fun testGalleryReflowsInRightToLeftLayout() {
        testHarness.render(
            items = BasemapGalleryTestData.scrollingItems,
            scenario = BasemapGalleryTestScenarios.RightToLeft
        )

        testHarness.assertTitleIsReachableWithoutVisualOverflow(BasemapGalleryTestData.LongTitle)
    }

    /**
     * Given no gallery items
     * Then the gallery should render without exposing stale item actions
     */
    @Test
    fun testEmptyGallery() {
        testHarness.render(items = emptyList())

        testHarness.gallery().assertExists()
        composeTestRule
            .onAllNodesWithText(BasemapGalleryTestData.TopographicTitle)
            .assertCountEquals(0)
    }

    /**
     * Given an empty gallery
     * Then the rendered component should pass automated accessibility checks
     */
    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testEmptyBasemapGalleryAccessibility() {
        testHarness.render(items = emptyList())

        testHarness.assertAutomatedAccessibilityChecks()
    }

    private companion object {
        val MINIMUM_INTERACTIVE_SIZE = 48.dp
    }
}

class BasemapGalleryKeyboardAccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testHarness = BasemapGalleryTestHarness(composeTestRule)

    /**
     * Given keyboard input
     * When focus traverses and activates gallery items
     * Then focus order and selection should remain operable
     */
    @Test
    fun testKeyboardTraversalAndEnterActivation() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(
            inputMode = InputMode.Keyboard,
            onItemClick = { selectedItem = it }
        )
        val firstItem = testHarness.item(BasemapGalleryTestData.TopographicTitle)
        val secondItem = testHarness.item(BasemapGalleryTestData.ImageryTitle)

        firstItem.requestFocus().assertIsFocused()
        firstItem.performKeyInput { pressKey(Key.DirectionRight) }
        secondItem.assertIsFocused()
        secondItem.performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertThat(selectedItem).isSameInstanceAs(BasemapGalleryTestData.items[1])
        }
        secondItem.assertIsFocused().assertIsSelected()
    }

    /**
     * Given a focused gallery item
     * When Space is pressed
     * Then the item should be activated
     */
    @Test
    fun testKeyboardSpaceActivation() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(
            inputMode = InputMode.Keyboard,
            onItemClick = { selectedItem = it }
        )
        val firstItem = testHarness.item(BasemapGalleryTestData.TopographicTitle)

        firstItem.requestFocus().assertIsFocused()
        firstItem.performKeyInput { pressKey(Key.Spacebar) }

        composeTestRule.runOnIdle {
            assertThat(selectedItem).isSameInstanceAs(BasemapGalleryTestData.items[0])
        }
        firstItem.assertIsFocused().assertIsSelected()
    }

    /**
     * Given right-to-left keyboard input
     * When focus moves in the visual direction
     * Then the next gallery item should receive focus and activate
     */
    @Test
    fun testRightToLeftKeyboardTraversalAndActivation() {
        var selectedItem: BasemapGalleryItem? = null
        testHarness.render(
            scenario = BasemapGalleryTestScenarios.RightToLeft,
            inputMode = InputMode.Keyboard,
            onItemClick = { selectedItem = it }
        )

        val firstItem = testHarness.item(BasemapGalleryTestData.TopographicTitle)
        val secondItem = testHarness.item(BasemapGalleryTestData.ImageryTitle)
        firstItem.requestFocus().assertIsFocused()
        firstItem.performKeyInput { pressKey(Key.DirectionLeft) }
        secondItem.assertIsFocused().performKeyInput { pressKey(Key.Enter) }

        composeTestRule.runOnIdle {
            assertThat(selectedItem).isSameInstanceAs(BasemapGalleryTestData.items[1])
        }
        secondItem.assertIsSelected()
    }
}