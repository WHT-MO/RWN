/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuButton(
    content: String,
    icon: Any? = null,
    modifier: Modifier = Modifier.size(140.dp).padding(10.dp),
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 10.dp,
        tonalElevation = 10.dp,
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon is Painter)
                Icon(icon, null, tint = MaterialTheme.colorScheme.surfaceTint, modifier = Modifier.padding(5.dp))
            else if (icon is ImageVector)
                Icon(icon, null, tint = MaterialTheme.colorScheme.surfaceTint, modifier = Modifier.padding(5.dp))

            Text(
                content,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}

/**
 * 月光菜单项：淡紫半透明面板（与浅色背景图互补），透明度联动背景透明度滑条，
 * 文字固定深紫保证任何背景透明度下可读。月牙图标 + 编号 + 标题 + 底部细分隔线。
 *
 * @param panelAlpha 面板透明度系数（由背景透明度滑条联动）
 */
@Composable
fun LunarMenuItem(
    number: String,
    content: String,
    panelAlpha: Float,
    icon: Any? = null,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val panel = if (hovered) 0.85f else 0.55f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFB9AFE8).copy(alpha = ((panel + panelAlpha) / 2f).coerceAtMost(1f)))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        hovered = event.type == PointerEventType.Enter
                    }
                }
            }
            .clickable(onClick = onClick)
    ) {
        // 内边距放在 Row 上而非 Column——下划线保持全宽，与悬停背景条左右对齐
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (icon is Painter)
                Icon(icon, null, tint = Color(0xFF554D9E), modifier = Modifier.size(20.dp))
            else if (icon is ImageVector)
                Icon(icon, null, tint = Color(0xFF554D9E), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                number,
                color = Color(0xFF554D9E).copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(28.dp)
            )
            Text(
                content,
                color = Color(0xFF35315E),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF554D9E).copy(alpha = 0.25f))
        )
    }
}

/** 菜单分组标题：小号字距拉开（如"开始游戏"/"客户端增强"） */
@Composable
fun LunarGroupTitle(title: String, panelAlpha: Float) {
    Text(
        title,
        color = Color(0xFF45407A).copy(alpha = (0.65f + panelAlpha / 4f).coerceAtMost(1f)),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 6.sp),
        modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun BoxScope.ExitButton(onClick: () -> Unit) {
    Button(
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                0.8f
            )
        ),
        modifier = Modifier.size(30.dp).align(Alignment.TopEnd),
        contentPadding = PaddingValues(0.dp),
        onClick = { onClick() },
    ) {
        Icon(
            Icons.Default.Close,
            tint = MaterialTheme.colorScheme.surfaceTint,
            contentDescription = null
        )
    }
}


