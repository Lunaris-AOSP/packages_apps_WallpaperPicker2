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

import android.os.Bundle
import com.android.systemui.shared.customization.data.SensorLocation
import com.android.systemui.shared.customization.data.content.CustomizationProviderContract
import com.android.systemui.shared.customization.data.content.FakeCustomizationProviderClient
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CustomizationRuntimeValuesRepositoryTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var testScope: TestScope

    private val customizationProviderClient = FakeCustomizationProviderClient()
    lateinit var underTest: CustomizationRuntimeValuesRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        underTest = CustomizationRuntimeValuesRepository(customizationProviderClient)
    }

    @Test
    fun getIsShadeLayoutWide() =
        testScope.runTest {
            customizationProviderClient.setRuntimeValues(
                Bundle().apply {
                    putBoolean(
                        CustomizationProviderContract.RuntimeValuesTable.KEY_IS_SHADE_LAYOUT_WIDE,
                        true,
                    )
                }
            )

            assertThat(underTest.getIsShadeLayoutWide()).isTrue()
        }

    @Test
    fun udfpsLocationUpdatesWhenClientUpdates() =
        testScope.runTest {
            val udfpsLocation = SensorLocation(640, 2068, 117, 0.75f)
            customizationProviderClient.setRuntimeValues(
                Bundle().apply {
                    putString(
                        CustomizationProviderContract.RuntimeValuesTable.KEY_UDFPS_LOCATION,
                        udfpsLocation.encode(),
                    )
                }
            )

            assertThat(underTest.getUdfpsLocation()).isEqualTo(udfpsLocation)
        }
}
