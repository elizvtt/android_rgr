package com.example.yourlibrary_palazova

import com.example.yourlibrary_palazova.helpers.SwipeHelper

fun List<SwipeHelper.UnderlayButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return map { it.intrinsicWidth }.reduce { acc, fl -> acc + fl }
}