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
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
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

/** Compose Key → 方案键名（与执行侧 AWT KeyEvent.getKeyText 对齐：字母/F1-F12/数字）
 *  不收录：Esc（留关闭）、方向键、修饰键、功能杂键（未知返回 null 不录入） */
private fun keyEventName(k: Key): String? = when (k) {
    Key.A -> "A"; Key.B -> "B"; Key.C -> "C"; Key.D -> "D"; Key.E -> "E"; Key.F -> "F"
    Key.G -> "G"; Key.H -> "H"; Key.I -> "I"; Key.J -> "J"; Key.K -> "K"; Key.L -> "L"
    Key.M -> "M"; Key.N -> "N"; Key.O -> "O"; Key.P -> "P"; Key.Q -> "Q"; Key.R -> "R"
    Key.S -> "S"; Key.T -> "T"; Key.U -> "U"; Key.V -> "V"; Key.W -> "W"; Key.X -> "X"
    Key.Y -> "Y"; Key.Z -> "Z"
    Key.One -> "1"; Key.Two -> "2"; Key.Three -> "3"; Key.Four -> "4"; Key.Five -> "5"
    Key.Six -> "6"; Key.Seven -> "7"; Key.Eight -> "8"; Key.Nine -> "9"; Key.Zero -> "0"
    Key.F1 -> "F1"; Key.F2 -> "F2"; Key.F3 -> "F3"; Key.F4 -> "F4"; Key.F5 -> "F5"
    Key.F6 -> "F6"; Key.F7 -> "F7"; Key.F8 -> "F8"; Key.F9 -> "F9"; Key.F10 -> "F10"
    Key.F11 -> "F11"; Key.F12 -> "F12"
    else -> null
}

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
                        // 键位=按键捕获式（聚焦后按一个键即录入，杜绝手输多字符）
                        OutlinedTextField(
                            value = if (key.isBlank()) "" else "[$key]",
                            onValueChange = { },
                            label = { Text(if (key.isBlank()) "键位（点击后按键）" else "键位") },
                            singleLine = true,
                            readOnly = true,
                            modifier = Modifier
                                .width(140.dp)
                                .focusable()
                                .onKeyEvent { ke ->
                                    if (ke.type == KeyEventType.KeyDown) {
                                        val name = keyEventName(ke.key)
                                        if (name != null) {
                                            key = name
                                            true
                                        } else false
                                    } else false
                                }
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
