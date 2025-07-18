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

import android.platform.test.flag.junit.SetFlagsRule
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.testing.TestInjector
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class WallpaperColorsRepositoryTest {
    @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    @Inject lateinit var testInjector: TestInjector
    lateinit var repository: WallpaperColorsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        InjectorProvider.setInjector(testInjector)
        repository = WallpaperColorsRepository()
    }

    @Test
    fun initialState() {
        assertThat(repository.homeWallpaperColors.value)
            .isInstanceOf(WallpaperColorsModel.Loading::class.java)
    }
}
