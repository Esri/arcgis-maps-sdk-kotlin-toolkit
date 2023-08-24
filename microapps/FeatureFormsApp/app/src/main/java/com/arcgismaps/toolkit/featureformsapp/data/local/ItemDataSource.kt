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

package com.arcgismaps.toolkit.featureformsapp.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The data source for portal data
 */
class ItemDataSource(
    private val dispatcher: CoroutineDispatcher, private val itemApi: ItemApi = object : ItemApi {
        override suspend fun fetchItems(): List<ItemData> =
            getListOfMaps()
                .map {
                    ItemData(it)
                }
    }
) {
    suspend fun fetchItemData(): List<ItemData> = withContext(dispatcher) {
        itemApi.fetchItems()
    }
}

///**
// * Data source of a list of portal urls
// */
//fun getListOfMaps(): List<String> {
//    return listOf(
//        //"https://runtimecoretest.maps.arcgis.com/home/item.html?id=d4884dcb5e4c4832b51e4129735cc9e6",
//        "https://www.arcgis.com/home/item.html?id=0c4b6b70a56b40b08c5b0420c570a6ac",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=f4cdb74cb4164d68b6b48ca2d4d02dba",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=ec09090060664cbda8d814e017337837",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=0f6864ddc35241649e5ad2ee61a3abe4",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=df0f27f83eee41b0afe4b6216f80b541",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=454422bdf7e24fb0ba4ffe0a22f6bf37",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=c606b1f345044d71881f99d00583f8f7",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=622c4674d6f64114a1de2e0b8382fcf3",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=a81d90609e4549479d1f214f28335af2",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=bb4c5e81740e4e7296943988c78a7ea6",
////        "https://runtimecoretest.maps.arcgis.com/home/item.html?id=5d69e2301ad14ec8a73b568dfc29450a",
//    )
//}
