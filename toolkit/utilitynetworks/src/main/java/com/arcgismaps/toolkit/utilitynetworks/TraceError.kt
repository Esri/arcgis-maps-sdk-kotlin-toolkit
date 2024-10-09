/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks

/**
 * Enum class to represent the different error messages that can be thrown by the Trace tool.
 *
 * @since 200.6.0
 */
internal enum class TraceError(val messageId: Int) {
    NO_UTILITY_NETWORK_FOUND(R.string.no_utility_network_found),
    NO_TRACE_CONFIGURATIONS_FOUND(R.string.no_trace_configurations_found),
    NOT_ENOUGH_STARTING_POINTS_ONE(R.string.not_enough_starting_points_please_set_at_least_1_starting_location),
    NOT_ENOUGH_STARTING_POINTS_TWO(R.string.not_enough_starting_points_please_set_at_least_2_starting_locations),
    COULD_NOT_CREATE_UTILITY_ELEMENT(R.string.could_not_create_utility_element_from_arcgisfeature),
    STARTING_POINT_ALREADY_EXISTS(R.string.one_or_more_starting_points_already_exists),
    COULD_NOT_CREATE_DRAWABLE(R.string.could_not_create_drawable_from_feature_symbol)
}

/**
 * Exception class to represent the different errors that can be thrown by the Trace tool.
 *
 * @param errorId The error id of the error thrown, used for localization.
 * @since 200.6.0
 */
internal class TraceToolException(val errorId: Int): Exception()
