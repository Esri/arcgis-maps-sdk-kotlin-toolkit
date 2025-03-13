package com.arcgismaps.toolkit.ar

public sealed class WorldScaleTrackingMode() {
    public data object Geospatial : WorldScaleTrackingMode()
    public data object World : WorldScaleTrackingMode()
}