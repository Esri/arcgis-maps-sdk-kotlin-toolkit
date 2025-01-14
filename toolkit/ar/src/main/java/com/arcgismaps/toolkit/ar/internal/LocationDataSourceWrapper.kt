package com.arcgismaps.toolkit.ar.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.location.LocationDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class LocationDataSourceWrapper(val locationDataSource: LocationDataSource, val onStartResult: (Result<Unit>) -> Unit) : DefaultLifecycleObserver {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onDestroy(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        scope.launch {
            locationDataSource.stop()
        }
    }

    fun startLocationDataSource() {
        scope.launch {
            val result = locationDataSource.start()
            onStartResult(result)
        }
    }
}
