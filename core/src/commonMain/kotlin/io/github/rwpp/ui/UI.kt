/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rwpp.AppContext
import io.github.rwpp.appKoin
import io.github.rwpp.config.ConfigIO
import io.github.rwpp.config.Settings
import io.github.rwpp.core.Initialization
import io.github.rwpp.coreVersion
import io.github.rwpp.event.EventPriority
import io.github.rwpp.event.GlobalEventChannel
import io.github.rwpp.event.events.DisconnectEvent
import io.github.rwpp.game.Player
import io.github.rwpp.i18n.I18nType
import io.github.rwpp.i18n.readI18n
import io.github.rwpp.net.Net
import io.github.rwpp.projectVersion
import io.github.rwpp.core.generated.resources.*
import io.github.rwpp.ui.UI.showQuestion
import io.github.rwpp.ui.UI.showWarning
import io.github.rwpp.ui.color.getTeamColor
import io.github.rwpp.widget.LunarMenuItem
import io.github.rwpp.widget.LunarMenuItem
import io.github.rwpp.widget.v2.RWIconButton
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

object UI : Initialization, IUserInterface {
    internal var backgroundTransparency by mutableStateOf(appKoin.get<Settings>().backgroundTransparency)
    internal var selectedColorSchemeName by mutableStateOf(appKoin.get<Settings>().selectedTheme ?: "Lunar Orbital")
    var question by mutableStateOf<Question?>(null)
        internal set
    var warning by mutableStateOf<Warning?>(null)
        internal set
    var dialogWidget by mutableStateOf<Widget?>(null)
        internal set
    var chatMessages by mutableStateOf(AnnotatedString(""))
        internal set

    var showMissionView by mutableStateOf(false)
    var showMultiplayerView by mutableStateOf(false)
    var showReplayView by mutableStateOf(false)
    var showSettingsView by mutableStateOf(false)
    var showModsView by mutableStateOf(false)
    var showRoomView by mutableStateOf(false)
    var showExtensionView by mutableStateOf(false)
    var showContributorList by mutableStateOf(false)
    var showResourceBrowser by mutableStateOf(false)

    var roomSelectedPlayer by mutableStateOf<Player?>(null)
        internal set
    var receivingNetworkDialogTitle by mutableStateOf("")
    var showNetworkDialog by mutableStateOf(false)
    var UiProvider: UIProvider = UIProvider()

    private val relayRegex = Regex("""R\d+""")

    override fun showWarning(reason: String, isKicked: Boolean) {
        synchronized(UI) {
            warning = Warning(reason, isKicked)
        }
    }

    override fun showQuestion(title: String, message: String, callback: (String?) -> Unit) {
        synchronized(UI) {
            question = Question(title, message, callback)
        }
    }

    override fun showDialog(widget: Widget) {
        synchronized(UI) {
            dialogWidget = widget
        }
    }

    fun onReceiveChatMessage(sender: String,  message: String, color: Int) {
        val configIO = appKoin.get<ConfigIO>()
        synchronized(UI) {
            chatMessages =
                buildAnnotatedString {
                    if (sender == "RELAY_CN-ADMIN") {
                        val result = relayRegex.find(message)?.value

                        if (!result.isNullOrBlank()) {
                            configIO.setGameConfig("lastNetworkIP", result)
                        }
                    }

                    if (sender.isNotBlank()) {
                        withStyle(
                            style = SpanStyle(
                                color = Player.getTeamColor(color),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("$sender: ")
                        }
                    }

                    withStyle(style = SpanStyle(color = Color.White)) {
                        append(message)
                    }

                    append("\n")
                } + chatMessages
        }

    }

    /**
     * @see [showQuestion]
     */
    class Question(val title: String, val message: String, val callback: (String?) -> Unit)

    /**
     * @see [showWarning]
     */
    class Warning(val reason: String, val isKicked: Boolean = false)

    override fun init() {
        GlobalEventChannel.filter(DisconnectEvent::class).subscribeAlways(priority = EventPriority.MONITOR) {
            chatMessages = AnnotatedString("")
        }
    }
}

open class UIProvider {
    val extraMenuList = mutableListOf<Menu>()

