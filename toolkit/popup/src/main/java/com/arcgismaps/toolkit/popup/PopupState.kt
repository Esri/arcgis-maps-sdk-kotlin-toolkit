package com.arcgismaps.toolkit.popup

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.popup.Popup
import kotlinx.coroutines.CoroutineScope

public class PopupState(@Stable public val popup: Popup) {

    public constructor(popup: Popup, scope: CoroutineScope) : this(popup) {
    }

}





