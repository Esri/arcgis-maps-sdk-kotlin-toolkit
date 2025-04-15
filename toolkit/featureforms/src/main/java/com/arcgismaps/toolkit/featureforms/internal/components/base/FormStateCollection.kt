/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.base

import androidx.compose.runtime.Immutable
import com.arcgismaps.mapping.featureforms.FormElement

/**
 * An iterable collection that provides a [FormElement] and its [FormElementState] as a
 * [FormStateCollection.Entry]. This does not allow for duplicate entries.
 */
@Immutable
internal interface FormStateCollection : Iterable<FormStateCollection.Entry> {

    /**
     * Provides the bracket operator to the collection.
     *
     * @param formElement the search for in the collection
     * @return the [FormElementState] associated with the formElement, or null if none.
     */
    operator fun get(formElement: FormElement): FormElementState?

    /**
     * Provides the bracket operator to the collection.
     *
     * @param id the unique identifier [FormElementState.id]
     * @return the [FormElementState] associated with the id, or null if none.
     */
    operator fun get(id: Int): FormElementState?

    interface Entry {
        val formElement: FormElement
        val state: FormElementState
        override fun equals(other: Any?): Boolean
        override fun hashCode(): Int
    }
}

/**
 * A Mutable iterable collection that provides a [FormElement] and its [FormElementState] as a
 * [FormStateCollection.Entry]. This does not allow for duplicate entries.
 */
internal interface MutableFormStateCollection : FormStateCollection {

    /**
     * Adds a new [FormStateCollection.Entry].
     *
     * @param formElement the [FormElement] to add.
     * @param state the [FormElementState] to add.
     * @return true if the entry was added, false otherwise.
     */
    fun add(formElement: FormElement, state: FormElementState) : Boolean
}

/**
 * Creates a new [MutableFormStateCollection].
 */
internal fun MutableFormStateCollection(): MutableFormStateCollection =
    MutableFormStateCollectionImpl()

/**
 * Default implementation for a [MutableFormStateCollection].
 */
private class MutableFormStateCollectionImpl : MutableFormStateCollection {

    private val entries: LinkedHashSet<FormStateCollection.Entry> = linkedSetOf()

    override fun iterator(): Iterator<FormStateCollection.Entry> {
        return entries.iterator()
    }

    override fun add(formElement: FormElement, state: FormElementState) : Boolean {
        return entries.add(EntryImpl(formElement, state))
    }

    override operator fun get(formElement: FormElement): FormElementState? =
        get(formElement.hashCode())

    override operator fun get(id: Int): FormElementState? {
        entries.forEach { entry ->
            when (entry.state) {
                is BaseGroupState -> {
                    val groupState = entry.state as BaseGroupState
                    groupState.fieldStates.forEach { childEntry ->
                        if (childEntry.state.id == id) {
                            return childEntry.state
                        }
                    }
                }

                else -> if (entry.state.id == id) {
                    return entry.state
                }
            }
        }
        return null
    }

    /**
     * Default implementation for a [FormStateCollection.Entry].
     */
    class EntryImpl(
        override val formElement: FormElement,
        override val state: FormElementState
    ) : FormStateCollection.Entry {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            other as EntryImpl

            return formElement == other.formElement
        }

        override fun hashCode(): Int = formElement.hashCode()

    }
}

/**
 * Casts and returns the [FormStateCollection.Entry.state] into the specified type. The type specified
 * must be a sub-class of a [FormElementState].
 *
 * @throws ClassCastException Throws an exception if the cast fails.
 */
internal inline fun <reified T : FormElementState> FormStateCollection.Entry.getState(): T {
    if (state is T) {
        return state as T
    } else {
        throw ClassCastException("${state::class} cannot be cast into ${T::class}")
    }
}
