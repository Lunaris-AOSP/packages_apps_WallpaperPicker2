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

package com.android.wallpaper.picker.preview.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.children

/** A [MotionLayout] that passes only clicks to one of its children if it is the recipient. */
class ClickableMotionLayout(context: Context, attrs: AttributeSet?) : MotionLayout(context, attrs) {
    private val singleTapDetector =
        GestureDetector(
            context,
            object : SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    return true
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return children.find { child ->
                        e.x >= child.left &&
                            e.x <= child.right &&
                            e.y >= child.top &&
                            e.y <= child.bottom
                    } == null
                }
            },
        )

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return singleTapDetector.onTouchEvent(event)
    }
}
