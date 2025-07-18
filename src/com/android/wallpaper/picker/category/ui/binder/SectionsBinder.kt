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

package com.android.wallpaper.picker.category.ui.binder

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.view.adapter.CategorySectionsAdapter
import com.android.wallpaper.picker.category.ui.view.decoration.CategoriesGridPaddingDecoration
import com.android.wallpaper.picker.category.ui.viewmodel.SectionViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel

/** Binds the collection of SectionViewModel to a section */
object SectionsBinder {
    private const val DEFAULT_SPAN = 3

    fun bind(
        sectionsListView: RecyclerView,
        sectionsViewModelList:
            List<SectionViewModel>, // TODO: this should not be a list rather a simple view model
        windowWidth: Int,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        onSignInBannerDismissed: (dismissed: Boolean) -> Unit,
        bannerProvider: BannerProvider,
    ) {
        sectionsListView.adapter =
            CategorySectionsAdapter(
                sectionsViewModelList,
                windowWidth,
                colorUpdateViewModel,
                shouldAnimateColor,
                lifecycleOwner,
                onSignInBannerDismissed,
                bannerProvider,
            )
        val gridLayoutManager =
            GridLayoutManager(sectionsListView.context, DEFAULT_SPAN).apply {
                spanSizeLookup =
                    object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return sectionsViewModelList[position].columnCount
                        }
                    }
            }
        sectionsListView.layoutManager = gridLayoutManager
        sectionsListView.removeItemDecorations()
        sectionsListView.addItemDecoration(
            CategoriesGridPaddingDecoration(
                sectionsViewModelList,
                sectionsListView.context.resources.getDimensionPixelSize(
                    R.dimen.grid_item_category_padding_horizontal
                ),
            ) { position ->
                return@CategoriesGridPaddingDecoration sectionsViewModelList[position].columnCount
            }
        )
    }

    fun RecyclerView.removeItemDecorations() {
        while (itemDecorationCount > 0) {
            removeItemDecorationAt(0)
        }
    }
}
