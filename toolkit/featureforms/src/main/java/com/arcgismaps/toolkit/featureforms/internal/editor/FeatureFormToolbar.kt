/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.toolkit.featureforms.FeatureFormManagerState

public object FeatureFormToolbarDefaults {

    /**
     * The default shape for the [FeatureFormToolbar].
     */
    public val shape: Shape = RoundedCornerShape(
        topStart = 25.dp,
        topEnd = 25.dp
    )

    @Composable
    public fun colors(): FeatureFormToolbarColors = FeatureFormToolbarColors(
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    @Composable
    public fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): FeatureFormToolbarColors = FeatureFormToolbarColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )
}

@Immutable
public data class FeatureFormToolbarColors(
    public val backgroundColor: Color,
    public val contentColor: Color
)

@Composable
public fun FeatureFormToolbar(
    state: FeatureFormManagerState,
    modifier: Modifier = Modifier,
    shape: Shape = FeatureFormToolbarDefaults.shape,
    colors: FeatureFormToolbarColors = FeatureFormToolbarDefaults.colors()
) {
    val activeForm = state.activeFeatureForm
    val forms = remember(activeForm) {
        // Read the forms from the state only when the active form changes to avoid unnecessary
        // recompositions
        state.featureForms.value
    }
    val (activeIndex, count) = remember(activeForm) {
        Pair(
            forms.indexOfFirst {
                it.feature == activeForm.feature
            },
            forms.size
        )
    }
    val previousForm = remember(forms) {
        forms.getOrNull((activeIndex - 1).mod(count))
    }
    val nextForm = remember(forms) {
        forms.getOrNull((activeIndex + 1).mod(count))
    }
    AnimatedVisibility(state.showNavigationBar) {
        FeatureFormToolbarContent(
            activeIndex = activeIndex,
            formCount = forms.size,
            onPrevious = { previousForm?.let(state::navigateToForm) },
            onNext = { nextForm?.let(state::navigateToForm) },
            onExpand = state::showOverview,
            shape = shape,
            colors = colors,
            modifier = modifier
        )
    }
    Card(
        colors = CardDefaults.cardColors().copy(

        )
    ) { }
}

@Composable
private fun FeatureFormToolbarContent(
    activeIndex: Int,
    formCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    shape: Shape,
    colors: FeatureFormToolbarColors,
    modifier: Modifier = Modifier
) {
    // HorizontalFloatingToolbar()
    Surface(
        modifier = modifier.clickable {
            onExpand()
        },
        color = colors.backgroundColor,
        contentColor = colors.contentColor,
        shape = shape,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics {
                    stateDescription = "${activeIndex + 1} of $formCount"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = formCount > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.30f
                        )
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Previous form",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Feature ${activeIndex + 1} of $formCount",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onNext,
                    enabled = formCount > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.30f
                        )
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Next form",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Returns the global ID of the [ArcGISFeature].
 */
internal val ArcGISFeature.objectId: String
    get() = (attributes["OBJECTID"] as? Long?).toString()

@Preview
@Composable
private fun FeatureFormToolbarPreview() {
    FeatureFormToolbarContent(
        activeIndex = 1,
        formCount = 3,
        onPrevious = {},
        onNext = {},
        onExpand = {},
        shape = FeatureFormToolbarDefaults.shape,
        colors = FeatureFormToolbarDefaults.colors(),
    )
}
