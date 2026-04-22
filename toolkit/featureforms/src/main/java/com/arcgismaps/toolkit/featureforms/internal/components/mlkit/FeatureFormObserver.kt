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

package com.arcgismaps.toolkit.featureforms.internal.components.mlkit

import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * A class to observe changes in a [FeatureForm]. The observer will only trigger on changes to
 * [FieldFormElement]s and [GroupFormElement]s when their editable or visible state changes, as
 * these are the properties that can affect the prompt for the generative model.
 *
 * @param featureForm The [FeatureForm] to observe.
 */
internal class FeatureFormObserver(
    private val featureForm: FeatureForm
) {

    /**
     * A flow that emits a value whenever a change occurs in the form that could affect the prompt.
     */
    val changes = merge(
        *buildList {
            getFieldsAsSequence().forEach { element ->
                if (element is FieldFormElement) {
                    add(element.isEditable.drop(1).map { Unit })
                }
                add(element.isVisible.drop(1).map { Unit })
            }
        }.toTypedArray()
    )


    /**
     * Returns all the observable [FormElement]s in the form as a sequence. This includes elements
     * that are nested within [GroupFormElement]s. Only [FieldFormElement]s and [GroupFormElement]s
     * are returned, as these are the only types of elements that can affect the prompt for the
     * generative model.
     *
     * @return A sequence of needed [FormElement]s in the form.
     */
    private fun getFieldsAsSequence(): Sequence<FormElement> {
        return featureForm.elements.asSequence().flatMap { element ->
            when (element) {
                is FieldFormElement -> sequenceOf(element)
                is GroupFormElement -> sequenceOf(element) + element.elements
                    .asSequence()
                    .filterIsInstance<FieldFormElement>()

                else -> emptySequence()
            }
        }
    }
}
