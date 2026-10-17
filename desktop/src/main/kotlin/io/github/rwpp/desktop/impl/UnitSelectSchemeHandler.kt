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

    /** 诊断日志（已退役）：功能验证完成，置空避免按键级同步写盘 */
    private fun diag(msg: String) {}

    @Volatile
    private var installed = false
    /** 引擎输入锁定标志（bS 实例的 "T" 字段，混淆名直接从实例反射，不依赖类名） */
    private val engineInputLockedField by lazy {
        runCatching {
            GameEngine.B().bS.javaClass.getDeclaredField("T").apply { isAccessible = true }
        }.getOrNull()
    }
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
        // 三通道监听（逐级兜底）：
        // 1. Toolkit AWTEventListener——事件队列最上游，与焦点组件无关
        //    （实测：引擎对局中焦点会转移到非 gameCanvas 组件，前两通道在此场景全聋）
        // 2. KeyEventDispatcher——全局分发（主菜单等 Compose 场景）
        // 3. gameCanvas KeyListener——组件级直挂
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener({ awtEvent ->
            if (awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                diag("queue key: ${keyNameOf(awtEvent)}")
                dispatcher.dispatchKeyEvent(awtEvent)
            }
        }, java.awt.AWTEvent.KEY_EVENT_MASK)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
        io.github.rwpp.desktop.gameCanvas.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                dispatcher.dispatchKeyEvent(e)
            }
        })
    }

    private val dispatcher = object : KeyEventDispatcher {
        override fun dispatchKeyEvent(e: KeyEvent): Boolean {
            if (e.id != KeyEvent.KEY_PRESSED) return false
            if (e.isControlDown || e.isAltDown || e.isMetaDown || e.isShiftDown) return false
            matchAndSelect(keyNameOf(e))
            return false // 不消费：同键的原生功能（若有）保持工作
        }
    }

    /**
     * 引擎键盘入口（终极通道）：RwInternalGameImpl.keyPressed 直通——
     * 引擎自身处理所有按键的必经之路（原生编队走的就是它），对局中必然活着。
     * 返回 true = 已被方案消费（RwInternalGameImpl 将跳过 super，不再传引擎
     * ——数字键场景必需：原生"编组N选中"会紧跟我们的选中清空选择，打架）
     * @param slickCode org.newdawn.slick.Input 键码
     */
    fun onEngineKey(slickCode: Int, keyChar: Char): Boolean {
        val keyName = when {
            keyChar.isLetterOrDigit() -> keyChar.uppercase()
            slickCode in org.newdawn.slick.Input.KEY_F1..org.newdawn.slick.Input.KEY_F12 ->
                "F${slickCode - org.newdawn.slick.Input.KEY_F1 + 1}"
            else -> return false
        }
        return matchAndSelect(keyName)
    }

    /** 键名 → 方案匹配 → 选中（全通道共用）；返回 true=方案命中并已选中 */
    private fun matchAndSelect(keyName: String): Boolean {
        val settings = runCatching { appKoin.get<Settings>() }.getOrNull() ?: return false
        if (!settings.enableUnitSelectSchemes) return false
        // 聊天/输入弹窗打开时跳过
        if (io.github.rwpp.desktop.sendMessageDialog.isVisible) return false
        // 引擎菜单/弹窗打开时跳过（对齐原生编队：ac=弹窗非空 或 T=输入锁定）
        runCatching {
            val bS = GameEngine.B().bS
            if (bS.ac != null) return false
            if (engineInputLockedField?.getBoolean(bS) == true) return false
        }

        val scheme = settings.unitSelectSchemes.firstOrNull { it.key.equals(keyName, ignoreCase = true) }
            ?: return false
        runCatching { selectSchemeUnits(scheme.unitTypes) }
        return true
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
            val all = arr.filterIsInstance<y>()
            val mine = all.filter { it.bX === local && !it.bV }
            // 归一化匹配：引擎/ini 双命名体系（内置枚举 tank vs 内容单位 c_tank）
            // 统一剥 c_ 前缀后比较——诊断日志实证场上是 c_tank、方案存 tank
            fun norm(n: String) = n.removePrefix("c_")
            val schemeNames = unitTypes.map { norm(it) }.toSet()
            val matched = mine.filter { schemeNames.contains(norm(it.dz.v())) }
            // 诊断：匹配失败时输出本机单位类型真名分布
            if (matched.isEmpty() && mine.isNotEmpty()) {
                val dist = mine.groupingBy { it.dz.v() }.eachCount()
                java.io.File("unit_scheme_diag.log").appendText(
                    "[diag] mine=${mine.size} types=$dist scheme=$unitTypes\n"
                )
            }
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
