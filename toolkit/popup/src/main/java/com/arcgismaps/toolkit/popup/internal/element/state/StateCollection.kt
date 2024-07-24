/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.element.state

import androidx.compose.runtime.Immutable
import com.arcgismaps.mapping.popup.PopupElement

/**
 * An iterable collection that provides a [PopupElement] and its [PopupElementState] as a
 * [PopupElementStateCollection.Entry].
 */
@Immutable
internal interface PopupElementStateCollection : Iterable<PopupElementStateCollection.Entry> {
    interface Entry {
        val popupElement: PopupElement
        val state: PopupElementState
    }
}

/**
 * A mutable [PopupElementStateCollection].
 */
internal interface MutablePopupElementStateCollection : PopupElementStateCollection {

    /**
     * Adds a new [PopupElementStateCollection.Entry].
     *
     * @param popupElement the [PopupElement] to add.
     * @param state the [PopupElementState] to add.
     */
    fun add(popupElement: PopupElement, state: PopupElementState)

}

/**
 * Creates a new [mutablePopupElementStateCollection].
 */
internal fun mutablePopupElementStateCollection(): MutablePopupElementStateCollection = MutablePopupElementStateCollectionImpl()

/**
 * Default implementation for a [MutablePopupElementStateCollection].
 */
private class MutablePopupElementStateCollectionImpl : MutablePopupElementStateCollection {

    private val entries: MutableSet<PopupElementStateCollection.Entry> = mutableSetOf()

    override fun iterator(): Iterator<PopupElementStateCollection.Entry> = entries.iterator()

    @Suppress("RedundantUnitReturnType")
    override fun add(popupElement: PopupElement, state: PopupElementState) : Unit {
        entries.add(EntryImpl(popupElement, state))
    }

    /**
     * Default implementation for a [PopupElementStateCollection.Entry].
     */
    class EntryImpl(
        override val popupElement: PopupElement,
        override val state: PopupElementState
    ) : PopupElementStateCollection.Entry
}
