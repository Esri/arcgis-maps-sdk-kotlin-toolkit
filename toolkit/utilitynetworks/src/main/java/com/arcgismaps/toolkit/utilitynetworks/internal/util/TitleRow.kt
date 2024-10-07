package com.arcgismaps.toolkit.utilitynetworks.internal.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R

@Composable
internal fun Title(name: String, onZoomToResults: () -> Unit, onDeleteResult: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box {
            Row(modifier = Modifier.clickable {
                expanded = !expanded
            }, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = ""
                )
            }
            MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.zoom_to_result)) },
                        onClick = onZoomToResults,
                        leadingIcon = { Icon(
                            Icons.Outlined.LocationOn, contentDescription = stringResource(
                                R.string.zoom_to_trace_result)
                        ) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = onDeleteResult,
                        leadingIcon = { Icon(
                            Icons.Outlined.Clear, contentDescription = stringResource(
                                R.string.delete_trace_result)
                        ) }
                    )
                }
            }
        }
    }
}