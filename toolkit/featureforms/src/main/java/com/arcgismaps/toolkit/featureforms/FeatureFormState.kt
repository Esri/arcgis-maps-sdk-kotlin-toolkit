/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureforms

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork

public class FeatureFormState(
    private val featureForm: FeatureForm,
    validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic,
    public val utilityNetwork: UtilityNetwork? = null
) : LifecycleEventObserver {
    private val store = ArrayDeque<FeatureForm>()

    private var navController: NavController? = null

    private var _validationErrorVisibility: MutableState<ValidationErrorVisibility> =
        mutableStateOf(validationErrorVisibility)
    public val validationErrorVisibility: State<ValidationErrorVisibility>
        get() = _validationErrorVisibility

    init {
        store.addLast(featureForm)
    }

    internal fun setNavController(navController: NavController?) {
        this.navController = navController
    }

    internal fun setLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    internal fun navigateTo(form: FeatureForm) {
        store.addLast(form)
        val newRoute = NavigationRoute.Form
        navController?.navigate(newRoute) {
            this.restoreState = true
        }
    }

    internal fun popBackStack(): Boolean {
        return if (navController != null) {
            navController!!.popBackStack().also { result ->
                if (result) {
                    val last = store.removeLastOrNull()
                    Log.e("TAG", "popBackStack: removed ${last?.feature?.objectId}", )
                }
            }
        } else false
    }

    internal fun hasBackStack(): Boolean {
        return navController?.previousBackStackEntry != null
    }

    internal suspend fun fetchFeatureForm(utilityElement: UtilityElement): Result<FeatureForm> {
        if (utilityNetwork == null) {
            return Result.failure(Exception("UtilityNetwork is not available"))
        }
        val features = utilityNetwork.getFeaturesForElements(listOf(utilityElement)).getOrNull()
        if (features.isNullOrEmpty()) {
            return Result.failure(Exception("No features found for UtilityElement"))
        } else {
            val feature = features.first()
            val newForm = FeatureForm(feature)
            return Result.success(newForm)
        }
    }

    public fun getActiveFeatureForm(): FeatureForm {
        Log.e("TAG", "getActiveFeatureForm: ${store.lastOrNull()?.feature?.objectId}", )
        return store.lastOrNull() ?: featureForm
    }

    public fun setValidationErrorVisibility(visibility: ValidationErrorVisibility) {
        _validationErrorVisibility.value = visibility
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            navController = null
        }
    }
}

internal val ArcGISFeature.objectId: Long
    get() = attributes["objectid"] as Long
