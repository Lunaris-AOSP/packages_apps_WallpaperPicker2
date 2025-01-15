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

package com.android.wallpaper.picker.customization.data.repository

import com.android.systemui.shared.customization.data.SensorLocation
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomizationRuntimeValuesRepository
@Inject
constructor(private val client: CustomizationProviderClient) {

    suspend fun getIsShadeLayoutWide(): Boolean {
        return client
            .queryRuntimeValues()
            .getBoolean(
                CustomizationProviderContract.RuntimeValuesTable.KEY_IS_SHADE_LAYOUT_WIDE,
                false,
            )
    }

    suspend fun getUdfpsLocation(): SensorLocation? {
        return client
            .queryRuntimeValues()
            .getString(CustomizationProviderContract.RuntimeValuesTable.KEY_UDFPS_LOCATION)
            ?.let { contentProviderString -> SensorLocation.decode(contentProviderString) }
    }
}
