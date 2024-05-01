/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.popup

import android.content.Intent
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.popup.TextPopupElement

/**
 * Renders the following [content] as HTML.
 *
 * @since 200.5.0
 */
@Composable
internal fun HTML(content: String) {
    // Set the initial scale to 1 to not allow user scaling
    val header = """
        <header>
            <meta name='viewport'
                content='width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no'>
        </header>
    """.trimIndent()


    // Inject css in a head element to:
    // - word wrap long content such as urls
    // - set font family to sans serif
    // - limit images to a maximum width of 100%
    val headStyle = """
        <head><style>
            html { word-wrap:break-word; font-family:sans-serif; }
            img { max-width: 100%; }
            body { margin:16px; padding:0px; }
        </style></head>
        """.trimIndent()
    AndroidView(factory = { context ->
        WebView(context).apply {
            webViewClient = object : WebViewClient() {

                // This view might have changed size, so request a layout to ensure it is displayed correctly.
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    parent?.requestLayout()
                }

                // Override the WebView's default behavior to open links. Instead of loading the URL in the WebView,
                // launch the device's default browser to handle the URL.
                override fun shouldOverrideUrlLoading(view: WebView, webResourceRequest: WebResourceRequest): Boolean {
                    val intent = Intent(Intent.ACTION_VIEW, webResourceRequest.url).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    runCatching {
                        context.startActivity(intent)
                    }.onFailure {
                        Log.e("ArcGISMapsSDK", "Failed to open link: ${it.message}")
                    }
                    return true
                }
            }
            val completeHtml = "$header$headStyle<html>${content.trim()}</html>"
            loadDataWithBaseURL(null, completeHtml, "text/html", "UTF-8", null)
        }
    })
}

/**
 * A composable that displays a TextPopupElement.
 *
 * @since 200.5.0
 */
@Composable
public fun TextPopupElement(popupElement: TextPopupElement) {
    HTML(content = popupElement.text)
}

@Preview
@Composable
internal fun TextPopupElementPreview() {
    val tempText =
        "<p><span style='color:#287fb8;font-family:Verdana;font-size:14px;'><strong>{NAME}</strong></span><span style='font-family:Verdana;font-size:14px;'> is a peak in California's {RANGE} range. It ranks </span><span style='color:#aa3427;font-family:Verdana;font-size:14px;'><strong>#{RANK}</strong></span><span style='font-family:Verdana;font-size:14px;'> among the California Fourteeners.</span></p><p><span style='font-family:Verdana;font-size:14px;'>The summit is </span><span style='color:#287fb8;font-family:Verdana;font-size:14px;'><strong>{ELEV_FEET}</strong></span><span style='font-family:Verdana;font-size:14px;'> feet high ({ELEV_METERS} meters) and has a prominence of </span><span style='color:#287fb8;font-family:Verdana;font-size:14px;'><strong>{PROM_FEET}</strong></span><span style='font-family:Verdana;font-size:14px;'> feet ({PROM_METERS} meters).</span></p><p><a href='https://arcgis.com' rel='nofollow ugc'><span style='font-family:Verdana;font-size:14px;'>More info</span></a></p>"
    val popupElement = TextPopupElement(tempText)
    ExpandableCard {
        TextPopupElement(popupElement)
    }
}

