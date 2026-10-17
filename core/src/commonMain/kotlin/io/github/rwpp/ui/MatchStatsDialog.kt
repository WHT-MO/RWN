/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rwpp.stats.MatchStatistics
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对局统计：顶部汇总（总局数/胜率/累计时长）+ 最近对局列表（仅多人对局）。
 */
@Composable
fun MatchStatsDialog(onDismiss: () -> Unit) {
    val records = MatchStatistics.records.sortedByDescending { it.timestamp }
    val total = records.size
    val wins = records.count { it.won }
    val totalMin = records.sumOf { it.durationSec } / 60

    AnimatedAlertDialog(true, onDismissRequest = onDismiss) { dismiss ->
        BorderCard(modifier = Modifier.size(520.dp, 560.dp)) {
            Box {
                ExitButton(dismiss)
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text("对局统计", style = MaterialTheme.typography.headlineLarge)

                    // 汇总
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Summary("总局数", "$total")
                        Summary("胜率", if (total > 0) "${wins * 100 / total}%" else "-")
                        Summary("累计时长", "${totalMin}分钟")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // 记录列表
                    if (total == 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "暂无对局记录\n（仅记录打完的多人对局）",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 28.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 6.dp)) {
                            items(records) { r ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (r.won) "胜" else "败",
                                        color = if (r.won) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.mapName, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${r.playerCount}人 · ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(r.timestamp))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    }
                                    Text(
                                        "${r.durationSec / 60}分${r.durationSec % 60}秒",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
