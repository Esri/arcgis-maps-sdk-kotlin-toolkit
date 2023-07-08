package com.arcgismaps.toolkit.indoors

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
public fun Indoors(viewModel: FloorFilterInterface) {
    val text = viewModel.someProperty.collectAsState()
    Text(text = text.value)
}

@Preview
@Composable
internal fun IndoorsPreview() {
    val viewModel = object: FloorFilterInterface {
        private val _someProperty: MutableStateFlow<String> = MutableStateFlow("Hello Indoors Preview")
        override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
    }
    Indoors(viewModel)
}
