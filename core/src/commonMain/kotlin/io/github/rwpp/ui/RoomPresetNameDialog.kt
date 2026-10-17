/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton
import io.github.rwpp.widget.RWTextButton

/**
 * 预设命名对话框：返回 null = 取消，非空 = 确认（空名视为取消）。
 */
@Composable
fun RoomPresetNameDialog(onResult: (String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    AnimatedAlertDialog(true, onDismissRequest = { onResult(null) }) { dismiss ->
        BorderCard(modifier = Modifier.size(360.dp, 190.dp)) {
            Box {
                ExitButton { dismiss(); onResult(null) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("保存为预设（含当前全部玩家的队伍分配）")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("预设名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RWTextButton("取消", Modifier.padding(5.dp)) { dismiss(); onResult(null) }
                        RWTextButton("保存", Modifier.padding(5.dp)) {
                            dismiss()
                            onResult(name.trim().ifEmpty { null })
                        }
                    }
                }
            }
        }
    }
}
