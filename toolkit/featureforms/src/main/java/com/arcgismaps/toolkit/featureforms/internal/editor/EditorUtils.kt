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

package com.arcgismaps.toolkit.featureforms.internal.editor

import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun ArcGISFeature.id(): String? {
    val table = featureTable as ArcGISFeatureTable
    val globalId = attributes[table.globalIdField]
    val objectId = attributes[table.objectIdField]
    return if (globalId == null && objectId == null) {
        null
    } else {
        "${globalId}_${objectId}"
    }
}

/**
 * Returns a list of all the elements in the [FeatureForm], including the default attachments element
 * if it exists.
 */
internal val FeatureForm.allElements: List<FormElement>
    get() = elements + listOfNotNull(defaultAttachmentsElement)

/**
 * Applies the edits in the feature form to the service feature table if it is a service feature
 * table in a connected state. If there is no service connection, for example, in case of an
 * offline scenario, this function will be a no-op and return a successful result.
 *
 * @return a [Result] containing a list of [Throwable]s if there were any errors applying the edits
 * to the service feature table. Note that a successful result may still contain errors. A failure
 * result indicates that the applyEdits operation failed to complete.
 */
internal suspend fun FeatureForm.applyEditsToService(): Result<List<Throwable>> {
    val table = feature.featureTable as? ServiceFeatureTable
    // if the table is not a service feature table then return as there is nothing to sync
    val serviceFeatureTable = table ?: return Result.success(emptyList())
    if (serviceFeatureTable.serviceGeodatabase?.hasLocalEdits() == false) {
        // if there are no local edits across all the tables in the service geodatabase
        // then return as there is nothing to sync
        return Result.success(emptyList())
    }
    // check if the service supports applyEdits using the service geodatabase
    val canUseServiceGeodatabaseApplyEdits =
        serviceFeatureTable.serviceGeodatabase?.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true
    var result = Result.success(emptyList<Throwable>())
    withContext(Dispatchers.IO) {
        if (canUseServiceGeodatabaseApplyEdits) {
            serviceFeatureTable.serviceGeodatabase!!.applyEdits()
                .onSuccess { featureTableEditResults ->
                    // build a list of edit results from the feature table edit results
                    val errors = featureTableEditResults.flatMap {
                        it.editResults.asSequence()
                    }.errors
                    result = Result.success(errors)
                }
                .onFailure {
                    result = Result.failure(it)
                }
        } else {
            serviceFeatureTable.applyEdits().onSuccess { featureEditResults ->
                result = Result.success(featureEditResults.errors)
            }.onFailure {
                result = Result.failure(it)
            }
        }
    }
    return result
}

/**
 * Returns a list of all the errors in the list of [FeatureEditResult]s that have an error
 * including the attachment results that have an error.
 */
private val List<FeatureEditResult>.errors: List<Throwable>
    get() = mapNotNull { editResult ->
        editResult.error
    } + flatMap {
        it.attachmentResults.mapNotNull { editResult ->
            editResult.error
        }
    }
