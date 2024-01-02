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

package com.arcgismaps.toolkit.featureforms.components.base

import com.arcgismaps.mapping.featureforms.FormElement

/**
 * An iterable collection that provides a [FormElement] and its [FormElementState] as a
 * [StateCollection.Entry].
 */
internal interface StateCollection : Iterable<StateCollection.Entry> {
    interface Entry {
        val formElement: FormElement
        val state: FormElementState
    }
}

/**
 * A mutable [StateCollection].
 */
internal interface MutableStateCollection : StateCollection {

    /**
     * Adds a new [StateCollection.Entry].
     *
     * @param formElement the [FormElement] to add.
     * @param state the [FormElementState] to add.
     */
    fun add(formElement: FormElement, state: FormElementState)
}

/**
 * Creates a new [MutableStateCollection].
 */
internal fun MutableStateCollection(): MutableStateCollection = MutableStateCollectionImpl()

/**
 * Default implementation for a [MutableStateCollection].
 */
private class MutableStateCollectionImpl : MutableStateCollection {

    private val entries: MutableList<StateCollection.Entry> = mutableListOf()

    override fun iterator(): Iterator<StateCollection.Entry> {
        return entries.iterator()
    }

    override fun add(formElement: FormElement, state: FormElementState) {
        entries.add(EntryImpl(formElement, state))
    }

    /**
     * Default implementation for a [StateCollection.Entry].
     */
    class EntryImpl(
        override val formElement: FormElement,
        override val state: FormElementState
    ) : StateCollection.Entry
}

/**
 * Casts and returns the [StateCollection.Entry.state] into the specified type. The type specified
 * must be a sub-class of a [FormElementState].
 *
 * @throws ClassCastException Throws an exception if the cast fails.
 */
internal inline fun <reified T : FormElementState> StateCollection.Entry.getState(): T {
    if (state is T) {
        return state as T
    } else {
        throw ClassCastException("${state::class} cannot be cast into ${T::class}")
    }
}
