package com.arcgismaps.toolkit.featureforms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.data.ArcGISFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An interface to provide the feature form state and control.
 *
 * @since 200.2.0
 */
public interface FeatureFormViewModelInterface {
    /**
     * The feature for which form data is editable
     *
     * @since 200.2.0
     */
    public val feature: StateFlow<ArcGISFeature?>
    
    /**
     * Indicates that the form UI is available to the user for editing
     *
     * @since 200.2.0
     */
    public val inEditingMode: StateFlow<Boolean>
    
    /**
     * Sets the feature to which edits will be applied.
     *
     * @since 200.2.0
     */
    public fun setFeature(feature: ArcGISFeature)
    
    /**
     * Sets the editing mode of the form
     *
     * @since 200.2.0
     */
    public fun setEditingActive(active: Boolean)
}

/**
 * A view model for the FeatureForms MapView UI
 */
public class FeatureFormViewModelImpl : ViewModel(), FeatureFormViewModelInterface {
    private val _feature: MutableStateFlow<ArcGISFeature?> = MutableStateFlow(null)
    override val feature: StateFlow<ArcGISFeature?> = _feature.asStateFlow()
    private val _inEditingMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val inEditingMode: StateFlow<Boolean> = _inEditingMode.asStateFlow()
    override fun setFeature(feature: ArcGISFeature) {
        _feature.value = feature
    }
    
    override fun setEditingActive(active: Boolean) {
        _inEditingMode.value = active
    }
}

public class FeatureFormViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FeatureFormViewModelImpl() as T
    }
}

