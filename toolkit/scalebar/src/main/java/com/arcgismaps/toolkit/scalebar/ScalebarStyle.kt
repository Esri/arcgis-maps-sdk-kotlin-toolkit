/*
 *
 *  Copyright 2025 Esri
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
package com.arcgismaps.toolkit.scalebar

/**
 * Defines the visual appearance of the Scalebar.
 *
 * @since 200.7.0
 */
public enum class ScalebarStyle {
    /**
     * Displays a single unit with segmented bars of alternating fill color.
     *
     * @since 200.7.0
     */
    AlternatingBar,

    /**
     * Displays a single unit.
     *
     * @since 200.7.0
     */
    Bar,

    /**
     * Displays both metric and imperial units. The primary unit is displayed on top.
     *
     * @since 200.7.0
     */
    DualUnitLine,

    /**
     * Displays a single unit with tick marks.
     *
     * @since 200.7.0
     */
    GraduatedLine,

    /**
     * Displays a single unit with endpoint tick marks.
     *
     * @since 200.7.0
     */
    Line
}
