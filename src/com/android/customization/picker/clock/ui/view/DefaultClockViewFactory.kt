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

package com.android.customization.picker.clock.ui.view

import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.systemui.plugins.clocks.ClockController
import javax.inject.Inject

class DefaultClockViewFactory @Inject constructor() : ClockViewFactory {

    override fun getController(clockId: String): ClockController {
        TODO("Not yet implemented")
    }

    override fun getLargeView(clockId: String): View {
        TODO("Not yet implemented")
    }

    override fun getSmallView(clockId: String): View {
        TODO("Not yet implemented")
    }

    override fun updateColorForAllClocks(seedColor: Int?) {
        TODO("Not yet implemented")
    }

    override fun updateColor(clockId: String, seedColor: Int?) {
        TODO("Not yet implemented")
    }

    override fun updateFontAxes(clockId: String, settings: ClockAxisStyle) {
        TODO("Not yet implemented")
    }

    override fun updateRegionDarkness() {
        TODO("Not yet implemented")
    }

    override fun updateTimeFormat(clockId: String) {
        TODO("Not yet implemented")
    }

    override fun registerTimeTicker(owner: LifecycleOwner) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun unregisterTimeTicker(owner: LifecycleOwner) {
        TODO("Not yet implemented")
    }
}
