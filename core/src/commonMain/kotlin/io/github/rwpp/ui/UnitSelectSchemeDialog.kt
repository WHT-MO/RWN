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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rwpp.appKoin
import io.github.rwpp.config.Settings
import io.github.rwpp.config.UnitSelectScheme
import io.github.rwpp.game.Game
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton
import io.github.rwpp.widget.RWTextButton

/**
 * 单位方案编辑对话框：名称 + 键位 + 单位类型多选（游戏全类型列表，带显示名）。
 * 键位与现有方案或引擎按键重复时仅提示不阻止（用户决策）。
 * onResult 返回 null=取消。
 */
@Composable
fun UnitSelectSchemeDialog(
    initial: UnitSelectScheme?,
    onResult: (UnitSelectScheme?) -> Unit
) {
    val settings = appKoin.get<Settings>()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var key by remember { mutableStateOf(initial?.key ?: "") }
    var selected by remember { mutableStateOf(initial?.unitTypes?.toSet() ?: emptySet()) }

    // 类型列表（游戏全单位类型：name=ini名 存储用 displayName 展示用）
    val allTypes = remember {
        runCatching {
            appKoin.get<Game>().getAllUnitTypes()
        }.getOrDefault(emptyList())
    }

    // 键位冲突提示（仅提示不阻止）
    val conflictHint = key.takeIf { it.isNotBlank() }?.let { k ->
        settings.unitSelectSchemes
            .firstOrNull { it !== initial && it.key.equals(k, ignoreCase = true) }
            ?.let { "与方案「${it.name}」键位相同" }
    } ?: if (key.length == 1 && "eamnEA MN".contains(key.uppercase())) "与引擎聊天/地图/设置等快捷键可能冲突" else null

    AnimatedAlertDialog(true, onDismissRequest = { onResult(null) }) { dismiss ->
        BorderCard(modifier = Modifier.size(460.dp, 560.dp)) {
            Box {
                ExitButton { dismiss(); onResult(null) }
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text(
                        if (initial == null) "新建单位方案" else "编辑单位方案",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "按键后实时选中场上所有匹配类型的本机存活单位（含新产出）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(20) },
                            label = { Text("方案名称") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(end = 5.dp)
                        )
                        OutlinedTextField(
                            value = key,
                            onValueChange = { key = it.trim().take(3) },
                            label = { Text("键位") },
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                    }
                    if (conflictHint != null) {
                        Text(
                            "⚠ $conflictHint",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        )
                    }

                    Text(
                        "单位类型（${selected.size} 种）",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    // 类型多选列表（滚动）
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(allTypes, key = { it.name }) { t ->
                            val checked = t.name in selected
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = { c ->
                                    selected = if (c) selected + t.name else selected - t.name
                                })
                                Text(
                                    t.displayName.ifBlank { t.name },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RWTextButton("取消", Modifier.padding(5.dp)) { dismiss(); onResult(null) }
                        RWTextButton("保存", Modifier.padding(5.dp)) {
                            val n = name.trim()
                            if (n.isNotEmpty() && key.isNotBlank() && selected.isNotEmpty()) {
                                dismiss()
                                onResult(UnitSelectScheme(n, key.trim().uppercase(), selected.toList()))
                            }
                        }
                    }
                }
            }
        }
    }
}
