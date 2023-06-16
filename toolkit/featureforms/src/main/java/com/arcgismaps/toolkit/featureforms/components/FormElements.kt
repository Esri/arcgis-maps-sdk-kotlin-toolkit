package com.arcgismaps.toolkit.featureforms.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput

@Composable
public fun FieldElement(field: FieldFeatureFormElement) {
    when (field.inputType) {
        is TextAreaFeatureFormInput -> {
            TextArea(field = field, modifier = Modifier.fillMaxSize().padding(10.dp))
        }
        is TextBoxFeatureFormInput -> {
            TextBox(field = field, modifier = Modifier.fillMaxSize().padding(10.dp))
        }
        else -> {
        
        }
    }
}
