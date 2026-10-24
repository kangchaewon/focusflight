package com.flightfocus.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flightfocus.app.domain.flight.FlightState
import com.flightfocus.app.ui.cockpit.CockpitScreen
import com.flightfocus.app.ui.hangar.HangarScreen
import com.flightfocus.app.ui.home.HomeScreen
import com.flightfocus.app.ui.logbook.LogbookScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object HangarRoute
@Serializable data object LogbookRoute
@Serializable data object CockpitRoute

private data class TopLevelTab(val route: Any, val label: String, val icon: String)

private val tabs = listOf(
    TopLevelTab(HomeRoute, "출발", "🛫"),
    TopLevelTab(HangarRoute, "격납고", "🛩️"),
    TopLevelTab(LogbookRoute, "비행 일지", "📋"),
)

@Composable
fun FlightFocusNavHost(flightState: FlightState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val inCockpit = destination?.hasRoute(CockpitRoute::class) == true

    // 앱을 다시 열었을 때 복원된 비행이 있으면 조종석으로 보낸다
    val airborne = flightState is FlightState.Airborne
    LaunchedEffect(airborne, destination) {
        if (airborne && destination != null && !inCockpit) {
            navController.navigate(CockpitRoute) { launchSingleTop = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (!inCockpit) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.route::class) == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(tab.icon) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> {
                HomeScreen(onTakeOff = { navController.navigate(CockpitRoute) })
            }
            composable<HangarRoute> { HangarScreen() }
            composable<LogbookRoute> { LogbookScreen() }
            composable<CockpitRoute> {
                CockpitScreen(onFinished = { navController.popBackStack(HomeRoute, inclusive = false) })
            }
        }
    }
}
