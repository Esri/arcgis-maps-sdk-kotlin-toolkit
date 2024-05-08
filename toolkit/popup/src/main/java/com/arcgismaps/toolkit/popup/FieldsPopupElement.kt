package com.arcgismaps.toolkit.popup

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arcgismaps.mapping.popup.FieldsPopupElement

@Composable
internal fun FieldsPopupElement(element: FieldsPopupElement) {
    val fields = element.fields
    val formattedValues = element.formattedValues

    Column {
        fields.forEachIndexed { index, field ->
            // Display the field
            Column {
                ListItem(
                    // Display the title
                    headlineContent = { Text(text = field.label) },
                // Display the value
                    supportingContent = { Text(text = formattedValues[index]) }
                )
            }
        }
    }

}


