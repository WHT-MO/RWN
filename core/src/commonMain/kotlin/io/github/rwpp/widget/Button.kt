/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.widget

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
 * 月光菜单项：主菜单竖排列表用。半透明白（月光感），hover 提亮，
 * 左侧图标 + 文字横排，宽度撑满父容器。
 */
@Composable
fun LunarMenuItem(
    content: String,
    icon: Any? = null,
    onClick: () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    Surface(
        color = if (hovered) Color(0xE6FFFFFF) else Color(0xB3FFFFFF),
        contentColor = Color(0xFF2B2E4A),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        hovered = event.type == PointerEventType.Enter
                    }
                }
            },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon is Painter)
                Icon(icon, null, tint = Color(0xFF6A5FD0), modifier = Modifier.size(24.dp))
            else if (icon is ImageVector)
                Icon(icon, null, tint = Color(0xFF6A5FD0), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                content,
                color = Color(0xFF2B2E4A),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
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


