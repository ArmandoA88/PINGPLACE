package com.pingplace.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pingplace.data.AppContainer
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.ui.add.AddReminderScreen
import com.pingplace.ui.add.AddReminderViewModel
import com.pingplace.ui.blocked.BlockedTimesScreen
import com.pingplace.ui.blocked.BlockedTimesViewModel
import com.pingplace.ui.brand.BrandDetailScreen
import com.pingplace.ui.brand.BrandDetailViewModel
import com.pingplace.ui.common.viewModelFactory
import com.pingplace.ui.common.ReliabilityStatus
import com.pingplace.ui.completed.CompletedScreen
import com.pingplace.ui.completed.CompletedViewModel
import com.pingplace.ui.home.HomeScreen
import com.pingplace.ui.home.HomeViewModel
import com.pingplace.ui.onboarding.OnboardingScreen
import com.pingplace.ui.onboarding.OnboardingViewModel
import com.pingplace.ui.settings.SettingsScreen
import com.pingplace.ui.settings.SettingsViewModel
import com.pingplace.ui.theme.Ember
import com.pingplace.ui.theme.MeadowGreen
import com.pingplace.ui.theme.PingPlaceTheme

private object PingPlaceRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val BLOCKED = "blocked"
    const val COMPLETED = "completed"
    const val SETTINGS = "settings"
    const val BRAND = "brand/{brandQuery}"

    fun brand(brandQuery: String): String = "brand/${Uri.encode(brandQuery)}"
    fun edit(id: Long): String = "edit/$id"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PingPlaceApp(
    container: AppContainer,
    initialBrandQuery: String?,
    reliabilityStatus: ReliabilityStatus,
    requestNotifications: () -> Unit,
    requestFineLocation: () -> Unit,
    requestBackgroundLocation: () -> Unit,
    openLocationSettings: () -> Unit,
    openAppSettings: () -> Unit,
    openBatterySettings: () -> Unit
) {
    val navController = rememberNavController()
    val settings by container.repository.observeUserSettings()
        .collectAsStateWithLifecycle(initialValue = UserSettingsEntity())

    val topLevelDestinations = remember {
        listOf(
            TopLevelDestination(PingPlaceRoutes.HOME, "Home", Icons.Outlined.Home),
            TopLevelDestination(PingPlaceRoutes.BLOCKED, "Blocked", Icons.Outlined.Schedule),
            TopLevelDestination(PingPlaceRoutes.COMPLETED, "Done", Icons.Outlined.CheckCircle),
            TopLevelDestination(PingPlaceRoutes.SETTINGS, "Settings", Icons.Outlined.Settings)
        )
    }

    LaunchedEffect(settings.onboardingComplete) {
        if (settings.onboardingComplete) {
            navController.navigate(PingPlaceRoutes.HOME) {
                popUpTo(PingPlaceRoutes.ONBOARDING) { inclusive = true }
            }
        }
    }

    LaunchedEffect(initialBrandQuery, settings.onboardingComplete) {
        if (settings.onboardingComplete && !initialBrandQuery.isNullOrBlank()) {
            navController.navigate(PingPlaceRoutes.brand(initialBrandQuery))
        }
    }

    PingPlaceTheme(darkTheme = settings.darkModeEnabled) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val showBottomBar = currentDestination?.route in topLevelDestinations.map { it.route }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        topLevelDestinations.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == destination.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (destination.route == PingPlaceRoutes.BLOCKED) {
                                        MaterialTheme.colorScheme.onError
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary
                                    },
                                    selectedTextColor = if (destination.route == PingPlaceRoutes.BLOCKED) {
                                        Ember
                                    } else {
                                        MeadowGreen
                                    },
                                    indicatorColor = if (destination.route == PingPlaceRoutes.BLOCKED) {
                                        Ember
                                    } else {
                                        MeadowGreen
                                    }
                                ),
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { androidx.compose.material3.Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = if (settings.onboardingComplete) PingPlaceRoutes.HOME else PingPlaceRoutes.ONBOARDING
            ) {
                composable(PingPlaceRoutes.ONBOARDING) {
                    val vm: OnboardingViewModel = viewModel(
                        factory = viewModelFactory {
                            OnboardingViewModel(container.repository, container.monitorScheduler)
                        }
                    )
                    OnboardingScreen(
                        innerPadding = padding,
                        reliabilityStatus = reliabilityStatus,
                        onRequestNotifications = requestNotifications,
                        onRequestFineLocation = requestFineLocation,
                        onRequestBackgroundLocation = requestBackgroundLocation,
                        onOpenLocationSettings = openLocationSettings,
                        onOpenAppSettings = openAppSettings,
                        onOpenBatterySettings = openBatterySettings,
                        onContinue = vm::finishOnboarding
                    )
                }
                composable(PingPlaceRoutes.HOME) {
                    val vm: HomeViewModel = viewModel(
                        factory = viewModelFactory {
                            HomeViewModel(container.repository, container.monitorScheduler)
                        }
                    )
                    HomeScreen(
                        innerPadding = padding,
                        viewModel = vm,
                        isLocationServicesEnabled = reliabilityStatus.locationServicesEnabled,
                        onAddReminder = { navController.navigate(PingPlaceRoutes.ADD) },
                        onBrandClick = { navController.navigate(PingPlaceRoutes.brand(it)) },
                        onEditReminder = { navController.navigate(PingPlaceRoutes.edit(it)) }
                    )
                }
                composable(PingPlaceRoutes.ADD) {
                    val vm: AddReminderViewModel = viewModel(
                        factory = viewModelFactory {
                            AddReminderViewModel(container.repository, container.monitorScheduler)
                        }
                    )
                    AddReminderScreen(
                        innerPadding = padding,
                        viewModel = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = PingPlaceRoutes.EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val reminderId = backStackEntry.arguments?.getLong("id") ?: 0L
                    val vm: AddReminderViewModel = viewModel(
                        key = "edit_$reminderId",
                        factory = viewModelFactory {
                            AddReminderViewModel(
                                repository = container.repository,
                                scheduler = container.monitorScheduler,
                                reminderId = reminderId
                            )
                        }
                    )
                    AddReminderScreen(
                        innerPadding = padding,
                        viewModel = vm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(PingPlaceRoutes.BLOCKED) {
                    val vm: BlockedTimesViewModel = viewModel(
                        factory = viewModelFactory {
                            BlockedTimesViewModel(container.repository, container.monitorScheduler)
                        }
                    )
                    BlockedTimesScreen(
                        innerPadding = padding,
                        viewModel = vm
                    )
                }
                composable(PingPlaceRoutes.COMPLETED) {
                    val vm: CompletedViewModel = viewModel(
                        factory = viewModelFactory {
                            CompletedViewModel(
                                repository = container.repository,
                                scheduler = container.monitorScheduler
                            )
                        }
                    )
                    CompletedScreen(
                        innerPadding = padding,
                        viewModel = vm
                    )
                }
                composable(PingPlaceRoutes.SETTINGS) {
                    val vm: SettingsViewModel = viewModel(
                        factory = viewModelFactory {
                            SettingsViewModel(
                                repository = container.repository,
                                scheduler = container.monitorScheduler,
                                notificationHelper = container.notificationHelper,
                                offlinePackManager = container.offlinePackManager
                            )
                        }
                    )
                    SettingsScreen(
                        innerPadding = padding,
                        viewModel = vm,
                        reliabilityStatus = reliabilityStatus,
                        onRequestNotifications = requestNotifications,
                        onRequestFineLocation = requestFineLocation,
                        onRequestBackgroundLocation = requestBackgroundLocation,
                        onOpenLocationSettings = openLocationSettings,
                        onOpenAppSettings = openAppSettings,
                        onOpenBatterySettings = openBatterySettings
                    )
                }
                composable(
                    route = PingPlaceRoutes.BRAND,
                    arguments = listOf(navArgument("brandQuery") { type = NavType.StringType })
                ) { backStackEntry ->
                    val brandQuery = Uri.decode(backStackEntry.arguments?.getString("brandQuery").orEmpty())
                    val vm: BrandDetailViewModel = viewModel(
                        key = brandQuery,
                        factory = viewModelFactory {
                            BrandDetailViewModel(
                                brandQuery = brandQuery,
                                repository = container.repository,
                                scheduler = container.monitorScheduler,
                                locationClient = container.locationClient,
                                placeSearchProvider = container.placeSearchProvider
                            )
                        }
                    )
                    BrandDetailScreen(
                        innerPadding = padding,
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                        onEditReminder = { navController.navigate(PingPlaceRoutes.edit(it)) }
                    )
                }
            }
        }
    }
}
