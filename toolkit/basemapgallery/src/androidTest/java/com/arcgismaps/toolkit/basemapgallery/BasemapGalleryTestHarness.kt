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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.FontWeightAdjustment
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.then
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.ui.unit.dp
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator

internal class BasemapGalleryTestHarness(
    private val composeTestRule: ComposeContentTestRule
) {
    private lateinit var inputModeManager: InputModeManager

    fun render(
        items: List<BasemapGalleryItem> = BasemapGalleryTestData.items,
        scenario: BasemapGalleryTestScenario = BasemapGalleryTestScenarios.Device,
        inputMode: InputMode = InputMode.Touch,
        onItemClick: (BasemapGalleryItem) -> Unit = {}
    ) {
        composeTestRule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasemapGalleryTestContent(
                items = items,
                scenario = scenario,
                onItemClick = onItemClick
            )
        }
        composeTestRule.runOnIdle {
            inputModeManager.requestInputMode(inputMode)
        }
    }

    fun item(title: String): SemanticsNodeInteraction = composeTestRule
        .onNodeWithText(title)
        .assertHasClickAction()

    fun gallery(): SemanticsNodeInteraction = composeTestRule.onNodeWithTag(GalleryTag)

    fun assertAutomatedAccessibilityChecks() {
        val accessibilityValidator = AccessibilityValidator()
            .setThrowExceptionFor(AccessibilityCheckResultType.WARNING)
        composeTestRule.enableAccessibilityChecks(accessibilityValidator)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    fun assertMinimumInteractiveSize(items: List<BasemapGalleryItem>, minimumSize: Dp) {
        items.forEach { galleryItem ->
            item(galleryItem.title)
                .assertWidthIsAtLeast(minimumSize)
                .assertHeightIsAtLeast(minimumSize)
        }
    }

    /**
     * Verifies reachability and visual text overflow. Overlap with adjacent content and contrast
     * still require preview and device inspection.
     */
    fun assertTitleIsReachableWithoutVisualOverflow(title: String) {
        gallery().performScrollToNode(hasText(title))
        composeTestRule
            .onNodeWithText(title, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasNoVisualOverflow()
    }

    private fun SemanticsNodeInteraction.assertHasNoVisualOverflow(): SemanticsNodeInteraction = assert(
        SemanticsMatcher("text has no visual overflow") { semanticsNode ->
            val textLayoutResults = mutableListOf<TextLayoutResult>()
            semanticsNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(textLayoutResults)
            textLayoutResults.singleOrNull()?.hasVisualOverflow == false
        }
    )

    private companion object {
        const val GalleryTag = BasemapGalleryTestTag
    }
}

@Composable
internal fun BasemapGalleryTestContent(
    items: List<BasemapGalleryItem> = BasemapGalleryTestData.items,
    scenario: BasemapGalleryTestScenario = BasemapGalleryTestScenarios.Device,
    onItemClick: (BasemapGalleryItem) -> Unit = {}
) {
    DeviceConfigurationOverride(scenario.configurationOverride) {
        val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BasemapGallery(
                    basemapGalleryItems = items,
                    onItemClick = onItemClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(BasemapGalleryTestTag)
                )
            }
        }
    }
}

private const val BasemapGalleryTestTag = "BasemapGallery"

internal class BasemapGalleryTestScenario(
    val configurationOverride: DeviceConfigurationOverride
)

internal object BasemapGalleryTestScenarios {
    private val PortraitSize = DpSize(360.dp, 640.dp)
    private val CompactSize = DpSize(320.dp, 480.dp)
    private val LandscapeSize = DpSize(640.dp, 360.dp)
    private val ExpandedSize = DpSize(840.dp, 900.dp)

    val Device = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.DarkMode(false)
    )
    val DarkDevice = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.DarkMode(true)
    )
    val LargeText = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.FontScale(2f) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    fun largeBoldText(): BasemapGalleryTestScenario = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.FontScale(2f) then
            DeviceConfigurationOverride.FontWeightAdjustment(200) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val RightToLeftDevice = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.LayoutDirection(ComposeLayoutDirection.Rtl) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val Portrait = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(PortraitSize) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val DarkPortrait = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(PortraitSize) then
            DeviceConfigurationOverride.DarkMode(true)
    )
    val CompactLargeText = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(CompactSize) then
            DeviceConfigurationOverride.FontScale(2f) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val Landscape = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(LandscapeSize) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val Expanded = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(ExpandedSize) then
            DeviceConfigurationOverride.DarkMode(false)
    )
    val RightToLeft = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(CompactSize) then
            DeviceConfigurationOverride.LayoutDirection(ComposeLayoutDirection.Rtl) then
            DeviceConfigurationOverride.DarkMode(false)
    )

    fun compactLargeBoldText(): BasemapGalleryTestScenario = BasemapGalleryTestScenario(
        DeviceConfigurationOverride.WindowSize(CompactSize) then
            DeviceConfigurationOverride.FontScale(2f) then
            DeviceConfigurationOverride.FontWeightAdjustment(200) then
            DeviceConfigurationOverride.DarkMode(false)
    )
}

internal object BasemapGalleryTestData {
    const val TopographicTitle = "Topographic"
    const val ImageryTitle = "Imagery"
    const val NavigationTitle = "Navigation map"
    const val ThreeDimensionalLabel = "3D"
    const val LongTitle =
        "Community navigation and accessible transportation basemap with detailed landmarks"

    val items = listOf(
        BasemapGalleryItem(title = TopographicTitle, tag = "topographic"),
        BasemapGalleryItem(title = ImageryTitle, tag = "imagery", is3D = true),
        BasemapGalleryItem(title = NavigationTitle, tag = "navigation")
    )

    val scrollingItems = items +
        (1..8).map { index ->
            BasemapGalleryItem(title = "Reference basemap $index", tag = index)
        } +
        BasemapGalleryItem(title = LongTitle, tag = "long-title")
}
