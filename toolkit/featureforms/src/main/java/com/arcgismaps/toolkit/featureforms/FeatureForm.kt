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
import com.arcgismaps.toolkit.featureforms.api.TextAreaFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.TextBoxFeatureFormInput
import com.arcgismaps.toolkit.featureforms.api.formInfoJson

@Composable
public fun FeatureForm(formInterface: FormInterface) {
    val feature by formInterface.feature.collectAsState()
    val featureTable = feature?.featureTable as ArcGISFeatureTable?
    featureTable?.formInfoJson?.let {
        println("hi")
        FeatureFormDefinition.fromJsonOrNull(it)?.let { formDefinition ->
            println("hi2")
            LazyColumn {
                println("form elements ${formDefinition.formElements.size}")
                items(formDefinition.formElements) { formElement ->
                    if (formElement is FieldFeatureFormElement) {
                        println("fieldformelement $formElement")
                        Field(field = formElement)
                    }
                }
            }
        }
    }
}

@Composable
public fun Field(field: FieldFeatureFormElement) {
    println("${field.inputType}")
    
    when (field.inputType) {
        is TextAreaFeatureFormInput -> {
        
        }
        
        is TextBoxFeatureFormInput -> {
            println("HUUUUUU ${field.value}")
            TextBox(field = field)
        }
        
        else -> {}
    }
    //TextBox(field = field)
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
        OutlinedTextField(value = text, onValueChange = {
            text = it
            if (it.length in textBoxInput.minLength.toInt()..textBoxInput.maxLength.toInt()) {
                supportingText = field.description
            } else {
                supportingText = ""
            }
        }, label = {
            Text(text = field.label)
        }, placeholder = {
            Text(text = "Hint")
        }, singleLine = true
        )
        Text(text = supportingText)
    }
}

@Composable
public fun TextArea() {

}
