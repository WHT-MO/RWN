/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.desktop.impl

import com.corrodinggames.rts.game.units.y
import io.github.rwpp.appKoin
import io.github.rwpp.config.Settings
import io.github.rwpp.desktop.GameEngine
import io.github.rwpp.game.Game
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities

/**
 * 单位方案选择快捷键（类型编组）执行链：
 * AWT 全局键盘分发器捕获按键 → 匹配方案 → 实时枚举本机玩家场上
 * 存活且类型匹配的单位 → 引擎原生选中（j() 内建敌我/死亡/规则过滤）。
 *
 * 防误触：聊天 Dialog 可见时跳过（焦点隔离的双保险）；对局未开始不响应。
 * 与原生 Ctrl+数字编组零接触（不碰编组数组，只改当前选择）。
 */
object UnitSelectSchemeHandler {

    @Volatile
    private var installed = false
    /** 全局单位列表（与 FogPatrolInject 同源反射） */
    private val unitListField by lazy {
        var c: Class<*>? = com.corrodinggames.rts.game.units.am::class.java
        while (c != null) {
            val f = runCatching { c!!.getDeclaredField("er").apply { isAccessible = true } }.getOrNull()
            if (f != null) return@lazy f
            c = c.superclass
        }
        null
    }

    fun install() {
        if (installed) return
        installed = true
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(object : KeyEventDispatcher {
            override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                if (e.id != KeyEvent.KEY_PRESSED) return false
                val settings = appKoin.get<Settings>()
                if (!settings.enableUnitSelectSchemes) return false
                // 聊天/输入弹窗打开时跳过（AWT Dialog 抢焦点本已隔离，此为双保险）
                if (io.github.rwpp.desktop.sendMessageDialog.isVisible) return false
                if (e.isControlDown || e.isAltDown || e.isMetaDown || e.isShiftDown) return false

                val keyName = keyNameOf(e)
                val scheme = settings.unitSelectSchemes.firstOrNull { it.key.equals(keyName, ignoreCase = true) }
                    ?: return false

                SwingUtilities.invokeLater { selectSchemeUnits(scheme.unitTypes) }
                return false // 不消费：同键的原生功能（若有）保持工作
            }
        })
    }

    /** AWT KeyEvent → 方案键名（与设置页展示一致：单字符大写 / F1-F12 / 数字） */
    private fun keyNameOf(e: KeyEvent): String {
        val text = KeyEvent.getKeyText(e.keyCode)
        return when {
            text.length == 1 -> text.uppercase()
            else -> text
        }
    }

    /** 选中本机玩家场上所有匹配类型的存活单位（走引擎 j() 继承全部可选中规则） */
    private fun selectSchemeUnits(unitTypes: List<String>) {
        runCatching {
            val game = appKoin.get<Game>()
            val room = game.gameRoom
            // 仅对局中响应（房间界面/菜单的按键不触发）
            if (!room.isStartGame) return

            val local = room.localPlayer
            val listObj = unitListField?.get(null) ?: return
            val arr = listObj::class.java.getMethod("a").invoke(listObj) as? Array<*> ?: return
            val matched = arr.filterIsInstance<y>()
                .filter { !it.bV && it.bX === local && unitTypes.contains(it.dz.v()) }
            if (matched.isEmpty()) return

            val bS = GameEngine.B().bS
            // 清空当前选择（原生编组选中同款序列）
            bS.l()
            bS.y()
            // 逐个选中（j() 内建：敌方/死亡/共享控制规则过滤）
            matched.forEach { bS.j(it) }
        }
    }
}
