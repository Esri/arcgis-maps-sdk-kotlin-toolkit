/*
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

package com.arcgismaps.toolkit.geocompose

import android.os.Parcel
import android.os.Parcelable

/**
 * Enum class representing the different types of viewpoint persistence on a composable [MapView].
 *
 * Viewpoint persistence determines how the viewpoint of a MapView is saved and restored across activity
 * or process recreation, for example, when the device is rotated or when the app is sent to the background
 * and then brought back to the foreground.
 *
 * Note that a [MutableState] of [ViewpointPersistence] can not be used with [remember], because it will
 * not be able to restore the state across process recreation. Instead, use [MutableState] of [Viewpoint]
 * inside of [rememberSaveable] or within a [ViewModel]. Note that this class implements [Parcelable] so it
 * can be used with [rememberSaveable] without any need for a custom [Saver].
 *
 * @since 200.4.0
 */
public sealed class ViewpointPersistence : Parcelable {

    /**
     * The viewpoint is not persisted.
     *
     * @since 200.4.0
     */
    public object None : ViewpointPersistence() {
        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // No state to write for this object
        }

        @JvmField
        public val CREATOR: Parcelable.Creator<None?> = object : Parcelable.Creator<None?> {
            override fun createFromParcel(source: Parcel): None? = None
            override fun newArray(size: Int): Array<None?> = arrayOfNulls(size)
        }
    }

    /**
     * The viewpoint is persisted by its center and scale.
     *
     * @since 200.4.0
     */
    public class ByCenterAndScale : ViewpointPersistence() {

        // Note: ByCenterAndScale and ByBoundingGeometry could have been defined as singletons (object) but we
        // want to keep the possibility open to add instance state (properties) to these classes in the future,
        // thus we had to declare them as classes. This meant we had to override hashCode and equals in order to
        // achieve the same equality behaviour as a singleton would do.
        override fun hashCode(): Int = 1
        override fun equals(other: Any?): Boolean = other is ByCenterAndScale

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // No state to write for this object
        }
        public companion object {
            @JvmField
            public val CREATOR: Parcelable.Creator<ByCenterAndScale?> =
                object : Parcelable.Creator<ByCenterAndScale?> {
                    override fun createFromParcel(source: Parcel): ByCenterAndScale? =
                        ByCenterAndScale()

                    override fun newArray(size: Int): Array<ByCenterAndScale?> = arrayOfNulls(size)
                }
        }
    }

    /**
     * The viewpoint is persisted by its bounding geometry.
     *
     * @since 200.4.0
     */
    public class ByBoundingGeometry : ViewpointPersistence() {

        override fun hashCode(): Int = 1
        override fun equals(other: Any?): Boolean = other is ByBoundingGeometry

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // No state to write for this object
        }
        public companion object {
            @JvmField
            public val CREATOR: Parcelable.Creator<ByBoundingGeometry?> =
                object : Parcelable.Creator<ByBoundingGeometry?> {
                    override fun createFromParcel(source: Parcel): ByBoundingGeometry? =
                        ByBoundingGeometry()

                    override fun newArray(size: Int): Array<ByBoundingGeometry?> =
                        arrayOfNulls(size)
                }
        }
    }
}
