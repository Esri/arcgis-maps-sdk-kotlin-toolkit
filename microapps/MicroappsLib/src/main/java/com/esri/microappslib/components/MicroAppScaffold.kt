package com.esri.microappslib.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.esri.microappslib.theme.MicroAppTheme

/**
 * Composable component to display a [Scaffold] with a customizable [TopAppBar]
 * using the provided [MenuActions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroAppScaffold(
    menuActions: MenuActions = MenuActions.None,
    title: String = "MicroApp",
    isSwitchEnabled: Boolean = false,
    onSwitchToggle: (Boolean) -> Unit = {},
    dropdownListItems: List<String> = listOf(),
    onItemSelected: (Int, String) -> Unit = { _: Int, _: String -> },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = { Text(title) },
                actions = {
                    when (menuActions) {
                        MenuActions.Switch -> {
                            Switch(
                                checked = isSwitchEnabled,
                                onCheckedChange = {
                                    onSwitchToggle.invoke(it)
                                })
                        }

                        MenuActions.DropDownMenu -> {
                            var actionsExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                                Icon(Icons.Default.MoreVert, "More")
                            }
                            MicroAppDropDown(
                                expanded = actionsExpanded,
                                dropdownListItems = dropdownListItems,
                                onDismissRequest = {
                                    actionsExpanded = false
                                }
                            )
                        }

                        MenuActions.SwitchAndDropDownMenu -> {
                            Switch(
                                checked = isSwitchEnabled,
                                onCheckedChange = {
                                    onSwitchToggle.invoke(it)
                                })

                            var actionsExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                                Icon(Icons.Default.MoreVert, "More")
                            }
                            MicroAppDropDown(
                                expanded = actionsExpanded,
                                dropdownListItems = dropdownListItems,
                                onDismissRequest = {
                                    actionsExpanded = false
                                },
                                onItemSelected = onItemSelected
                            )
                        }

                        else -> {
                            /*Do nothing*/
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        // Displays microapp content
        content.invoke(innerPadding)
    }
}

@Composable
fun MicroAppDropDown(
    modifier: Modifier = Modifier,
    dropdownListItems: List<String> = listOf(),
    expanded: Boolean = false,
    onDismissRequest: () -> Unit = {},
    onItemSelected: (Int, String) -> Unit = { _: Int, _: String -> },
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        dropdownListItems.forEach {
            DropdownMenuItem(
                text = { Text(text = it) },
                onClick = {
                    onDismissRequest()
                    onItemSelected.invoke(dropdownListItems.indexOf(it), it)
                })
        }
    }
}


enum class MenuActions { None, Switch, DropDownMenu, SwitchAndDropDownMenu }

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MicroAppScaffoldPreview() {
    MicroAppTheme {
        MicroAppScaffold(
            menuActions = MenuActions.SwitchAndDropDownMenu,
            title = "MicroApp Title",
            dropdownListItems = listOf("Option 1")

        ) {
            Text(text = "Hello world! ")
        }
    }
}
