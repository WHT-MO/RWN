/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.desktop.impl

import com.corrodinggames.rts.java.u
import org.newdawn.slick.GameContainer

class RwInternalGameImpl(str: String) : u(str) {

    override fun update(p0: GameContainer?, p1: Int) {
        try {
            super.update(p0, p1)
        } catch (_: Exception){}
    }


    override fun b() {
    }

    // 引擎键盘入口：单位方案快捷键（类型编组）在此通道触发——
    // AWT 三通道（Toolkit/Dispatcher/Canvas）实测在对局中收不到字母键事件，
    // 引擎走 Slick 输入管道，此处是所有按键的必经之路（原生编队同源）。
    override fun keyPressed(p0: Int, p1: Char) {
        super.keyPressed(p0, p1)
        runCatching { UnitSelectSchemeHandler.onEngineKey(p0, p1) }
    }
}