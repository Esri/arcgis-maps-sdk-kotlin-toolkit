/*
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
 */

package com.arcgismaps.toolkit.popup

import android.util.Log
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultDescriptor
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement

/**
 * Runs the default Accessibility Test Framework checks against the rendered Compose UI.
 * Set [failOnViolation] to `false` to log violations without failing the test.
 * See `A11Y` tags in test result logs.
 */
internal fun ComposeContentTestRule.checkAccessibility(
    failOnViolation: Boolean = true
) {
    val resultDescriptor = AccessibilityCheckResultDescriptor()
    val validator = AccessibilityValidator()
        .setCheckPreset(AccessibilityCheckPreset.PRERELEASE)
        .setRunChecksFromRootView(true)
        .setThrowExceptionFor(if (failOnViolation) AccessibilityCheckResultType.WARNING else null)
        .addCheckListener { _, results ->
            val violations = results.filter { result ->
                result.type == AccessibilityCheckResultType.ERROR || result.type == AccessibilityCheckResultType.WARNING
            }
            Log.w("A11Y", "${violations.size} accessibility violation(s)")
            violations.forEach { result ->
                Log.w("A11Y", "[${result.type}] ${resultDescriptor.describeResult(result)}")
                val context = result.element?.let { element -> elementContext(result, element) }
                    ?: "[${result.sourceCheckClass.simpleName}] no view hierarchy context was provided"
                Log.w("A11Y", context)
            }
        }

    enableAccessibilityChecks(accessibilityValidator = validator)
    try {
        val roots = onAllNodes(isRoot())
        repeat(roots.fetchSemanticsNodes().size) { index ->
            roots[index].tryPerformAccessibilityChecks()
        }
    } finally {
        disableAccessibilityChecks()
    }
}

/**
 * @return [String] context of the View element with accessibility violation.
 */
private fun elementContext(
    result: AccessibilityViewCheckResult,
    element: ViewHierarchyElement
): String {
    val ancestry = buildList {
        var current: ViewHierarchyElement? = element
        while (current != null) {
            add(current.className.toString())
            current = current.parentView
        }
    }.joinToString(" <- ")
    return StringBuilder()
        .append('[').append(result.sourceCheckClass.simpleName).append("] ")
        .append("origin=").append(element.origin)
        .append(", package=").append(element.packageName)
        .append(", class=").append(element.className)
        .append(", a11yClass=").append(element.accessibilityClassName)
        .append(", resource=").append(element.resourceName)
        .append(", testTag=").append(element.testTag)
        .append(", bounds=").append(element.boundsInScreen)
        .append(", text=").append(element.text)
        .append(", description=").append(element.contentDescription)
        .append(", state=").append(element.stateDescription)
        .append(", visible=").append(element.isVisibleToUser)
        .append(", clickable=").append(element.isClickable)
        .append(", scrollable=").append(element.isScrollable)
        .append(", ancestry=").append(ancestry)
        .toString()
}
