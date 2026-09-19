/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.throneproj.thronem.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf

object LocalResultEventBus {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus?> =
        compositionLocalOf { null }

    /**
     * The current [ResultEventBus]
     */
    val current: ResultEventBus
        @Composable
        get() = requireNotNull(LocalResultEventBus.current) { "No ResultEventBus has been provided" }

    /**
     * Provides a [ResultEventBus] to the composition
     */
    infix fun provides(
        bus: ResultEventBus,
    ): ProvidedValue<ResultEventBus?> {
        return LocalResultEventBus.provides(bus)
    }
}