package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.utils.ClearFocus
import com.arcgismaps.toolkit.featureforms.utils.PlaceholderTransformation
import kotlinx.coroutines.launch

@Composable
internal fun ComboBoxField(state: ComboBoxFieldState, modifier: Modifier = Modifier) {
    val text = state.selectedValue.value?.name ?: ""
    var showBottomSheet by remember { mutableStateOf(false) }
    var clearFocus by remember { mutableStateOf(false) }
    // if the keyboard is gone clear focus from the field as a side-effect
    ClearFocus(clearFocus) { clearFocus = false }

    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            // any tap on a blank space will also dismiss the keyboard and clear focus
            detectTapGestures { clearFocus = true }
        }
        .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                // state.onValueChanged(it)
            },
            modifier = Modifier
                .fillMaxSize()
                .focusable(!state.isEditable)
                .semantics { contentDescription = "outlined text field" },
            readOnly = true,
            enabled = state.isEditable,
            label = {
                Text(
                    text = state.label,
                    modifier = Modifier.semantics { contentDescription = "label" })
            },
            trailingIcon = {
                Icon(imageVector = Icons.Outlined.List, contentDescription = null)
            },
            supportingText = {
            },
            visualTransformation = if (text.isEmpty())
                PlaceholderTransformation(state.placeholder.ifEmpty { " " })
            else VisualTransformation.None,
            keyboardActions = KeyboardActions(
                onDone = { clearFocus = true }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            colors = if (text.isEmpty() && state.placeholder.isNotEmpty())
                OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.Gray,
                    focusedTextColor = Color.Gray
                )
            else
                OutlinedTextFieldDefaults.colors(),
            interactionSource = interactionSource
        )
    }

    if (showBottomSheet) {
        ComboBoxBottomSheet(state = state) {
            showBottomSheet = false
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                showBottomSheet = true
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComboBoxBottomSheet(state: ComboBoxFieldState, onDismissRequest: () -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = bottomSheetState) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(text = "Filter ${state.label}")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            bottomSheetState.hide()
                            // need to call this otherwise the BottomSheet is not fully hidden
                            onDismissRequest()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                }
            }
            Divider(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .height(2.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    state.codedValues.filter {
                        if (searchText.isEmpty()) {
                            true
                        } else {
                            it.name.contains(searchText, ignoreCase = true)
                        }
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(text = it.name) },
                        modifier = Modifier.clickable {
                            state.setValue(it)
                        },
                        trailingContent = {
                            if (it == state.selectedValue.value) {
                                Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                            }
                        })
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun ComboBoxPreview() {

}
