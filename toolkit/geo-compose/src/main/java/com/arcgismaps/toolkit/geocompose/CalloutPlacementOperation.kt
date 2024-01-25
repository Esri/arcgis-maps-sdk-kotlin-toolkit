package com.arcgismaps.toolkit.geocompose

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.compose.runtime.Stable
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.mapping.view.zero
import kotlinx.coroutines.CompletableDeferred

/*
 * COPYRIGHT 1995-2024 ESRI
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

/**
 *
 * @since 200.4.0
 */
@Stable
public sealed class CalloutPlacementOperation {

    private val deferred = CompletableDeferred<Result<Boolean>>()

    /**
     * Awaits the completion of this MapViewpointOperation.
     *
     * @return a Result returning a boolean used to indicate if the operation completed successfully or not
     * @since 200.4.0
     */
    public suspend fun await(): Result<Boolean> = deferred.await()

    internal fun complete(result: Result<Boolean>) {
        deferred.complete(result)
    }


    /**
     *
     * @since 200.4.0
     */
    public class ShowTextAtLocation(
        public val context: Context,
        public val content: String,
        public val location: Point,
        public val offset: DoubleXY = DoubleXY.zero,
        public val rotateOffsetWithGeoView: Boolean = false,
        public val animated: Boolean = false
    ) : CalloutPlacementOperation()

    /**
     *
     * @since 200.4.0
     */
    public class ShowAtLocation(
        public val contentView: View,
        public val location: Point,
        public val offset: DoubleXY = DoubleXY.zero,
        public val rotateOffsetWithGeoView: Boolean = false,
        public val animated: Boolean = false
    ) : CalloutPlacementOperation()

    /**
     *
     * @since 200.4.0
     */
    public class ShowAtGeoElement(
        public val contentView: View,
        public val geoElement: GeoElement,
        public val tapLocation: Point? = null,
        public val animated: Boolean = false
    ) : CalloutPlacementOperation()

    public class Dismiss(public val animated: Boolean = false) : CalloutPlacementOperation()
}


internal fun CalloutPlacementOperation.execute(geoView: com.arcgismaps.mapping.view.GeoView) {
    when (this) {
        is CalloutPlacementOperation.ShowTextAtLocation -> {
            val callout = geoView.callout
            callout.isAnimationEnabled = animated
            val text = TextView(context).apply { text = content }
            callout.show(text, location, offset, rotateOffsetWithGeoView)
            this.complete(Result.success(true))
        }
        is CalloutPlacementOperation.ShowAtLocation -> {
            val callout = geoView.callout
            callout.isAnimationEnabled = animated
            callout.show(contentView, location, offset, rotateOffsetWithGeoView)
            this.complete(Result.success(true))
        }
        is CalloutPlacementOperation.ShowAtGeoElement -> {
            val callout = geoView.callout
            callout.isAnimationEnabled = animated
            callout.show(contentView, geoElement, tapLocation)
            this.complete(Result.success(true))
        }
        is CalloutPlacementOperation.Dismiss -> {
            val callout = geoView.callout
            callout.dismiss()
            this.complete(Result.success(true))
        }
    }
}


