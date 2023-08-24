package com.arcgismaps.toolkit.featureformsapp.data

import com.arcgismaps.toolkit.featureformsapp.data.local.PortalItemCacheEntry
import com.arcgismaps.toolkit.featureformsapp.data.local.PortalItemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

class PortalItemRepository(
    private val scope: CoroutineScope,
    private val portalItemDataSource: PortalItemDao
) {
    suspend fun createEntry(itemId: String, json : String, portalUrl : String) = withContext(scope.coroutineContext) {
        portalItemDataSource.upsert(PortalItemCacheEntry(itemId, json, portalUrl))
    }

    suspend fun getEntry(itemId: String): PortalItemCacheEntry? = withContext(scope.coroutineContext) {
        return@withContext portalItemDataSource.getById(itemId)
    }
}
