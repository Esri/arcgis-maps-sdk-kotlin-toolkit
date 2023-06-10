/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.templateapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.toolkit.template.TemplateInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TemplateViewModel(
    initialValue: String
) : ViewModel(), TemplateInterface {
    private val _someProperty: MutableStateFlow<String> = MutableStateFlow(initialValue)
    override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
}

class TemplateViewModelFactory(
    private val initialValue: String = ""
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TemplateViewModel(initialValue) as T
    }
}
