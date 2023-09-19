package com.arcgismaps.toolkit.featureforms.components.combo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
internal fun ComboBoxField(state: ComboBoxFieldState) {
    val value by state.selectedValue

    OutlinedTextField(
        value = value?.name ?: "no val",
        onValueChange = {

        },
        modifier = Modifier.fillMaxSize(),
        label = {
            Text(text = state.label)
        }
    )
}
