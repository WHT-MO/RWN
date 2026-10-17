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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
 * 预设保存对话框：命名 + 队伍调整规则编辑。
 * 规则 = "移动序号x玩家到x队伍"（序号为房间玩家列表槽位，0基），可增删。
 * 初始规则为当前房间玩家的队伍快照，可自由修改。
 * 返回 (名称, 队伍规则)；名称空/取消返回 null。
 */
@Composable
fun RoomPresetNameDialog(
    initialTeamChanges: List<RoomPresets.Preset.TeamChange>,
    onResult: (Pair<String, List<RoomPresets.Preset.TeamChange>>?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf(initialTeamChanges) }

    AnimatedAlertDialog(true, onDismissRequest = { onResult(null) }) { dismiss ->
        BorderCard(modifier = Modifier.size(420.dp, 460.dp)) {
            Box {
                ExitButton { dismiss(); onResult(null) }
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text("保存为预设", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "队伍规则：移动序号x玩家到x队伍（序号按房间玩家列表从0计）",
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
                        "队伍调整规则",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )

                    // 规则列表（可滚动）
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(rules) { index, rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("移动序号", style = MaterialTheme.typography.bodyMedium)
                                NumberBox(
                                    value = rule.slot,
                                    onValue = { v -> rules = rules.replaceAt(index, rule.copy(slot = v)) },
                                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                                )
                                Text("到队伍", style = MaterialTheme.typography.bodyMedium)
                                NumberBox(
                                    value = rule.team,
                                    onValue = { v -> rules = rules.replaceAt(index, rule.copy(team = v)) },
                                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                                )
                                Spacer(Modifier.weight(1f))
                                RWTextButton("删除", Modifier.padding(2.dp)) {
                                    rules = rules.filterIndexed { i, _ -> i != index }
                                }
                            }
                            if (index < rules.lastIndex)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }

                    // 添加规则
                    RWTextButton("+ 添加队伍调整", Modifier.padding(vertical = 4.dp)) {
                        rules = rules + RoomPresets.Preset.TeamChange(rules.size, 1)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RWTextButton("取消", Modifier.padding(5.dp)) { dismiss(); onResult(null) }
                        RWTextButton("保存", Modifier.padding(5.dp)) {
                            val trimmed = name.trim()
                            dismiss()
                            onResult(
                                if (trimmed.isEmpty()) null
                                else trimmed to rules.filter { it.slot >= 0 && it.team >= 1 }
                            )
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
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit).take(3)
            onValue(text.toIntOrNull() ?: 0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        modifier = modifier
    )
}
