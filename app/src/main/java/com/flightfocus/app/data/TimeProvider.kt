package com.flightfocus.app.data

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

interface TimeProvider {
    fun nowMillis(): Long

    /** 오늘(기기 시간대) 0시 ~ 내일 0시. */
    fun todayRangeMillis(): LongRange
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun todayRangeMillis(): LongRange {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }
}
