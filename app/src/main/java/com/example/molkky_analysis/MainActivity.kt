package com.example.molkky_analysis // ベースパッケージ

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.vector.ImageVector // ImageVectorのimportは不要になる
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.molkky_analysis.data.local.AppDatabase
import com.example.molkky_analysis.data.repository.ThrowRepository
import com.example.molkky_analysis.ui.analysis.AnalysisScreen
import com.example.molkky_analysis.ui.data_display.DataScreen
import com.example.molkky_analysis.ui.practice.PracticePage
import com.example.molkky_analysis.ui.practice.PracticeViewModel
import com.example.molkky_analysis.ui.settings.SettingsScreenLayout // こちらの関数名に合わせています
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            Molkky_analysisTheme {
                Molkky_analysisApp()
            }
        }
    }
}

// AppDestinations enum から icon プロパティを削除
enum class AppDestinations(
    val routeName: String,
    val label: String
    // val icon: ImageVector // アイコンプロパティを削除
) {
    PAGE1("Page1", "Page 1"),
    PRACTICE("PracticePage", "Practice"),
    ANALYSIS("AnalysisPage", "Analysis"),
    DATA("DataPage", "Data"),
    SETTINGS("SettingsPage", "Settings");
}

@Composable
fun Molkky_analysisApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.PAGE1) }
    val context = LocalContext.current
    val appDatabase = remember { AppDatabase.getDatabase(context) }
    val throwRepository = remember { ThrowRepository(appDatabase.throwDao()) }
    val practiceViewModelFactory = remember { { userId: Int -> PracticeViewModel(throwRepository, userId) } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // bottomBar = { // もしNavigationBarなどがあれば、そこもテキスト表示にするか、コンポーネント自体を削除
        //     NavigationBar {
        //         AppDestinations.entries.forEach { destination ->
        //             NavigationBarItem(
        //                 selected = currentDestination == destination,
        //                 onClick = { currentDestination = destination },
        //                 label = { Text(destination.label) }, // ラベル(テキスト)のみ使用
        //                 icon = { } // アイコン部分は空にするか、NavigationBarItemのicon引数自体を削除
        //             )
        //         }
        //     }
        // }
    ) { innerPadding ->
        AppScreen(
            currentDestination = currentDestination,
            onNavigateTo = { destination -> currentDestination = destination },
            practiceViewModelFactory = practiceViewModelFactory,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
fun AppScreen(
    currentDestination: AppDestinations,
    onNavigateTo: (AppDestinations) -> Unit,
    practiceViewModelFactory: (Int) -> PracticeViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (currentDestination) {
            AppDestinations.PAGE1 -> Page1ScreenLayout(
                onNavigateTo = onNavigateTo,
                modifier = Modifier.fillMaxSize()
            )
            AppDestinations.PRACTICE -> {
                val practiceViewModel = remember { practiceViewModelFactory(1) } // 仮のユーザーID
                PracticePage(
                    viewModel = practiceViewModel,
                    onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) }
                )
            }
            AppDestinations.ANALYSIS -> AnalysisScreen(
                pageLabel = currentDestination.label,
                onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) },
                modifier = Modifier.fillMaxSize()
            )
            AppDestinations.DATA -> DataScreen(
                pageLabel = currentDestination.label,
                onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) },
                modifier = Modifier.fillMaxSize()
            )
            AppDestinations.SETTINGS -> SettingsScreenLayout(
                pageLabel = currentDestination.label,
                onReturnToPage1 = { onNavigateTo(AppDestinations.PAGE1) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun Page1ScreenLayout(
    onNavigateTo: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ${AppDestinations.PAGE1.label}",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(32.dp))
        // 各ページへのナビゲーションボタン (テキストのみ)
        Button(onClick = { onNavigateTo(AppDestinations.PRACTICE) }) { Text(AppDestinations.PRACTICE.label) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigateTo(AppDestinations.ANALYSIS) }) { Text(AppDestinations.ANALYSIS.label) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigateTo(AppDestinations.DATA) }) { Text(AppDestinations.DATA.label) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigateTo(AppDestinations.SETTINGS) }) { Text(AppDestinations.SETTINGS.label) }
    }
}

@Preview(showBackground = true, name = "Page 1 Layout Preview (MainActivity)")
@Composable
fun Page1LayoutPreview() {
    Molkky_analysisTheme {
        Page1ScreenLayout(onNavigateTo = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(device = Devices.PHONE, showBackground = true, name = "Full App Preview (MainActivity)")
@Composable
fun FullAppPreview() {
    Molkky_analysisTheme {
        Molkky_analysisApp()
    }
}