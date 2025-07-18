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

package com.android.wallpaper.picker.category.domain.interactor.implementations

import com.android.wallpaper.picker.category.data.repository.WallpaperCategoryRepository
import com.android.wallpaper.picker.category.domain.interactor.CategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/** This class implements the business logic in assembling ungrouped category models */
@Singleton
class CategoryInteractorImpl
@Inject
constructor(
    private val defaultWallpaperCategoryRepository: WallpaperCategoryRepository,
    @BackgroundDispatcher private val backgroundScope: CoroutineScope,
) : CategoryInteractor {

    override val categories: Flow<List<CategoryModel>> =
        defaultWallpaperCategoryRepository.isDefaultCategoriesFetched
            .filter { it }
            .flatMapLatest {
                combine(
                    defaultWallpaperCategoryRepository.onDeviceCategory,
                    defaultWallpaperCategoryRepository.systemCategories,
                    defaultWallpaperCategoryRepository.thirdPartyLiveWallpaperCategory,
                ) { onDeviceCategory, systemCategories, thirdPartyLiveWallpaperCategory ->
                    val combinedList = (systemCategories + thirdPartyLiveWallpaperCategory)
                    val finalList = onDeviceCategory?.let { combinedList + it } ?: combinedList
                    // Sort the categories based on their priority value
                    finalList.sortedBy { it.commonCategoryData.priority }
                }
            }

    override val isNetworkCategoriesNotEmpty: Flow<Boolean> = emptyFlow()

    override fun refreshNetworkCategories() {}

    override fun refreshThirdPartyLiveWallpaperCategories() {
        backgroundScope.launch {
            defaultWallpaperCategoryRepository.refreshThirdPartyLiveWallpaperCategories()
        }
    }
}
