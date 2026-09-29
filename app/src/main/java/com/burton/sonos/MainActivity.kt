package com.burton.sonos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.sonos.ui.alarms.AlarmEditorScreen
import com.burton.sonos.ui.alarms.AlarmsScreen
import com.burton.sonos.ui.browse.BrowseScreen
import com.burton.sonos.ui.components.NowPlayingBar
import com.burton.sonos.ui.navigation.Routes
import com.burton.sonos.ui.room.RoomDetailScreen
import com.burton.sonos.ui.rooms.RoomsScreen
import com.burton.sonos.ui.rooms.RoomsViewModel
import com.burton.sonos.ui.search.SearchScreen
import com.burton.sonos.ui.sources.SourcesScreen
import com.burton.sonos.ui.theme.BurtonBlack
import com.burton.sonos.ui.theme.BurtonIvory
import com.burton.sonos.ui.theme.BurtonMute
import com.burton.sonos.ui.theme.BurtonSonosTheme
import com.burton.sonos.ui.theme.BurtonVoid
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var permitted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permitted = granted || Build.VERSION.SDK_INT < 33
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permitted = hasNetworkPermission()
        if (!permitted) requestNetworkPermission()
        setContent {
            BurtonSonosTheme {
                if (permitted) {
                    BurtonApp()
                } else {
                    PermissionGate(onRequest = ::requestNetworkPermission)
                }
            }
        }
    }

    private fun hasNetworkPermission(): Boolean {
        val permission = requiredPermission()
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < 33
    }

    private fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    private fun requestNetworkPermission() {
        permissionLauncher.launch(requiredPermission())
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Burton Sonos", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Spacer(Modifier.height(12.dp))
        Text(
            "Local speaker discovery uses Wi-Fi. Allow nearby devices so the app can find your system.",
            style = MaterialTheme.typography.bodyLarge,
            color = BurtonMute,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Allow discovery")
        }
    }
}

@Composable
private fun BurtonApp() {
    val navController = rememberNavController()
    val roomsViewModel: RoomsViewModel = hiltViewModel()
    val snapshot by roomsViewModel.state.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.ROOMS, Routes.SOURCES, Routes.SEARCH, Routes.ALARMS)
    val showChrome = route in tabs
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack),
        containerColor = BurtonBlack,
        bottomBar = {
            if (showChrome) {
                Column(
                    modifier = Modifier
                        .background(BurtonBlack)
                        .navigationBarsPadding(),
                ) {
                    NowPlayingBar(
                        snapshot = snapshot,
                        onToggle = roomsViewModel::toggle,
                        onOpen = {
                            snapshot.selectedGroupId?.let {
                                navController.navigate(Routes.room(it))
                            }
                        },
                    )
                    NavigationBar(containerColor = BurtonBlack, contentColor = BurtonIvory) {
                        NavigationBarItem(
                            selected = route == Routes.ROOMS,
                            onClick = { navController.navigate(Routes.ROOMS) { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = "System") },
                            label = { Text("System") },
                            colors = navColors(route == Routes.ROOMS),
                        )
                        NavigationBarItem(
                            selected = route == Routes.SOURCES,
                            onClick = { navController.navigate(Routes.SOURCES) { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Sources") },
                            label = { Text("Sources") },
                            colors = navColors(route == Routes.SOURCES),
                        )
                        NavigationBarItem(
                            selected = route == Routes.SEARCH,
                            onClick = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            colors = navColors(route == Routes.SEARCH),
                        )
                        NavigationBarItem(
                            selected = route == Routes.ALARMS,
                            onClick = { navController.navigate(Routes.ALARMS) { launchSingleTop = true } },
                            icon = { Icon(Icons.Rounded.Alarm, contentDescription = "Alarms") },
                            label = { Text("Alarms") },
                            colors = navColors(route == Routes.ALARMS),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ROOMS,
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding(),
        ) {
            composable(Routes.ROOMS) {
                RoomsScreen(onOpenRoom = { navController.navigate(Routes.room(it)) })
            }
            composable(
                Routes.ROOM,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) {
                RoomDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SOURCES) {
                SourcesScreen(
                    onBrowse = { id, title -> navController.navigate(Routes.browse(id, title)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onOpenFolder = { id, title -> navController.navigate(Routes.browse(id, title)) },
                )
            }
            composable(
                Routes.BROWSE,
                arguments = listOf(
                    navArgument("objectId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Browse" },
                ),
            ) {
                BrowseScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFolder = { id, title -> navController.navigate(Routes.browse(id, title)) },
                )
            }
            composable(Routes.ALARMS) {
                AlarmsScreen(
                    onEdit = { navController.navigate(Routes.alarmEdit(it)) },
                    onCreate = { navController.navigate(Routes.alarmNew()) },
                )
            }
            composable(
                Routes.ALARM_EDIT,
                arguments = listOf(navArgument("alarmId") { type = NavType.StringType }),
            ) {
                AlarmEditorScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
