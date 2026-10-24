package com.flightfocus.app

import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flightfocus.app.flight.FlightController
import com.flightfocus.app.ui.FlightFocusNavHost
import com.flightfocus.app.ui.theme.FlightFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var flightController: FlightController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 항상 어두운 테마라 시스템 바 아이콘은 밝게 고정
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            FlightFocusTheme {
                FlightFocusNavHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        flightController.onReturnedToApp()
    }

    /**
     * 임시 Focus Lock: 화면이 켜진 채 앱이 가려지면 다른 앱으로 이탈한 것으로 본다.
     * 화면 끄기는 공부 중 흔한 동작이라 이탈로 치지 않는다.
     * TODO(5주차): UsageStatsManager로 실제 전면 앱을 확인하고 허용 앱 목록 적용.
     */
    override fun onStop() {
        super.onStop()
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (screenOn && !isChangingConfigurations) flightController.onLeftApp()
    }
}
