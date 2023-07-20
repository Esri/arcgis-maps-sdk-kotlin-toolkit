package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.toolkit.featureforms.api.FeatureFormDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The editing state of the FeatureForm.
 *
 * @since 200.2.0
 */
public sealed class EditingTransactionState {
    /**
     * No ongoing editing session.
     *
     * @since 200.2.0
     */
    public object NotEditing: EditingTransactionState()
    
    /**
     * An editing session is ongoing.
     *
     * @since 200.2.0
     */
    public object Editing: EditingTransactionState()
    
    /**
     * The Feature is being updated and the edits are being applied to the Feature's Geodatabase
     *
     *  @since 200.2.0
     */
    public object Committing: EditingTransactionState()
    
    /**
     * Local edits to the Feature's attributes are being discarded.
     *
     *  @since 200.2.0
     */
    public object RollingBack: EditingTransactionState()
}

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
    public val transactionState: StateFlow<EditingTransactionState>
    
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
    public fun setTransactionState(state: EditingTransactionState)
    
    /**
     * Save form edits to the Feature
     * @param stateAfterCommit the state to put the form into after the commit is completed.
     *
     * @since 200.2.0
     */
    public suspend fun commitEdits(stateAfterCommit: EditingTransactionState): Result<Unit>
    
    /**
     * Discard form edits to the Feature
     * @param stateAfterRollback the state to put the form into after the rollback is completed.
     *
     * @since 200.2.0
     */
    public suspend fun rollbackEdits(stateAfterRollback: EditingTransactionState): Result<Unit>
}

/**
 * Default implementation for the [FeatureFormState]
 */
public class FeatureFormStateImpl : FeatureFormState {
    private val _formDefinition: MutableStateFlow<FeatureFormDefinition?> = MutableStateFlow(null)
    override val formDefinition: StateFlow<FeatureFormDefinition?> = _formDefinition.asStateFlow()
    private val _inEditingTransaction: MutableStateFlow<EditingTransactionState> = MutableStateFlow(EditingTransactionState.NotEditing)
    override val transactionState: StateFlow<EditingTransactionState> = _inEditingTransaction.asStateFlow()
    override fun setTransactionState(state: EditingTransactionState) {
        _inEditingTransaction.value = state
    }
    
    public override suspend fun commitEdits(stateAfterCommit: EditingTransactionState): Result<Unit> {
        setTransactionState(EditingTransactionState.Committing)
        val feature = formDefinition.value?.feature
            ?: return Result.failure(IllegalStateException("cannot save feature edit without a Feature"))
        val serviceFeatureTable =
            formDefinition.value?.feature?.featureTable as? ServiceFeatureTable ?: return Result.failure(
                IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
            )
        
        val result = serviceFeatureTable.updateFeature(feature)
            .map {
                serviceFeatureTable.serviceGeodatabase?.applyEdits()
                    ?: throw IllegalStateException("cannot apply feature edit without a ServiceGeodatabase")
                feature.refresh()
                Unit
            }
        
        // note: this will silently fail and close the form.
        setTransactionState(stateAfterCommit)
        return result
    }
    
    override suspend fun rollbackEdits(stateAfterRollback: EditingTransactionState): Result<Unit> {
        setTransactionState(EditingTransactionState.RollingBack)
        val feature = formDefinition.value?.feature
        (feature?.featureTable as? ServiceFeatureTable)?.undoLocalEdits()
        feature?.refresh()
        setTransactionState(stateAfterRollback)
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
