/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.system

import android.app.UiModeManager.ContrastChangeListener
import java.util.concurrent.Executor

interface UiModeManagerWrapper {

    fun addContrastChangeListener(executor: Executor, listener: ContrastChangeListener)

    fun removeContrastChangeListener(listener: ContrastChangeListener)

    fun getContrast(): Float?

    fun getIsNightModeActivated(): Boolean

    /**
     * Activating night mode for the current user
     *
     * @return if the change is successful
     */
    fun setNightModeActivated(isActive: Boolean): Boolean
}
