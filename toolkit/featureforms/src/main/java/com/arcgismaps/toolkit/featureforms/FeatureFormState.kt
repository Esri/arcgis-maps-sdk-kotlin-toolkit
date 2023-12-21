package com.arcgismaps.toolkit.featureforms

import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.featureforms.FeatureForm
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
     * The FeatureForm that defines the Form and provides Feature access.
     *
     * @since 200.2.0
     */
    public val featureForm: StateFlow<FeatureForm?>
    
    /**
     * Indicates that the form UI is available to the user for editing
     *
     * @since 200.2.0
     */
    public val transactionState: StateFlow<EditingTransactionState>
    
    /**
     * Sets the feature form to which edits will be applied.
     *
     * @since 200.2.0
     */
    public fun setFeatureForm(featureForm: FeatureForm)
    
    /**
     * Sets the editing mode of the form
     *
     * @since 200.2.0
     */
    public fun setTransactionState(state: EditingTransactionState)
    
    
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
    private val _featureForm: MutableStateFlow<FeatureForm?> = MutableStateFlow(null)
    override val featureForm: StateFlow<FeatureForm?> = _featureForm.asStateFlow()
    private val _transactionState: MutableStateFlow<EditingTransactionState> = MutableStateFlow(EditingTransactionState.NotEditing)
    override val transactionState: StateFlow<EditingTransactionState> = _transactionState.asStateFlow()
    override fun setTransactionState(state: EditingTransactionState) {
        _transactionState.value = state
    }
    
    override suspend fun rollbackEdits(stateAfterRollback: EditingTransactionState): Result<Unit> {
        setTransactionState(EditingTransactionState.RollingBack)
        val feature = featureForm.value?.feature
        (feature?.featureTable as? ServiceFeatureTable)?.undoLocalEdits()
        feature?.refresh()
        setTransactionState(stateAfterRollback)
        return Result.success(Unit)
    }
    
    override fun setFeatureForm(featureForm: FeatureForm) {
        _featureForm.value = featureForm
    }
}

/**
 * Factory function for the default implementation of [FeatureFormState]
 */
public fun FeatureFormState(): FeatureFormState = FeatureFormStateImpl()