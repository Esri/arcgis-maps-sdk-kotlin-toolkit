/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.FeatureTemplate
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeatureSheet(
    onDismissRequest: () -> Unit,
    onSelected: (FeatureTemplate?, FeatureLayer, Point) -> Unit,
    uiState: UIState.AddFeature,
    paddingValues: PaddingValues = PaddingValues(0.dp),
) {
    val windowSize = getWindowSize(LocalContext.current)
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = {
            it != SheetValue.Hidden
        },
        skipHiddenState = false
    )
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val reticle = remember {
        with(density) {
            Offset(
                configuration.screenWidthDp.dp.toPx() / 2,
                configuration.screenHeightDp.dp.toPx() / 3
            )
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val circle = Path().apply {
            addOval(Rect(reticle, 200f))
        }
        clipPath(circle, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.5f))
        }
        // outer cross-hair
        drawCrossHair(
            center = reticle,
            color = Color.Black,
            size = 9.dp.toPx(),
            strokeWidth = 5.dp.toPx()
        )
        // inner cross-hair
        drawCrossHair(
            center = reticle,
            color = Color.White,
            size = 8.dp.toPx(),
            strokeWidth = 2.dp.toPx()
        )
    }
    SheetLayout(
        windowSizeClass = windowSize,
        sheetOffsetY = { bottomSheetState.requireOffset() },
        maxWidth = BottomSheetMaxWidth,
        modifier = Modifier.padding(paddingValues)
    ) { layoutWidth, layoutHeight ->
        StandardBottomSheet(
            state = bottomSheetState,
            peekHeight = 40.dp,
            expansionHeight = SheetExpansionHeight(0.3f),
            sheetSwipeEnabled = true,
            shape = RoundedCornerShape(5.dp),
            layoutHeight = layoutHeight.toFloat(),
            sheetWidth = with(LocalDensity.current) { layoutWidth.toDp() }
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.select_a_feature_template),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Add Feature"
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
            ) {
                uiState.layerTemplates.forEach {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = it.layer.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (it.defaultFeatureRow != null) {
                        item {
                            DefaultFeatureRow(
                                onClick = {
                                    onSelected(
                                        null,
                                        it.layer,
                                        Point(reticle.x.toDouble(), reticle.y.toDouble())
                                    )
                                }, defaultFeatureRow = it.defaultFeatureRow
                            )
                        }
                    }
                    items(it.templates) { row ->
                        FeatureTemplateRow(
                            onClick = {
                                onSelected(
                                    row.template,
                                    it.layer,
                                    Point(reticle.x.toDouble(), reticle.y.toDouble())
                                )
                            },
                            template = row
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureTemplateRow(
    onClick: () -> Unit,
    template: TemplateRow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        template.bitmap?.asImageBitmap()?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = template.template.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun DefaultFeatureRow(
    onClick: () -> Unit,
    defaultFeatureRow: DefaultFeatureRow,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        defaultFeatureRow.bitmap?.asImageBitmap()?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = stringResource(R.string.default_feature),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(10.dp)
        )
    }
}

private fun DrawScope.drawCrossHair(
    center: Offset,
    color: Color,
    size: Float,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        Offset(center.x - size, center.y),
        Offset(center.x + size, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        Offset(center.x, center.y - size),
        Offset(center.x, center.y + size),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
