/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.desktop.impl.inject

import android.graphics.Paint
import android.graphics.RectF
import com.corrodinggames.rts.game.units.am
import io.github.rwpp.game.base.GamePaint
import io.github.rwpp.game.base.Rect
import io.github.rwpp.game.units.GameUnit
import io.github.rwpp.inject.Inject
import io.github.rwpp.inject.InjectClass
import io.github.rwpp.inject.InjectMode
import io.github.rwpp.inject.RedirectMethod


@InjectClass(am::class)
object UnitInject {
    @Inject("p", InjectMode.InsertBefore)
    fun am.onDraw(delta: Float) {
        (this as GameUnit).comp.onDraw(this, delta)
        if (delta >= 0f) runCatching { drawFactoryQueue() }
    }

    /**
     * 工厂头顶待生产队列（仅己方工厂，默认关）。
     * 工厂判定=按"队列字段存在性"而非类继承（内置 d.i 体系与 ini 内容单位
     * land_factory 等体系通吃——用户实证其工厂全是 ini 单位，不继承 d.i）。
     * 队列结构（javap 实证）：字段值 d.k(队列容器,公开字段c) → 条目 d.j.g=UnitType。
     */
    private fun am.drawFactoryQueue() {
        val settings = runCatching { io.github.rwpp.appKoin.get<io.github.rwpp.config.Settings>() }.getOrNull() ?: return
        if (!settings.showFactoryQueue) return
        // 严格己方（盟友也不显示——看不见盟友工厂内部，用户决策）
        val local = runCatching {
            io.github.rwpp.appKoin.get<io.github.rwpp.game.Game>().gameRoom.localPlayer
        }.getOrNull() ?: return
        if (bX !== local) return

        // 沿继承链找队列字段（内置工厂 z=d.k；ini 工厂可能字段名不同，
        // 兜底：任意类型为 d.k 的字段）
        var queueObj: Any? = null
        var cls: Class<*>? = javaClass
        outer@ while (cls != null && queueObj == null) {
            for (f in runCatching { cls!!.declaredFields }.getOrDefault(emptyArray())) {
                if (f.type == com.corrodinggames.rts.game.units.d.k::class.java) {
                    runCatching { f.isAccessible = true }
                    queueObj = runCatching { f.get(this) }.getOrNull()
                    if (queueObj != null) break@outer
                }
            }
            cls = cls.superclass
        }
        if (queueObj == null) return
        // 队列容器（d.k.c 公开）
        val list = runCatching { queueObj.javaClass.getField("c").get(queueObj) as? List<*> }.getOrNull() ?: return
        if (list.isEmpty()) return

        // 分组计数（条目.g=UnitType → e() 显示名），最多 3 类 + "等"
        val counts = LinkedHashMap<String, Int>()
        for (entry in list) {
            val type = (entry as? com.corrodinggames.rts.game.units.d.j)?.g ?: continue
            val name = runCatching { type.e() }.getOrNull() ?: continue
            counts[name] = (counts[name] ?: 0) + 1
        }
        if (counts.isEmpty()) return
        val text = counts.entries.take(3).joinToString(" ") { "${it.key}×${it.value}" } +
                if (counts.size > 3) " 等" else ""

        // 世界-相机偏移=屏幕坐标（视野圈同款算法），引擎画笔绘制，画建筑上方
        val unit = this as GameUnit
        val world = io.github.rwpp.appKoin.get<io.github.rwpp.game.Game>().world
        world.drawText(
            text,
            unit.x - world.cx - (text.length * 5f),
            unit.y - world.cy - 30f,
            queuePaint
        )
    }

    /** 半透明白字（RWPP 官方画笔工厂，UnitComp 同款姿势，一次构造） */
    private val queuePaint: io.github.rwpp.game.base.GamePaint by lazy {
        io.github.rwpp.appKoin.get<io.github.rwpp.game.base.BaseFactory>()
            .createPaint(226, 255, 255, 255, io.github.rwpp.game.base.GamePaint.Style.FILL)
    }

    @RedirectMethod(
        "a",
        "(FZ)V",
        "com.corrodinggames.rts.gameFramework.m.y",
        "a"
    )
    @Suppress("TYPE_MISMATCH")
    fun am.onDrawStoke(
        rectF: RectF, param: Paint
    ) {
        (this as GameUnit).comp.onDrawBar(this, rectF as Rect, param as GamePaint)
    }

}