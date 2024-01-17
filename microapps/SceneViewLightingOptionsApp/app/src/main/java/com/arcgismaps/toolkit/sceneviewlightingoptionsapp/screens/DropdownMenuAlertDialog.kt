package com.arcgismaps.toolkit.sceneviewlightingoptionsapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Displays an alert dialog with a dropdown containing the items in [itemList].
 *
 * The index of the selected item when the user presses the confirm button is passed to [onConfirm].
 *
 * @param itemList the list of item labels to be displayed in the dropdown
 * @param currentSelectedIndex the index of the item in [itemList] to display as currently selected
 * @param title the title of this alert dialog
 * @param onDismissRequest called when the dialog should be dismissed
 * @param onConfirm called when the user presses the confirm button, indicating a new item to select
 */
@Composable
fun DropdownMenuAlertDialog(
    itemList: List<String>,
    currentSelectedIndex: Int,
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(currentSelectedIndex) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedIndex)
            }) {
                Text("Confirm")
            }
        },
        title = {
            Text(title)
        },
        text = {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        dropdownExpanded = !dropdownExpanded
                    }
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        itemList[selectedIndex] ?: "Unexpected index",
                        modifier = Modifier.padding(8.dp)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "Select Item",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                itemList.forEachIndexed { idx, name ->
                    DropdownMenuItem(
                        text = {
                            Text(name)
                        },
                        onClick = {
                            selectedIndex = idx
                            dropdownExpanded = false
                        })
                }
            }
        }
    )
}
