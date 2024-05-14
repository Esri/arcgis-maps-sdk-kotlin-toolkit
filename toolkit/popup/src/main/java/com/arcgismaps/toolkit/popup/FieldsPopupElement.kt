package com.arcgismaps.toolkit.popup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.PopupField

@Composable
internal fun FieldsPopupElement(element: FieldsPopupElement) {
    val fieldValues = element.fields.zip(element.formattedValues).toMap()
    AnimatedVisibility(visible = true) {
        Column {
            fieldValues.forEach {
                // Display the field
                Column {
                    ListItem(
                        headlineContent = { Text(text = it.key.label) },
                        supportingContent = { Text(text = it.value.ifEmpty { "--" }) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FieldsPopupElementPreview() {
    val fieldPopupElement = FieldsPopupElement(
        fields = listOf(
            PopupField().apply { label = "Field 1" },
            PopupField().apply { label = "Field 2" },
            PopupField().apply { label = "Field 3" }
        )
    )
    ExpandableCard(title = "Fields Popup Element") {
        FieldsPopupElement(fieldPopupElement)
    }
}


