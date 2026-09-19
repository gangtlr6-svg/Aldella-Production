package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AldellaViewModel
import com.example.ui.screens.*
import com.example.ui.theme.AldellaNavyDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AldellaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AldellaNavyDark
                ) {
                    AldellaAppNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AldellaAppNavHost(viewModel: AldellaViewModel) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    if (!isUserLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { }
        )
    } else {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("live_overview") {
            LiveOverviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("attendance") {
            AttendanceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("complaint") {
            ComplaintDowntimeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("production_operator") {
            ProductionOperatorScreen(
                viewModel = viewModel,
                onSelectArea = { areaName ->
                    val encoded = Uri.encode(areaName)
                    navController.navigate("area_processes/$encoded")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "area_processes/{areaName}",
            arguments = listOf(navArgument("areaName") { type = NavType.StringType })
        ) { backStackEntry ->
            val areaName = Uri.decode(backStackEntry.arguments?.getString("areaName") ?: "Defrosting")
            AreaProcessesScreen(
                areaName = areaName,
                viewModel = viewModel,
                onOpenSheet = { processName ->
                    val encArea = Uri.encode(areaName)
                    val encProcess = Uri.encode(processName)
                    navController.navigate("process_sheet/$encArea/$encProcess")
                },
                onOpenUpdates = { processName ->
                    val encArea = Uri.encode(areaName)
                    val encProcess = Uri.encode(processName)
                    navController.navigate("process_updates/$encArea/$encProcess")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "process_sheet/{areaName}/{processName}",
            arguments = listOf(
                navArgument("areaName") { type = NavType.StringType },
                navArgument("processName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val areaName = Uri.decode(backStackEntry.arguments?.getString("areaName") ?: "")
            val processName = Uri.decode(backStackEntry.arguments?.getString("processName") ?: "")
            ProcessSheetScreen(
                areaName = areaName,
                processName = processName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "process_updates/{areaName}/{processName}",
            arguments = listOf(
                navArgument("areaName") { type = NavType.StringType },
                navArgument("processName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val areaName = Uri.decode(backStackEntry.arguments?.getString("areaName") ?: "")
            val processName = Uri.decode(backStackEntry.arguments?.getString("processName") ?: "")
            ProcessUpdatesScreen(
                areaName = areaName,
                processName = processName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("team_chat") {
            TeamChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("admin_panel") {
            AdminControlPanelScreen(
                viewModel = viewModel,
                onNavigateToLiveOverview = { navController.navigate("live_overview") },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
}
