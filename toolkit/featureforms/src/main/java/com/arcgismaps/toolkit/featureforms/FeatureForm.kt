package com.arcgismaps.toolkit.featureforms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import com.arcgismaps.toolkit.featureforms.api.FieldFeatureFormElement
import com.arcgismaps.toolkit.featureforms.api.TestData
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.formInfoJson

@Composable
public fun FeatureForm(formInterface: FormInterface) {
    val feature by formInterface.feature.collectAsState()
    val featureTable = feature?.featureTable as ArcGISFeatureTable?
    val formInfo = featureTable?.formInfoJson ?: ""

    FeatureFormDefinition.fromJsonOrNull(TestData.formInfo)?.let { formDefinition ->
        LazyColumn {
            items(formDefinition.formElements) { formElement ->
                if (formElement is FieldFeatureFormElement) {
                    Field(field = formElement)
                }
            }
        }
    }
}

@Composable
public fun Field(field: FieldFeatureFormElement) {
    when (field.inputType) {
        is TextAreaFeatureFormInput -> {

        }

        is TextBoxFeatureFormInput -> {
            TextBox(field = field)
        }
    }
    TextBox(field = field)
}

@Composable
public fun TextBox(field: FieldFeatureFormElement) {
    val textBoxInput = field.inputType as TextBoxFeatureFormInput
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var supportingText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    supportingText = field.description.ifEmpty { "" }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                if (it.length in textBoxInput.minLength..textBoxInput.maxLength) {
                    supportingText = field.description
                } else {
                    supportingText = ""
                }
            },
            label = {
                Text(text = field.label)
            },
            placeholder = {
                Text(text = "Hint")
            },
            singleLine = true
        )
        Text(text = supportingText)
    }
}

@Composable
public fun TextArea() {

}
