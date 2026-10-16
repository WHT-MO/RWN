/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eclipsesource.json.Json
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import io.github.rwpp.appKoin
import io.github.rwpp.net.Net
import io.github.rwpp.widget.AnimatedAlertDialog
import io.github.rwpp.widget.BorderCard
import io.github.rwpp.widget.ExitButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * 社区公告数据源（静态 JSON，字段: title / body）。
 * 部署渠道确定后修改此 URL；留空字符串则显示默认占位内容。
 */
const val announcementUrl = ""

/**
 * 社区公告对话框：点击主菜单公告按钮时拉取并展示，不自动弹窗打扰游戏。
 */
@Composable
fun AnnouncementDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("社区公告") }
    var body by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (announcementUrl.isBlank()) {
            body = "暂无公告。"
            return@LaunchedEffect
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val net = appKoin.get<Net>()
                val response = net.client.newCall(Request.Builder().url(announcementUrl).get().build()).execute()
                response.body?.string()
            }
        }.onSuccess { text ->
            runCatching {
                val obj = Json.parse(text).asObject()
                title = obj.getString("title", "社区公告")
                body = obj.getString("body", "")
            }.onFailure { loadFailed = true }
        }.onFailure { loadFailed = true }
    }

    AnimatedAlertDialog(true, onDismissRequest = onDismiss) { dismiss ->
        BorderCard(modifier = Modifier.size(500.dp)) {
            Box {
                ExitButton(dismiss)
                Column(
                    modifier = Modifier.fillMaxSize().padding(15.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(10.dp))

                    when {
                        body != null -> Markdown(
                            body!!,
                            colors = markdownColor(),
                            typography = markdownTypography()
                        )
                        loadFailed -> Text(
                            "公告加载失败，请检查网络后重试。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        else -> Text(
                            "加载中…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
