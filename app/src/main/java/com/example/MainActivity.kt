package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StudioBottomNav
import com.example.ui.navigation.StudioDestination
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.background.BackgroundStudioScreen
import com.example.ui.screens.document.DocumentStudioScreen
import com.example.ui.screens.idcard.IdCardStudioScreen
import com.example.ui.screens.passport.PassportStudioScreen
import com.example.ui.screens.projects.ProjectsHistoryScreen
import com.example.ui.screens.studio.StudioHealthScreen
import com.example.ui.theme.PixelProTheme
import com.example.viewmodel.BackgroundViewModel
import com.example.viewmodel.DocumentViewModel
import com.example.viewmodel.IdCardViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PassportViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val passportViewModel: PassportViewModel by viewModels()
    private val idCardViewModel: IdCardViewModel by viewModels()
    private val backgroundViewModel: BackgroundViewModel by viewModels()
    private val documentViewModel: DocumentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelProTheme {
                PixelProApp(
                    mainViewModel = mainViewModel,
                    passportViewModel = passportViewModel,
                    idCardViewModel = idCardViewModel,
                    backgroundViewModel = backgroundViewModel,
                    documentViewModel = documentViewModel
                )
            }
        }
    }
}

@Composable
fun PixelProApp(
    mainViewModel: MainViewModel,
    passportViewModel: PassportViewModel,
    idCardViewModel: IdCardViewModel,
    backgroundViewModel: BackgroundViewModel,
    documentViewModel: DocumentViewModel
) {
    val currentDestination by mainViewModel.currentDestination.collectAsStateWithLifecycle()
    val recentProjects by mainViewModel.recentProjects.collectAsStateWithLifecycle()
    val allProjects by mainViewModel.allProjects.collectAsStateWithLifecycle()
    val totalCount by mainViewModel.totalProjectsCount.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle user messages from ViewModel
    LaunchedEffect(Unit) {
        mainViewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // BackHandler: return to dashboard if on another screen
    BackHandler(enabled = currentDestination != StudioDestination.DASHBOARD) {
        mainViewModel.navigateTo(StudioDestination.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            StudioBottomNav(
                currentDestination = currentDestination,
                onDestinationSelected = { dest ->
                    mainViewModel.navigateTo(dest)
                }
            )
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (currentDestination) {
            StudioDestination.DASHBOARD -> {
                DashboardScreen(
                    recentProjects = recentProjects,
                    totalProjectsCount = totalCount,
                    onNavigate = { mainViewModel.navigateTo(it) },
                    onShareProject = { mainViewModel.shareProject(it) },
                    modifier = modifier
                )
            }
            StudioDestination.PASSPORT -> {
                PassportStudioScreen(
                    viewModel = passportViewModel,
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    onExportComplete = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = modifier
                )
            }
            StudioDestination.ID_CARD -> {
                IdCardStudioScreen(
                    viewModel = idCardViewModel,
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    onExportComplete = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = modifier
                )
            }
            StudioDestination.BACKGROUND -> {
                BackgroundStudioScreen(
                    viewModel = backgroundViewModel,
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    onExportComplete = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = modifier
                )
            }
            StudioDestination.DOCUMENT -> {
                DocumentStudioScreen(
                    viewModel = documentViewModel,
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    onExportComplete = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = modifier
                )
            }
            StudioDestination.PROJECTS -> {
                ProjectsHistoryScreen(
                    projects = allProjects,
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    onNavigate = { mainViewModel.navigateTo(it) },
                    onExportToGallery = { mainViewModel.exportToGallery(it) },
                    onShareProject = { mainViewModel.shareProject(it) },
                    onDeleteProject = { mainViewModel.deleteProject(it) },
                    modifier = modifier
                )
            }
            StudioDestination.HEALTH -> {
                StudioHealthScreen(
                    onBackClick = { mainViewModel.navigateTo(StudioDestination.DASHBOARD) },
                    modifier = modifier
                )
            }
        }
    }
}
