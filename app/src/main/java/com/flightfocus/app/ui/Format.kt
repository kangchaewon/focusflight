package com.flightfocus.app.ui

import java.text.NumberFormat
import java.util.Locale

fun Long.miles(): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(this) + " 마일"

/** 65 → "1시간 5분", 40 → "40분" */
fun Int.durationText(): String {
    val h = this / 60
    val m = this % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

/** 남은/경과 시간 표시. 3725초 → "1:02:05", 125초 → "02:05" */
fun Long.clockText(): String {
    val total = this.coerceAtLeast(0) / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
