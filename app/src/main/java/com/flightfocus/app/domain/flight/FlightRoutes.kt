package com.flightfocus.app.domain.flight

import com.flightfocus.app.domain.model.Airport
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object FlightRoutes {
    private const val EARTH_RADIUS_KM = 6371.0

    /** 가상 순항 속도. 1분 공부 ≈ 13.3km 비행. */
    const val CRUISE_SPEED_KMH = 800.0

    /** 이착륙에 더하는 고정 시간. */
    const val TAXI_MINUTES = 5

    /** 두 공항 사이 대권거리(haversine). */
    fun distanceKm(from: Airport, to: Airport): Double {
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    /** 노선의 목표 비행(공부) 시간(분). */
    fun flightMinutes(from: Airport, to: Airport): Int =
        ceil(distanceKm(from, to) / CRUISE_SPEED_KMH * 60).toInt() + TAXI_MINUTES

    /** 공부한 시간을 비행 거리로 환산. 부정기편 기록에 쓴다. */
    fun distanceForMinutes(minutes: Int): Double = CRUISE_SPEED_KMH * minutes / 60
}
