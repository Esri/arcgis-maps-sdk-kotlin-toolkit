/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewerapp

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.arcgismaps.toolkit.orientedimageryviewerapp.screens.MainScreen
import com.esri.microappslib.theme.MicroAppTheme

class MainActivity : ComponentActivity() {
    private val requestMediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: viewer component checks current permission state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestViewerMediaPermissionIfNeeded()
        setContent {
            MicroAppTheme {
                OrientedImageryViewerApp()
            }
        }
    }

    private fun requestViewerMediaPermissionIfNeeded() {
        val permission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                @Suppress("DEPRECATION")
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestMediaPermissionLauncher.launch(permission)
        }
    }
}

@Composable
fun OrientedImageryViewerApp() {
    MainScreen()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MicroAppTheme {
        OrientedImageryViewerApp()
    }
}
