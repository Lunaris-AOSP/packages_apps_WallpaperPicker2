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

import com.android.wallpaper.picker.category.domain.interactor.CreativeCategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** This class implements the business logic in assembling creative category models */
@Singleton
class CreativeCategoryInteractorImpl @Inject constructor() : CreativeCategoryInteractor {
    // default implementation of creatives is empty in aosp
    override val categories: Flow<List<CategoryModel>> = flowOf(emptyList())

    override val standaloneCategories: Flow<List<CategoryModel>> = flowOf(emptyList())

    override fun updateCreativeCategories() {
        // nothing to update in aosp
    }
}
