package com.arcgismaps.toolkit.featureformsapp.screens.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.featureforms.FormInterface
import com.arcgismaps.toolkit.featureforms.FormInterfaceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FormViewModel : ViewModel(), FormInterface by FormInterfaceImpl() {

    private val _visible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun setFormVisibility(visible: Boolean) {
        _visible.value = visible
    }
}

class FormViewModelFactory() : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FormViewModel() as T
    }
}
