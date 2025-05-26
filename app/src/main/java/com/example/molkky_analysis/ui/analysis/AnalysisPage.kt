package com.example.molkky_analysis.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.tooling.preview.Preview // コメントアウト済み
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.molkky_analysis.data.model.User
import java.util.Locale
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel, // ViewModel を受け取る
    onReturnToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUserDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分析") },
                actions = {
                    Box {
                        Button(onClick = { showUserDropdown = true }) {
                            Text(uiState.selectedUserId?.let { id -> uiState.availableUsers.find { it.id == id }?.name } ?: "全ユーザー")
                        }
                        DropdownMenu(
                            expanded = showUserDropdown,
                            onDismissRequest = { showUserDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全ユーザー") },
                                onClick = {
                                    viewModel.selectUser(null)
                                    showUserDropdown = false
                                }
                            )
                            uiState.availableUsers.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name) },
                                    onClick = {
                                        viewModel.selectUser(user.id)
                                        showUserDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // サマリー統計表示
                SummaryStatsCard(stats = uiState.summaryStats)
                Spacer(Modifier.height(16.dp))

                Text("距離別成功率:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (uiState.plotData.isEmpty()) {
                    Text("表示するデータがありません。")
                } else {
                    // プロットエリア (簡易的なCanvas描画の例、またはテキスト表示)
                    DistanceSuccessRatePlot(plotData = uiState.plotData, modifier = Modifier.weight(1f).fillMaxWidth())

                    // 参考：プロットデータをテキストでリスト表示
                    // LazyColumn(modifier = Modifier.weight(1f)) {
                    //    items(uiState.plotData) { data ->
                    //        Text(
                    //            "距離: ${data.distance}m, 成功率: %.1f%% (%d/%d), エラー: +/- %.1f%%".format(
                    //                data.successRate * 100,
                    //                data.successes,
                    //                data.totalThrows,
                    //                (data.errorMargin ?: 0f) * 100
                    //            )
                    //        )
                    //    }
                    // }
                }
            }

            Spacer(Modifier.weight(1f, fill = false)) // グラフの下に余白を確保しボタンを一番下に
            Button(
                onClick = onReturnToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Return to Home")
            }
        }
    }
}

@Composable
fun SummaryStatsCard(stats: SummaryStats) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("サマリー", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("総投擲回数: ${stats.totalThrows} 回")
            stats.overallSuccessRate?.let { Text("全体成功率: %.1f %%".format(it * 100)) }
            stats.averageDistance?.let { Text("平均投擲距離: %.2f m".format(it)) }
            if (stats.successRateByAngle.isNotEmpty()){
                Text("角度別成功率:")
                stats.successRateByAngle.forEach { (angle, rate) ->
                    Text("  ${angle.uppercase(Locale.getDefault())}: ${rate?.let { "%.1f %%".format(it * 100)} ?: "N/A"}")
                }
            }
        }
    }
}


@Composable
fun DistanceSuccessRatePlot(plotData: List<DistanceSuccessRateData>, modifier: Modifier = Modifier) {
    if (plotData.isEmpty()) {
        Text("プロットするデータがありません。", modifier = modifier)
        return
    }

    // 簡易的なCanvas描画の例
    // 本格的な描画にはライブラリの利用を推奨
    Canvas(modifier = modifier.fillMaxSize()) {
        val padding = 40.dp.toPx() // 軸ラベルなどのためのパディング
        val chartWidth = size.width - 2 * padding
        val chartHeight = size.height - 2 * padding

        val minDistance = plotData.minOfOrNull { it.distance } ?: 0f
        val maxDistance = plotData.maxOfOrNull { it.distance } ?: 1f
        val maxSuccessRate = 1.0f // 成功率は0.0-1.0

        // X軸 (距離)
        drawLine(
            color = Color.Black,
            start = Offset(padding, size.height - padding),
            end = Offset(size.width - padding, size.height - padding),
            strokeWidth = 2.dp.toPx()
        )
        // Y軸 (成功率)
        drawLine(
            color = Color.Black,
            start = Offset(padding, padding),
            end = Offset(padding, size.height - padding),
            strokeWidth = 2.dp.toPx()
        )

        // Y軸の目盛り (0%, 50%, 100%)
        for (i in 0..2) {
            val y = size.height - padding - (i / 2f) * chartHeight
            drawLine(
                color = Color.Gray,
                start = Offset(padding - 5.dp.toPx(), y),
                end = Offset(padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }


        plotData.forEach { data ->
            val x = padding + ((data.distance - minDistance) / (maxDistance - minDistance).coerceAtLeast(1f)) * chartWidth
            val y = size.height - padding - (data.successRate / maxSuccessRate) * chartHeight

            // データポイント
            drawCircle(
                color = Color.Blue,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )

            // エラーバー
            data.errorMargin?.let { margin ->
                if (margin > 0) { // marginが0やnullでない場合のみ描画
                    val errorBarHalfHeight = (margin / maxSuccessRate) * chartHeight
                    val yTop = (y - errorBarHalfHeight).coerceAtLeast(padding)
                    val yBottom = (y + errorBarHalfHeight).coerceAtMost(size.height - padding)

                    // エラーバーの縦線
                    drawLine(
                        color = Color.Blue,
                        start = Offset(x, yTop),
                        end = Offset(x, yBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                    // エラーバーの上下の横線
                    drawLine(
                        color = Color.Blue,
                        start = Offset(x - 4.dp.toPx(), yTop),
                        end = Offset(x + 4.dp.toPx(), yTop),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.Blue,
                        start = Offset(x - 4.dp.toPx(), yBottom),
                        end = Offset(x + 4.dp.toPx(), yBottom),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}

/* // Previewはコメントアウト済み
@Preview(showBackground = true)
@Composable
fun AnalysisScreenPreview() {
    Molkky_analysisTheme {
        // AnalysisScreen( /* ViewModelのモックが必要 */ )
    }
}
*/