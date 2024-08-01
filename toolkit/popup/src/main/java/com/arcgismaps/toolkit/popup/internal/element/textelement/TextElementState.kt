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

package com.arcgismaps.toolkit.popup.internal.element.textelement

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementState
import kotlinx.parcelize.Parcelize

/**
 * A class to handle the state of a [TextPopupElement].
 *
 * @param value the text of the element.
 */
@Immutable
@Parcelize
internal class TextElementState(
    val value: String,
    override val id: Int
) : Parcelable, PopupElementState()

@Composable
internal fun rememberTextElementState(
    element: TextPopupElement,
    popup: Popup
): TextElementState = rememberSaveable(
    inputs = arrayOf(popup, element)
) {
    TextElementState(
        value = element.text,
        id = PopupElementState.createId()
    )
}
