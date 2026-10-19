/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.SavedEditsState
import com.arcgismaps.toolkit.featureforms.internal.utils.ApplyEditsException
import com.arcgismaps.toolkit.featureforms.internal.utils.FinishEditingException
import com.arcgismaps.toolkit.featureforms.internal.utils.FormExpressionEvaluationException
import com.arcgismaps.toolkit.featureforms.internal.utils.InvalidFormGroupStateException
import com.arcgismaps.toolkit.featureforms.internal.utils.InvalidFormStateException

@Composable
internal fun SavingsEditsDialog(
    onDismissRequest: () -> Unit,
    state: SavedEditsState
) {
    when (state) {
        SavedEditsState.Saved -> {
            LaunchedEffect(Unit) {
                onDismissRequest()
            }
        }

        is SavedEditsState.Error -> {
            ErrorDialog(
                onDismissRequest = onDismissRequest,
                onAction = null,
                title = stringResource(R.string.error_saving_edits),
                body = saveEditsErrorMessage(state.error),
                actionText = ""
            )
        }

        else -> {}
    }
}

@Composable
private fun SavingEditsProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(stringResource(R.string.saving_edits))
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    )
}

@Composable
private fun saveEditsErrorMessage(error: Throwable): String {
    return when (error) {
        is InvalidFormGroupStateException -> {
            pluralStringResource(
                R.plurals.form_group_has_validation_errors,
                error.count,
                error.count
            )
        }

        is InvalidFormStateException -> {
            pluralStringResource(
                R.plurals.you_have_errors_that_must_be_fixed_before_saving,
                error.count,
                error.count
            )
        }

        is FormExpressionEvaluationException -> {
            val errorCount = error.errors.size
            pluralStringResource(
                R.plurals.form_expression_evaluation_errors,
                errorCount,
                errorCount
            )
        }

        is FinishEditingException -> {
            error.message ?: stringResource(R.string.unknown_error)
        }

        is ApplyEditsException -> {
            error.message ?: stringResource(R.string.unknown_error)
        }

        else -> {
            stringResource(
                R.string.error_while_saving_the_edits,
                error.localizedMessage ?: stringResource(R.string.unknown_error)
            )
        }
    }
}
