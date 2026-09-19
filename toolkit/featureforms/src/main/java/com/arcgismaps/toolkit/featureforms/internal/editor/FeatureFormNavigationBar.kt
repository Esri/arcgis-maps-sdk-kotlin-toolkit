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

import android.util.Log
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormManagerState
import com.arcgismaps.toolkit.featureforms.id

@Composable
public fun FeatureFormNavigationBar(
    state: FeatureFormManagerState,
    modifier: Modifier = Modifier
) {
    val activeForm = state.activeFeatureForm
    val forms = remember(activeForm) {
        // Read the forms from the state only when the active form changes to avoid unnecessary
        // recompositions
        state.featureForms.value
    }
    AnimatedVisibility(state.showNavigationBar) {
        FeatureFormNavigationBar(
            activeForm = state.activeFeatureForm,
            featureForms = forms,
            onFeatureFormSelected = state::navigateToForm,
            onExpand = state::showOverview,
            modifier = modifier
        )
    }
}

@Composable
private fun FeatureFormNavigationBar(
    activeForm: FeatureForm,
    featureForms: List<FeatureForm>,
    onFeatureFormSelected: (FeatureForm) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.e("TAG", "FeatureFormNavigationBar: ${activeForm.id()} - ${activeForm}")
    val (activeIndex, count) = remember(activeForm) {
        Pair(
            featureForms.indexOfFirst {
                it.feature == activeForm.feature
            },
            featureForms.size
        )
    }
    val previousForm = featureForms.getOrNull((activeIndex - 1).mod(count))
    val nextForm = featureForms.getOrNull((activeIndex + 1).mod(count))
    //Log.e("TAG", "FeatureFormNavigationBar: prev:$previousForm, next$nextForm", )

    FeatureFormNavigationBarContent(
        activeIndex = activeIndex,
        formCount = featureForms.size,
        onPrevious = { previousForm?.let(onFeatureFormSelected) },
        onNext = { nextForm?.let(onFeatureFormSelected) },
        onExpand = onExpand,
        modifier = modifier
    )
}

@Composable
private fun FeatureFormNavigationBarContent(
    activeIndex: Int,
    formCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable {
            onExpand()
        },
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(
            topStart = 25.dp,
            topEnd = 25.dp
        ),
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
private fun FeatureFormNavigationBarPreview() {
    FeatureFormNavigationBarContent(
        activeIndex = 1,
        formCount = 3,
        onPrevious = {},
        onNext = {},
        onExpand = {}
    )
}
