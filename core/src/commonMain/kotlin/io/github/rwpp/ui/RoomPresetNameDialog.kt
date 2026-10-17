/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rwpp.room.RoomPresets
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton
import io.github.rwpp.widget.RWTextButton

/**
 * 预设保存对话框：命名 + 移动规则编辑。
 * 规则 = "移动序号x玩家到序号x位置、x队伍"（位置=出生点位0基；-1=不换位）。
 * 初始为当前玩家的位置+队伍快照，可增删改。目标位被占时应用环节自动交换。
 */
@Composable
fun RoomPresetNameDialog(
    initialTeamChanges: List<RoomPresets.Preset.TeamChange>,
    initialName: String? = null,
    onResult: (Pair<String, List<RoomPresets.Preset.TeamChange>>?) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName ?: "") }
    var rules by remember { mutableStateOf(initialTeamChanges) }

    AnimatedAlertDialog(true, onDismissRequest = { onResult(null) }) { dismiss ->
        BorderCard(modifier = Modifier.size(460.dp, 480.dp)) {
            Box {
                ExitButton { dismiss(); onResult(null) }
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text(if (initialName != null) "编辑预设" else "保存为预设", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "预设 = 房间设置（雾/资金/AI难度/速度等面板当前值）+ 移动规则",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "移动规则：移动序号x玩家到序号x位置、x队伍（序号=玩家当前所在出生点位1-10，与队伍无关；位置留空只换队伍；目标位被占时自动交换）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("预设名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "移动规则",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(rules) { index, rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("序号", style = MaterialTheme.typography.bodyMedium)
                                NumberBox(
                                    value = rule.slot + 1,
                                    onValue = { v -> rules = rules.replaceAt(index, rule.copy(slot = v - 1)) },
                                    modifier = Modifier.width(64.dp).padding(horizontal = 3.dp)
                                )
                                Text("→位置", style = MaterialTheme.typography.bodyMedium)
                                NumberBox(
                                    value = rule.targetSpawn + 1,
                                    onValue = { v -> rules = rules.replaceAt(index, rule.copy(targetSpawn = v - 1)) },
                                    modifier = Modifier.width(64.dp).padding(horizontal = 3.dp)
                                )
                                Text("队伍", style = MaterialTheme.typography.bodyMedium)
                                NumberBox(
                                    value = rule.targetTeam,
                                    onValue = { v -> rules = rules.replaceAt(index, rule.copy(targetTeam = v)) },
                                    modifier = Modifier.width(64.dp).padding(horizontal = 3.dp)
                                )
                                Spacer(Modifier.weight(1f))
                                RWTextButton("删", Modifier.padding(2.dp)) {
                                    rules = rules.filterIndexed { i, _ -> i != index }
                                }
                            }
                            if (index < rules.lastIndex)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }

                    RWTextButton("+ 添加移动规则", Modifier.padding(vertical = 4.dp)) {
                        rules = rules + RoomPresets.Preset.TeamChange(rules.size, rules.size, 1)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RWTextButton("取消", Modifier.padding(5.dp)) { dismiss(); onResult(null) }
                        var nameError by remember { mutableStateOf(false) }
                        if (nameError) {
                            Text(
                                "请输入预设名称",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 5.dp)
                            )
                        }
                        RWTextButton("保存", Modifier.padding(5.dp)) {
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                nameError = true
                            } else {
                                dismiss()
                                onResult(trimmed to rules.filter { it.slot >= 0 && it.targetSpawn >= -1 })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun List<RoomPresets.Preset.TeamChange>.replaceAt(index: Int, v: RoomPresets.Preset.TeamChange): List<RoomPresets.Preset.TeamChange> =
    mapIndexed { i, r -> if (i == index) v else r }

@Composable
private fun NumberBox(value: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(if (value < 0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(2)
            onValue(text.toIntOrNull() ?: -1)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        modifier = modifier
    )
}
