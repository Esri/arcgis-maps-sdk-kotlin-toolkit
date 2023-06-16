package com.arcgismaps.toolkit.featureforms.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun TextArea(
    field: FieldFeatureFormElement,
    modifier: Modifier = Modifier
) {
    val textBoxInput = field.inputType as TextAreaFeatureFormInput
    FormTextField(
        label = field.label,
        modifier = modifier,
        description = field.description,
        minLength = textBoxInput.minLength,
        maxLength = textBoxInput.maxLength,
        singleLine = false
    )
}
