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

package com.android.wallpaper.wrapper

import android.content.Context
import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.category.wrapper.DefaultWallpaperCategoryWrapper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
class DefaultWallpaperCategoryWrapperTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context

    @Inject lateinit var fakeDefaultWallpaperCategoryRepository: WallpaperCategoryRepository

    @Inject lateinit var defaultWallpaperCategoryWrapper: DefaultWallpaperCategoryWrapper

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testGetCategories() = runTest {
        val categories = defaultWallpaperCategoryWrapper.getCategories(false)
        assertThat(categories).isNotEmpty()
        assertThat(categories.size).isEqualTo(1)
    }
}
