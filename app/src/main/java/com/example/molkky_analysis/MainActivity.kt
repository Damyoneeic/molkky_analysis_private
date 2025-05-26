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
import androidx.compose.ui.platform.LocalContext // Context取得に必要
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.molkky_analysis.data.local.AppDatabase
import com.example.molkky_analysis.data.repository.ThrowRepository
import com.example.molkky_analysis.ui.analysis.AnalysisScreen
import com.example.molkky_analysis.ui.data_display.DataScreen
import com.example.molkky_analysis.ui.practice.PracticePage
import com.example.molkky_analysis.ui.practice.PracticeViewModel
import com.example.molkky_analysis.ui.settings.SettingsScreenLayout
import com.example.molkky_analysis.ui.theme.Molkky_analysisTheme
import com.example.molkky_analysis.data.repository.UserRepository
// import com.example.molkky_analysis.data.repository.IUserRepository // 未使用であれば削除
import com.example.molkky_analysis.ui.data_display.DataViewModel
import com.example.molkky_analysis.ui.analysis.AnalysisViewModel
import androidx.compose.ui.unit.sp

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

enum class AppDestinations(
    val routeName: String,
    val label: String
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
    val context = LocalContext.current // ★ Context をここで取得
    val appDatabase = remember { AppDatabase.getDatabase(context) }
    val throwRepository = remember { ThrowRepository(appDatabase.throwDao()) }
    val userRepository = remember { UserRepository(appDatabase.userDao()) }

    // ★ PracticeViewModelFactory の修正
    val practiceViewModelFactory = remember {
        // userId: Int は不要になり、代わりに context を渡す
        { PracticeViewModel(throwRepository, userRepository, context) }
    }
    val dataViewModelFactory = remember {
        { DataViewModel(throwRepository, userRepository) }
    }
    val analysisViewModelFactory = remember {
        { AnalysisViewModel(throwRepository, userRepository) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        AppScreen(
            currentDestination = currentDestination,
            onNavigateTo = { destination -> currentDestination = destination },
            practiceViewModelFactory = practiceViewModelFactory, // ★ 更新されたファクトリを渡す
            dataViewModelFactory = dataViewModelFactory,
            analysisViewModelFactory = analysisViewModelFactory,
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
    practiceViewModelFactory: () -> PracticeViewModel, // ★ ファクトリの型を変更 (Int引数を削除)
    dataViewModelFactory: () -> DataViewModel,
    analysisViewModelFactory: () -> AnalysisViewModel,
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
                // ★ practiceViewModelFactory の呼び出し方を変更 (引数なし)
                val practiceViewModel = remember { practiceViewModelFactory() }
                PracticePage(
                    viewModel = practiceViewModel,
                    onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) }
                )
            }
            AppDestinations.ANALYSIS -> {
                val analysisViewModel = remember { analysisViewModelFactory() }
                AnalysisScreen(
                    viewModel = analysisViewModel,
                    onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestinations.DATA -> {
                val dataViewModel = remember { dataViewModelFactory() }
                DataScreen(
                    viewModel = dataViewModel,
                    onReturnToHome = { onNavigateTo(AppDestinations.PAGE1) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestinations.SETTINGS -> SettingsScreenLayout(
                pageLabel = currentDestination.label,
                onReturnToPage1 = { onNavigateTo(AppDestinations.PAGE1) },
                modifier = Modifier.fillMaxSize()
            )
            // 重複していた ANALYSIS の case を削除 (上の case で処理されるため)
        }
    }
}

// Page1ScreenLayout と Preview は変更なし (以下同様)
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
        val buttonModifier = Modifier
            .width(200.dp)
            .height(80.dp)
        val buttonSpacerHeight = 16.dp

        Button(
            onClick = { onNavigateTo(AppDestinations.PRACTICE) },
            modifier = buttonModifier
        ) {
            Text(AppDestinations.PRACTICE.label, fontSize = 20.sp)
        }
        Spacer(Modifier.height(buttonSpacerHeight))
        Button(
            onClick = { onNavigateTo(AppDestinations.ANALYSIS) },
            modifier = buttonModifier
        ) {
            Text(AppDestinations.ANALYSIS.label, fontSize = 20.sp)
        }
        Spacer(Modifier.height(buttonSpacerHeight))
        Button(
            onClick = { onNavigateTo(AppDestinations.DATA) },
            modifier = buttonModifier
        ) {
            Text(AppDestinations.DATA.label, fontSize = 20.sp)
        }
        Spacer(Modifier.height(buttonSpacerHeight))
        Button(
            onClick = { onNavigateTo(AppDestinations.SETTINGS) },
            modifier = buttonModifier
        ) {
            Text(AppDestinations.SETTINGS.label, fontSize = 20.sp)
        }
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