    @Composable
    open fun MainMenu(
        state: LazyListState,
        multiplayer: () -> Unit,
        mission: () -> Unit,
        skirmish: () -> Unit,
        settings: () -> Unit,
        mods: () -> Unit,
        sandbox: () -> Unit,
        extension: () -> Unit,
        replay: () -> Unit,
        resourceBrowser: () -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 左侧竖排主菜单（月光列表），背景人物完整可见
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(340.dp)
                    .padding(start = 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val panelAlpha = UI.backgroundTransparency

                // 标题：花体（系统cursive）+ 深紫渐变——与浅色背景图互补
                Text(
                    "RWNoctiluna",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF4A4390), Color(0xFF7A6FC9))
                        ),
                        fontSize = 46.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    "$projectVersion (core $coreVersion)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF45407A).copy(alpha = (0.7f + panelAlpha / 4f).coerceAtMost(1f)),
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp, start = 4.dp)
                )

                LunarMenuItem("01", readI18n("menu.mission"), panelAlpha, painterResource(Res.drawable.destruction_30), onClick = mission)
                LunarMenuItem("02", readI18n("menus.singlePlayer.skirmish", I18nType.RW), panelAlpha, painterResource(Res.drawable.swords_30), onClick = skirmish)
                LunarMenuItem("03", readI18n("menu.multiplayer"), panelAlpha, painterResource(Res.drawable.group_30), onClick = multiplayer)

                LunarMenuItem("04", readI18n("menu.mods"), panelAlpha, painterResource(Res.drawable.dns_30), onClick = mods)
                LunarMenuItem("05", readI18n("menu.sandbox"), panelAlpha, painterResource(Res.drawable.edit_square_30), onClick = sandbox)
                LunarMenuItem("06", readI18n("browser.resourceBrowser"), panelAlpha, painterResource(Res.drawable.public_30), onClick = resourceBrowser)
                LunarMenuItem("07", readI18n("menu.extension"), panelAlpha, painterResource(Res.drawable.extension_30), onClick = extension)
                LunarMenuItem("08", readI18n("menu.replay"), panelAlpha, Icons.Default.PlayArrow, onClick = replay)
                LunarMenuItem("09", readI18n("menu.settings"), panelAlpha, Icons.Default.Settings, onClick = settings)

                extraMenuList.forEachIndexed { index, it ->
                    LunarMenuItem((10 + index).toString(), it.title, panelAlpha, it.iconModel, onClick = it.onClick)
                }
            }
        }

        // 右上角覆盖层：公告 / GitHub / 关于 / 退出（本土化：删赞助与QQ群入口）
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                val net = koinInject<Net>()

                var showAnnouncement by remember { mutableStateOf(false) }
                RWIconButton(Icons.Filled.Info, modifier = Modifier.padding(4.dp)) {
                    showAnnouncement = true
                }
                if (showAnnouncement) {
                    AnnouncementDialog { showAnnouncement = false }
                }

                RWIconButton(painterResource(Res.drawable.octocat_30), modifier = Modifier.padding(4.dp)) {
                    net.openUriInBrowser("https://github.com/Minxyzgo/RWPP")
                }

                var showAbout by remember { mutableStateOf(false) }
                RWIconButton(Icons.Filled.Favorite, modifier = Modifier.padding(4.dp), tint = Color.Red) {
                    showAbout = true
                }
                if (showAbout) {
                    AboutDialog { showAbout = false }
                }

                with(koinInject<AppContext>()) {
                    RWIconButton(painterResource(Res.drawable.exit_30), modifier = Modifier.padding(4.dp)) {
                        exit()
                    }
                }
            }
        }
    }

    class Menu(
        val title: String,
        val iconModel: Any?,
        val onClick: () -> Unit
    )
}

