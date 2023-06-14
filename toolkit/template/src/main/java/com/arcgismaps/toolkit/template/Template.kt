package com.arcgismaps.toolkit.template

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
public fun Template(viewModel: TemplateInterface) {
    val text = viewModel.someProperty.collectAsState()
    Text(text = text.value)
}

@Preview
@Composable
internal fun TemplatePreview() {
    val viewModel = object: TemplateInterface {
        private val _someProperty: MutableStateFlow<String> = MutableStateFlow("Hello Template Preview")
        override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
    }
    Template(viewModel)
}
