/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rwpp.coreVersion
import io.github.rwpp.projectVersion
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton

/**
 * 关于对话框：版本信息 + 对 RWPP 上游的致谢（AGPL 衍生项目的出处声明）。
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AnimatedAlertDialog(true, onDismissRequest = onDismiss) { dismiss ->
        BorderCard(modifier = Modifier.size(420.dp)) {
            Box {
                ExitButton(dismiss)
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("RWNoctiluna", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "$projectVersion (core $coreVersion)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "社区增强客户端，基于 RWPP (AGPL-3.0) 修改。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "感谢 RWPP contributors 的开源工作。\n上游项目：github.com/Minxyzgo/RWPP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
