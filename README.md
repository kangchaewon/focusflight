# ✈️ Flight Focus

> 공부 시간 = 비행 시간. 공부한 만큼 노선을 날고, 마일리지로 기종을 늘려가는 Android 집중 타이머.

## 현재 기능 (MVP 2주차)

- **정기편 / 부정기편:** 노선을 고르면 실제 공항 간 거리로 목표 시간이 정해지는 카운트다운, 또는 기종 연료 한계까지 카운트업
- **착륙 정산:** 1분 = 10마일, 정시 도착 +20%, 회항 시 절반, 하루 14시간 상한
- **난기류 / 회항 (임시 Focus Lock):** 화면이 켜진 채 앱을 떠나면 난기류, 20초 안에 돌아오지 않으면 회항
- **격납고:** 마일리지로 경비행기 → 리저널 제트 → 협동체 → 광동체 순서로 해금
- **비행 일지:** 누적 비행 시간·거리·회항 수, 비행 기록
- 마지막으로 도착한 공항에서 다음 비행이 출발
- **백그라운드 비행:** Foreground Service + 상단 알림으로 화면을 꺼도 비행 유지, 남은 시간은 알림 크로노미터로 표시
- **강제 종료 복구:** 진행 중 비행을 Room에 저장해 두고, 앱을 다시 열면 조종석으로 돌아와 이어서 비행
- **알림:** 앱을 떠나면 20초 카운트다운 난기류 경고, 화면을 보고 있지 않을 때 착륙·회항 결과 알림

## 기술 스택

Kotlin · Jetpack Compose (Material 3) · Hilt · Room · Coroutines/Flow · Navigation (type-safe) · kotlinx.serialization · JUnit + Turbine · GitHub Actions

## 구조

```
app/src/main/java/com/flightfocus/app/
├─ domain/        # 순수 Kotlin. Android 의존성 없음 → 단위 테스트 집중
│  ├─ model/      # Airport, AircraftTier, FlightPlan, ActiveFlight, FlightResult
│  ├─ flight/     # FlightStateMachine (이륙·난기류·회항·착륙), FlightRoutes (대권거리)
│  └─ economy/    # MileageCalculator, HangarRules
├─ data/          # Room DB, Repository, 공항 데이터(assets/airports.json)
├─ flight/        # FlightController(비행 상태의 유일한 원천·저장·복원), FlightService(Foreground Service), 알림
├─ di/            # Hilt 모듈
└─ ui/            # home(노선 선택) · cockpit(비행 중) · hangar · logbook
```

설계 포인트

- 비행 시간은 tick을 세지 않고 **타임스탬프로 계산**한다. 앱이 잠들었다 깨어나도, 프로세스가 죽었다 복원돼도 상태 머신이 같은 결과를 낸다.
- 비행 상태는 `FlightController` 하나만 갖는다. `FlightService`는 그 상태를 지켜보며 알림을 그리고 프로세스를 살려 둘 뿐이다.
- 정산 기록과 진행 중 비행 삭제를 한 트랜잭션으로 묶어, 중간에 죽어도 같은 비행이 두 번 정산되지 않는다.

## 빌드

Android Studio(JDK 21 내장)와 SDK Platform 37이 필요하다.

> ⚠️ 프로젝트 경로에 작은따옴표(`'`)가 있으면 Gradle 단위 테스트가 `ClassNotFoundException`으로 실패한다. 빌드는 되지만 테스트를 돌리려면 따옴표 없는 경로에 두어야 한다.

```bash
./gradlew :app:testDebugUnitTest   # 단위 테스트
./gradlew :app:assembleDebug       # 디버그 APK
```

## 다음 단계

- [x] 2주차: Foreground Service로 타이머 이전 + 진행 중 비행 Room 저장/복구
- [ ] 배터리 최적화 예외 안내 화면 (삼성 기기 대응)
- [ ] 5주차: UsageStatsManager 기반 Focus Lock, 허용 앱 목록
- [ ] 노선 지도, 탑승권 공유, 위젯
- [x] 앱 아이콘 (임시) / [ ] 앱 이름 확정 후 applicationId 변경
