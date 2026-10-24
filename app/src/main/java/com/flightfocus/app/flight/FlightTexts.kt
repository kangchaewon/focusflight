package com.flightfocus.app.flight

import com.flightfocus.app.domain.model.FlightMode
import com.flightfocus.app.domain.model.FlightOutcome
import com.flightfocus.app.domain.model.FlightPlan
import com.flightfocus.app.domain.model.FlightResult

/** 조종석 화면과 알림이 같은 문구를 쓰도록 모아 둔다. */

fun FlightPlan.routeText(): String =
    destination?.let { "${origin.iata} → ${it.iata}" } ?: "${origin.iata} 출발"

/** 착륙 결과의 (제목, 설명). */
fun FlightResult.headline(): Pair<String, String> = when (outcome) {
    FlightOutcome.COMPLETED ->
        if (plan.mode == FlightMode.SCHEDULED) "✅ 정시 도착" to "${plan.destination?.nameKo}에 도착했습니다"
        else "🛬 착륙 완료" to "부정기편 비행을 마쳤습니다"
    FlightOutcome.LANDED_EARLY -> "🛬 조기 착륙" to "${plan.origin.nameKo}(으)로 돌아왔습니다"
    FlightOutcome.DIVERTED -> "🌪️ 비상 회항" to "난기류로 ${plan.origin.nameKo}(으)로 회항했습니다"
}
