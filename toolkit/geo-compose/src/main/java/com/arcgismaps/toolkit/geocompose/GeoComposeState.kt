/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.DrawStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public sealed interface GeoComposeState {
    public val drawStatus: StateFlow<DrawStatus?>
    public fun setDrawStatus(drawStatus: DrawStatus)
}

internal open class GeoComposeStateImpl() : GeoComposeState {
    private val _drawStatus: MutableStateFlow<DrawStatus?> = MutableStateFlow(null)
    override val drawStatus: StateFlow<DrawStatus?> = _drawStatus.asStateFlow()
    override fun setDrawStatus(drawStatus: DrawStatus) {
        _drawStatus.value = drawStatus
    }
}
