package com.flightfocus.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Airport(
    val iata: String,
    val nameKo: String,
    val nameEn: String,
    val countryKo: String,
    val lat: Double,
    val lng: Double,
)
