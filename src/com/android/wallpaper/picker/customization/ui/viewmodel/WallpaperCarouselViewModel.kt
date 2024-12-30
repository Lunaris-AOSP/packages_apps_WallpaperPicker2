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

package com.android.wallpaper.picker.customization.ui.viewmodel

import android.content.Context
import com.android.wallpaper.asset.ContentUriAsset
import com.android.wallpaper.picker.category.domain.interactor.CuratedPhotosInteractor
import com.android.wallpaper.picker.category.ui.view.SectionCardinality
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.category.ui.viewmodel.TileViewModel
import com.android.wallpaper.picker.data.WallpaperModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WallpaperCarouselViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    curatedPhotosInteractor: CuratedPhotosInteractor,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    private val _navigationEvents = MutableSharedFlow<CategoriesViewModel.NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    val wallpaperCarouselItems: Flow<List<TileViewModel>> =
        curatedPhotosInteractor.category.distinctUntilChanged().map { category ->
            category.collectionCategoryData?.wallpaperModels?.map { wallpaperModel ->
                val staticWallpaperModel = wallpaperModel as? WallpaperModel.StaticWallpaperModel
                TileViewModel(
                    defaultDrawable = null,
                    thumbnailAsset =
                        ContentUriAsset(context, staticWallpaperModel?.imageWallpaperData?.uri),
                    text = category.commonCategoryData.title,
                    maxCategoriesInRow = SectionCardinality.Single,
                ) {
                    navigateToPreviewScreen(wallpaperModel)
                }
            } ?: emptyList()
        }

    private fun navigateToPreviewScreen(wallpaperModel: WallpaperModel) {
        viewModelScope.launch {
            _navigationEvents.emit(
                CategoriesViewModel.NavigationEvent.NavigateToPreviewScreen(
                    wallpaperModel,
                    CategoriesViewModel.CategoryType.MyPhotosCategories,
                )
            )
        }
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): WallpaperCarouselViewModel
    }
}
