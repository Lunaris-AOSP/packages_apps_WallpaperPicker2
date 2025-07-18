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

package com.android.wallpaper.picker.category.ui.view.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.picker.category.ui.viewmodel.CategoriesViewModel
import com.android.wallpaper.picker.category.ui.viewmodel.SectionViewModel

/**
 * This class adds the appropriate padding to a category based on the number of columns it occupies
 */
class CategoriesGridPaddingDecoration(
    val sectionViewModelList: List<SectionViewModel>,
    val padding: Int,
    val columnCalculator: (Int) -> Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {

        outRect.left = padding
        outRect.right = padding
        val position = parent.getChildAdapterPosition(view)

        // This needs to be done since the carousel view we are using for CuratedPhotos does not
        // support adding margins at the individual image level. If we do not do this, every photo
        // will have a margin which we do not want.
        if (
            sectionViewModelList[position].displayType == CategoriesViewModel.DisplayType.Carousel
        ) {
            outRect.left -= NEGATIVE_HORIZONTAL_PADDING
            outRect.right -= NEGATIVE_HORIZONTAL_PADDING
        }

        val columnCount = columnCalculator(position)
        if (columnCount > 1) {
            outRect.bottom =
                parent.context.resources.getDimensionPixelSize(
                    R.dimen.grid_item_featured_category_padding_bottom
                )
        } else {
            outRect.bottom =
                parent.context.resources.getDimensionPixelSize(
                    R.dimen.grid_item_category_padding_bottom
                )
        }
    }

    companion object {
        const val NEGATIVE_HORIZONTAL_PADDING = 4
    }
}
