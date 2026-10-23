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

package com.esri.microappslib.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.fail
import org.junit.Test
import java.util.Locale
import kotlin.math.pow

@Suppress("DEPRECATION")
class ColorSchemeContrastTest {

    @Test
    fun lightColorSchemeMeetsContrastRequirements() {
        assertColorSchemeContrast(LightColorScheme, "Light")
    }

    @Test
    fun darkColorSchemeMeetsContrastRequirements() {
        assertColorSchemeContrast(DarkColorScheme, "Dark")
    }

    private fun assertColorSchemeContrast(colorScheme: ColorScheme, name: String) {
        val failures = mutableListOf<String>()
        val surfaceRoles = listOf(
            "background" to colorScheme.background,
            "surface" to colorScheme.surface,
            "surfaceVariant" to colorScheme.surfaceVariant,
            "surfaceDim" to colorScheme.surfaceDim,
            "surfaceBright" to colorScheme.surfaceBright,
            "surfaceContainerLowest" to colorScheme.surfaceContainerLowest,
            "surfaceContainerLow" to colorScheme.surfaceContainerLow,
            "surfaceContainer" to colorScheme.surfaceContainer,
            "surfaceContainerHigh" to colorScheme.surfaceContainerHigh,
            "surfaceContainerHighest" to colorScheme.surfaceContainerHighest
        )
        val semanticTextPairs = listOf(
            "onPrimary / primary" to (colorScheme.onPrimary to colorScheme.primary),
            "onPrimaryContainer / primaryContainer" to
                (colorScheme.onPrimaryContainer to colorScheme.primaryContainer),
            "onSecondary / secondary" to (colorScheme.onSecondary to colorScheme.secondary),
            "onSecondaryContainer / secondaryContainer" to
                (colorScheme.onSecondaryContainer to colorScheme.secondaryContainer),
            "onTertiary / tertiary" to (colorScheme.onTertiary to colorScheme.tertiary),
            "onTertiaryContainer / tertiaryContainer" to
                (colorScheme.onTertiaryContainer to colorScheme.tertiaryContainer),
            "onError / error" to (colorScheme.onError to colorScheme.error),
            "onErrorContainer / errorContainer" to
                (colorScheme.onErrorContainer to colorScheme.errorContainer),
            "onBackground / background" to
                (colorScheme.onBackground to colorScheme.background),
            "onSurface / surface" to (colorScheme.onSurface to colorScheme.surface),
            "onSurfaceVariant / surfaceVariant" to
                (colorScheme.onSurfaceVariant to colorScheme.surfaceVariant),
            "inverseOnSurface / inverseSurface" to
                (colorScheme.inverseOnSurface to colorScheme.inverseSurface),
            "inversePrimary / inverseSurface" to
                (colorScheme.inversePrimary to colorScheme.inverseSurface)
        )
        val textOnSurfaces = surfaceRoles.flatMap { (surfaceName, surface) ->
            listOf(
                "onSurface / $surfaceName" to (colorScheme.onSurface to surface),
                "onSurfaceVariant / $surfaceName" to
                    (colorScheme.onSurfaceVariant to surface),
                "primary / $surfaceName" to (colorScheme.primary to surface),
                "error / $surfaceName" to (colorScheme.error to surface)
            )
        }

        (semanticTextPairs + textOnSurfaces).forEach { (pairName, pair) ->
            contrastFailure(
                schemeName = name,
                pairName = pairName,
                foreground = pair.first,
                background = pair.second,
                backdrop = colorScheme.background,
                requiredRatio = TEXT_CONTRAST_MINIMUM
            )?.let(failures::add)
        }

        surfaceRoles.forEach { (surfaceName, surface) ->
            contrastFailure(
                schemeName = name,
                pairName = "outline / $surfaceName",
                foreground = colorScheme.outline,
                background = surface,
                backdrop = colorScheme.background,
                requiredRatio = NON_TEXT_CONTRAST_MINIMUM
            )?.let(failures::add)
        }

        if (failures.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("$name color scheme contrast failures:")
                    failures.forEach { appendLine("- $it") }
                    append(
                        "These results cover the listed color pairs only; they do not " +
                            "establish full WCAG 2.2 conformance."
                    )
                }
            )
        }
    }

    private fun contrastFailure(
        schemeName: String,
        pairName: String,
        foreground: Color,
        background: Color,
        backdrop: Color,
        requiredRatio: Double
    ): String? {
        val renderedBackground = composite(background, backdrop)
        val renderedForeground = composite(foreground, renderedBackground)
        val ratio = contrastRatio(renderedForeground, renderedBackground)
        if (ratio >= requiredRatio) return null

        val suggestedForeground = listOf(Color.Black, Color.White)
            .maxBy { contrastRatio(it, renderedBackground) }
        val suggestedRatio = contrastRatio(suggestedForeground, renderedBackground)

        return "$schemeName $pairName: ${renderedForeground.toHex()} on " +
            "${renderedBackground.toHex()} is ${ratio.format()}:1; requires " +
            "${requiredRatio.format()}:1. Suggested foreground " +
            "${suggestedForeground.toHex()} gives ${suggestedRatio.format()}:1."
    }

    private fun composite(color: Color, background: Color): Color {
        val foregroundAlpha = color.alpha
        val backgroundAlpha = background.alpha
        val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)
        if (outputAlpha == 0f) return Color.Transparent

        fun channel(foreground: Float, backdrop: Float): Float =
            (foreground * foregroundAlpha +
                backdrop * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha

        return Color(
            red = channel(color.red, background.red),
            green = channel(color.green, background.green),
            blue = channel(color.blue, background.blue),
            alpha = outputAlpha
        )
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (maxOf(firstLuminance, secondLuminance) + 0.05) /
            (minOf(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) {
                value / 12.92
            } else {
                ((value + 0.055) / 1.055).pow(2.4)
            }
        }

        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }

    private fun Color.toHex(): String {
        fun channel(value: Float): String =
            (value * 255).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')

        return "#${channel(red)}${channel(green)}${channel(blue)}".uppercase(Locale.ROOT)
    }

    private fun Double.format(): String = "%.2f".format(Locale.ROOT, this)

    private companion object {
        const val TEXT_CONTRAST_MINIMUM = 4.5
        const val NON_TEXT_CONTRAST_MINIMUM = 3.0
    }
}
