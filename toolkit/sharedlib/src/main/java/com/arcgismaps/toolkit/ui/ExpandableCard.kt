package com.arcgismaps.toolkit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Composable Card that has the ability to expand and collapse its [content].
 *
 * @since 200.5.0
 */
@Composable
public fun ExpandableCard(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    toggleable: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    // TODO: promote to public theme.
    val shapes = ExpandableCardDefaults.shapes()
    val colors = ExpandableCardDefaults.colors()
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.containerColor
        ),
        border = BorderStroke(shapes.borderThickness, colors.borderColor),
        shape = shapes.containerShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(shapes.padding)
    ) {
        Column {
            ExpandableHeader2(
                title = title,
                description = description,
                expandable = toggleable,
                colors = colors,
                isExpanded = expanded
            ) {
                if (toggleable) {
                    expanded = !expanded
                }
            }

            AnimatedVisibility(visible = expanded) {
                content()
            }

        }
    }
}

@Composable
private fun ExpandableHeader2(
    title: String = "",
    description: String = "",
    expandable: Boolean,
    colors: ExpandableCardColors,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    if (title.isEmpty() && description.isEmpty() && !expandable) return
    val shapes = ExpandableCardDefaults.shapes()
    Row(
        Modifier
            .fillMaxWidth()
            .applyIf(expandable) {
                clickable {
                    onClick()
                }
            }
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(shapes.padding)
                .weight(0.5f)
        ) {
            Text(
                text = title,
                color = colors.headerTextColor,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty() && isExpanded) {
                Text(
                    text = description,
                    color = colors.headerTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }

        if (expandable) {
            Crossfade(targetState = isExpanded, label = "expandPopupElement") {
                Icon(
                    modifier = Modifier
                        .padding(16.dp),
                    imageVector = if (it) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Expand"
                )
            }
        }
    }
}

@Preview
@Composable
internal fun ExpandableHeaderPreview() {
    ExpandableHeader2(
        title = "The Title",
        colors = ExpandableCardDefaults.colors(),
        description = "the description",
        expandable = true,
        isExpanded = true
    ) {}
}

@Preview
@Composable
private fun ExpandableCardPreview() {
    ExpandableCard(
        description = "Foo",
        title = "Title"
    ) {
        Text(
            "Hello World",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

internal fun Modifier.applyIf(condition: Boolean, then: Modifier.() -> Modifier): Modifier =
    if (condition) {
        then()
    } else {
        this
    }


