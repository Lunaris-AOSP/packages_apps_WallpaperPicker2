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

package com.android.wallpaper.util.converter.category

import android.content.Context
import android.util.Log
import com.android.wallpaper.model.Category
import com.android.wallpaper.model.ImageCategory
import com.android.wallpaper.model.ThirdPartyAppCategory
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CollectionCategoryData
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.picker.data.category.ImageCategoryData
import com.android.wallpaper.picker.data.category.ThirdPartyCategoryData
import com.android.wallpaper.util.converter.WallpaperModelFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** This class creates an instance of [CategoryModel] from an instance of [Category] object. */
@Singleton
class DefaultCategoryFactory
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperModelFactory: WallpaperModelFactory,
) : CategoryFactory {

    override fun getCategoryModel(category: Category): CategoryModel {
        return CategoryModel(
            commonCategoryData = getCommonCategoryData(category),
            collectionCategoryData = (category as? WallpaperCategory)?.getCollectionsCategoryData(),
            imageCategoryData = getImageCategoryData(category),
            thirdPartyCategoryData = getThirdPartyCategoryData(category),
        )
    }

    private fun getCommonCategoryData(category: Category): CommonCategoryData {
        return CommonCategoryData(
            title = category.title,
            collectionId = category.collectionId,
            priority = category.priority,
        )
    }

    private fun WallpaperCategory.getCollectionsCategoryData(): CollectionCategoryData? {
        if (wallpapers == null) {
            return null
        }
        val wallpaperModelList =
            wallpapers
                .map { wallpaperInfo ->
                    wallpaperModelFactory.getWallpaperModel(context, wallpaperInfo)
                }
                .toMutableList()
        return CollectionCategoryData(
            wallpaperModels = wallpaperModelList,
            thumbAsset = getThumbnail(context),
            featuredThumbnailIndex = featuredThumbnailIndex,
            isSingleWallpaperCategory = isSingleWallpaperCategory,
        )
    }

    private fun getImageCategoryData(category: Category): ImageCategoryData? {
        return if (category is ImageCategory) {
            ImageCategoryData(
                thumbnailAsset = category.getThumbnail(context),
                defaultDrawable = category.getOverlayIcon(context),
            )
        } else {
            Log.w(TAG, "Passed category is not of type ImageCategory")
            null
        }
    }

    private fun getThirdPartyCategoryData(category: Category): ThirdPartyCategoryData? {
        return if (category is ThirdPartyAppCategory) {
            ThirdPartyCategoryData(
                resolveInfo = category.resolveInfo,
                defaultDrawable = category.getOverlayIcon(context),
            )
        } else {
            Log.w(TAG, "Passed category is not of type ThirdPartyAppCategory")
            null
        }
    }

    companion object {
        private const val TAG = "DefaultCategoryFactory"
    }
}
