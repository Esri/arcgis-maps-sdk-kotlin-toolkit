package com.arcgismaps.toolkit.featureforms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public interface FeatureFormViewModelInterface {
    public val feature: StateFlow<ArcGISFeature?>
    public val visible: StateFlow<Boolean>
    public fun setFeature(feature: ArcGISFeature)
    public fun setFormVisibility(visible: Boolean)
}

/**
 * A view model for the FeatureForms MapView UI
 */
public class FeatureFormViewModelImpl : ViewModel(), FeatureFormViewModelInterface {
    private val _feature: MutableStateFlow<ArcGISFeature?> = MutableStateFlow(null)
    override val feature: StateFlow<ArcGISFeature?> = _feature.asStateFlow()
    private val _visible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val visible: StateFlow<Boolean> = _visible.asStateFlow()
    override fun setFeature(feature: ArcGISFeature) {
        _feature.value = feature
    }
    
    override fun setFormVisibility(visible: Boolean) {
        _visible.value = visible
    }
}

public class FeatureFormViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormViewModelImpl() as T
    }
}

