package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A state holder to provide the feature form state and control.
 *
 * @since 200.2.0
 */
public interface FeatureFormState {
    /**
     * The FormDefinition that defines the Form
     *
     * @since 200.2.0
     */
    public val formDefinition: StateFlow<FeatureFormDefinition?>
    
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
    public fun setFormDefinition(definition: FeatureFormDefinition)
    
    /**
     * Sets the editing mode of the form
     *
     * @since 200.2.0
     */
    public fun setEditingActive(active: Boolean)
    
    /**
     * Save form edits to the Feature
     *
     * @since 200.2.0
     */
    public suspend fun saveFeatureEdits(): Result<Unit>
    
    /**
     * Discard form edits to the Feature
     *
     * @since 200.2.0
     */
    public suspend fun discardFeatureEdits(): Result<Unit>
}

/**
 * Default implementation for the [FeatureFormState]
 */
public class FeatureFormStateImpl : FeatureFormState {
    private val _formDefinition: MutableStateFlow<FeatureFormDefinition?> = MutableStateFlow(null)
    override val formDefinition: StateFlow<FeatureFormDefinition?> = _formDefinition.asStateFlow()
    private val _inEditingMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val inEditingMode: StateFlow<Boolean> = _inEditingMode.asStateFlow()
    override fun setEditingActive(active: Boolean) {
        _inEditingMode.value = active
    }
    
    public override suspend fun saveFeatureEdits(): Result<Unit> {
        val feature = formDefinition.value?.feature
            ?: return Result.failure(IllegalStateException("cannot save feature edit without a Feature"))
        val serviceFeatureTable =
            formDefinition.value?.feature?.featureTable as? ServiceFeatureTable ?: return Result.failure(
                IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
            )
        
        return serviceFeatureTable.updateFeature(feature)
            .map {
                serviceFeatureTable.serviceGeodatabase?.applyEdits()
                    ?: throw IllegalStateException("cannot apply feature edit without a ServiceGeodatabase")
                feature.refresh()
                Unit
            }
    }
    
    override suspend fun discardFeatureEdits(): Result<Unit> {
        val feature = formDefinition.value?.feature
        (feature?.featureTable as? ServiceFeatureTable)?.undoLocalEdits()
        feature?.refresh()
        return Result.success(Unit)
    }
    
    override fun setFormDefinition(definition: FeatureFormDefinition) {
        _formDefinition.value = definition
    }
}

/**
 * Factory function for the default implementation of [FeatureFormState]
 */
public fun FeatureFormState(): FeatureFormState = FeatureFormStateImpl()
