package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDescription
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField
import kotlinx.coroutines.launch

@Composable
internal fun ComboBoxField(state: ComboBoxFieldState, modifier: Modifier = Modifier) {
    val text = state.value.value
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    BaseTextField(
        text = text,
        onValueChange = {},
        readOnly = true,
        isEditable = state.isEditable,
        label = state.label,
        placeholder = state.placeholder,
        singleLine = true,
        trailingIcon = Icons.Outlined.List,
        supportingText = {
            Text(
                text = state.description,
                modifier = Modifier.semantics { contentDescription = "description" },
            )
        },
        interactionSource = interactionSource
    )

    if (showDialog) {
        ComboBoxDialog(
            initialValue = state.value.value,
            codedValues = state.codedValues.map { it.name },
            label = state.label,
            description = state.description,
            onValueChange = {
                state.onValueChanged(it)
            }
        ) {
            showDialog = false
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                showDialog = true
            }
        }
    }
}

@Composable
internal fun ComboBoxDialog(
    initialValue: String,
    codedValues: List<String>,
    label: String,
    description: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .padding(start = 15.dp, top = 15.dp, bottom = 10.dp, end = 10.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(text = "Filter $label")
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(15.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                        )
                        TextButton(onClick = onDismissRequest) {
                            Text(text = "Done")
                        }
                    }
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        codedValues.filter {
                            if (searchText.isEmpty()) {
                                true
                            } else {
                                it.contains(searchText, ignoreCase = true)
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text(text = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(it)
                                },
                            trailingContent = {
                                if (it == initialValue) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null
                                    )
                                }
                            })
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun ComboBoxDialogPreview() {
    ComboBoxDialog(
        initialValue = "Spruce",
        codedValues = listOf("Birch", "Maple", "Oak", "Spruce", "Hickory", "Hemlock"),
        label = "Types",
        description = "Select the tree species",
        onValueChange = {},
        onDismissRequest = {}
    )
}
