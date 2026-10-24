package com.flightfocus.app.data.airport

import android.content.Context
import com.flightfocus.app.domain.model.Airport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 앱에 내장한 주요 공항 목록(assets/airports.json). */
@Singleton
class AirportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val airports: List<Airport> by lazy {
        context.assets.open(ASSET_PATH).bufferedReader().use { json.decodeFromString(it.readText()) }
    }

    private val byIata: Map<String, Airport> by lazy { airports.associateBy { it.iata } }

    fun find(iata: String): Airport? = byIata[iata]

    val home: Airport get() = byIata.getValue(HOME_IATA)

    companion object {
        private const val ASSET_PATH = "airports.json"

        /** 첫 비행의 출발 공항. 이후에는 마지막으로 도착한 공항에서 출발한다. */
        const val HOME_IATA = "GMP"
    }
}
