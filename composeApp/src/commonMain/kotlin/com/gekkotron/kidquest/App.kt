package com.gekkotron.kidquest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gekkotron.kidquest.data.SettingsRepository
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.tab_goals
import com.gekkotron.kidquest.resources.tab_home
import com.gekkotron.kidquest.resources.tab_settings
import com.gekkotron.kidquest.ui.locale.AppEnvironment
import com.gekkotron.kidquest.ui.child.ChildDetailScreen
import com.gekkotron.kidquest.ui.home.HomeScreen
import com.gekkotron.kidquest.ui.nav.ChildDetailRoute
import com.gekkotron.kidquest.ui.nav.GoalsRoute
import com.gekkotron.kidquest.ui.nav.HomeRoute
import com.gekkotron.kidquest.ui.nav.SettingsRoute
import com.gekkotron.kidquest.ui.theme.KidQuestTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.reflect.KClass

@Composable
fun App() {
    val settings = koinInject<SettingsRepository>()
    val language by settings.observeLanguage().collectAsStateWithLifecycle(initialValue = null)
    AppEnvironment(language) {
        KidQuestTheme {
            val nav = rememberNavController()
            val snackbarHostState = remember { SnackbarHostState() }
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = { BottomBar(nav) },
                ) { padding ->
                    NavHost(nav, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
                        composable<HomeRoute> {
                            HomeScreen(snackbarHostState, onOpenChild = { nav.navigate(ChildDetailRoute(it)) })
                        }
                        composable<GoalsRoute> { Placeholder("screen-goals") }
                        composable<SettingsRoute> { Placeholder("screen-settings") }
                        composable<ChildDetailRoute> { entry ->
                            val route = entry.toRoute<ChildDetailRoute>()
                            ChildDetailScreen(route.childId, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(tag: String) {
    Box(Modifier.fillMaxSize().testTag(tag))
}

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: StringResource, val icon: String, val tag: String)

private val tabs = listOf(
    Tab(HomeRoute, HomeRoute::class, Res.string.tab_home, "🏠", "tab-home"),
    Tab(GoalsRoute, GoalsRoute::class, Res.string.tab_goals, "🎯", "tab-goals"),
    Tab(SettingsRoute, SettingsRoute::class, Res.string.tab_settings, "⚙️", "tab-settings"),
)

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    NavigationBar {
        tabs.forEach { tab ->
            val selected = entry?.destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
            NavigationBarItem(
                selected = selected,
                modifier = Modifier.testTag(tab.tag),
                icon = { Text(tab.icon) },
                label = { Text(stringResource(tab.label)) },
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
