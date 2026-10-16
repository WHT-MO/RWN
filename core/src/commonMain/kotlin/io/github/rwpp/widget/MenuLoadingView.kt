/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rwpp.widget.v2.LineSpinFadeLoaderIndicator

@Composable
fun MenuLoadingView(
    message: String,
) {
    // 沉底布局：不遮挡背景人物；logo 用文字版标题替代原 CORRODING GAMES 图片
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "RWNoctiluna",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(Color(0xFFB98F3E), Color(0xFF6A5FD0))),
                fontSize = 40.sp
            )
        )
        Spacer(modifier = Modifier.size(24.dp))
        LineSpinFadeLoaderIndicator(
            radius = 25f,
            penThickness = 8f,
            color = Color.White,
        )
        Text(
            message,
            modifier = Modifier.padding(20.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
