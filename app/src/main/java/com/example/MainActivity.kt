package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.BatchStudioScreen
import com.example.ui.HomeScreen
import com.example.ui.PdfAppViewModel
import com.example.ui.PdfViewerScreen
import com.example.ui.ScannerScreen
import com.example.ui.theme.PDFStudioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PdfAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isAppDarkMode.collectAsState()
            PDFStudioTheme(darkTheme = isDark) {
                PdfAppNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PdfAppNavigation(viewModel: PdfAppViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenDocument = { doc ->
                    viewModel.openDocument(doc)
                    navController.navigate("viewer")
                },
                onNavigateScanner = {
                    viewModel.clearScannedBitmaps()
                    navController.navigate("scanner")
                },
                onNavigateBatchStudio = {
                    navController.navigate("batch_studio")
                },
                onNavigateTools = {
                    navController.navigate("tools")
                }
            )
        }

        composable("viewer") {
            PdfViewerScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.closeDocument()
                    navController.popBackStack()
                }
            )
        }

        composable("scanner") {
            ScannerScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.clearScannedBitmaps()
                    navController.popBackStack()
                },
                onPdfCreated = { doc ->
                    viewModel.openDocument(doc)
                    navController.navigate("viewer") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("batch_studio") {
            BatchStudioScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onOpenDocument = { doc ->
                    viewModel.openDocument(doc)
                    navController.navigate("viewer") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable("tools") {
            com.example.ui.ToolsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                },
                onOpenDocument = { doc ->
                    viewModel.openDocument(doc)
                    navController.navigate("viewer") {
                        popUpTo("home")
                    }
                }
            )
        }
    }
}

