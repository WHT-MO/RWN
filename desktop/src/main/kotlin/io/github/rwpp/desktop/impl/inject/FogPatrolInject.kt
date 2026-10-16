/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.desktop.impl.inject

import com.corrodinggames.rts.game.b.b
import io.github.rwpp.appKoin
import io.github.rwpp.config.Settings
import io.github.rwpp.desktop.GameEngine
import io.github.rwpp.game.Game
import io.github.rwpp.inject.Inject
import io.github.rwpp.inject.InjectClass
import io.github.rwpp.inject.InjectMode
import java.util.WeakHashMap

/**
 * 迷雾巡逻反作弊 v3：基线完整性巡逻 + 自研独立重扫。
 *
 * 全链排查结论（源码+运行时双重验证）：
 * - 房间 fog 设置经网络包(GameMapData.fog 0/1/2)在加载时译码为地图实例的
 *   E(迷雾总开关)/F(原生重扫允许)；G 为 revealedMap(开局全图揭示)。
 *   浓雾=1→E,F=true,false：原生 260s 重扫被 E&&F 门控**从不运行**，
 *   CE 改 N 数组（透视）或翻 E/G（判定短路）均可一整局有效。
 * - 每次对局/回放加载创建新地图实例(EngineLoad: bL = new class_484())，
 *   合法的 E/F/G 变更只发生在加载译码，无对局中途合法翻转。
 *
 * 三层防御（全部本机玩家本地数据，零联机同步风险）：
 * 1. 基线巡逻：实例首个 tick 记录 E/F/G 基线（含无雾房/沙盒/revealedMap
 *    的合法 false/true 值），此后每 tick 校验，翻转立即写回——封死翻开关型。
 * 2. 自研独立重扫：本机玩家 N 数组中 <5(可见) 的格子全部压回 5，标记重绘；
 *    合法视野由引擎逐 tick 揭示在下一 tick 自动重建。复刻原生重扫的核心
 *    动作但不经过 E&&F 门控——浓雾下也运行。开局 30s 突发间隔 1.5s
 *    （CE 改视野仅开局同步生效，重进=新实例=自动再突发），随后按档位。
 * 3. 采样检测：每秒统计可见格数，突变(>3倍且>30%全图，或单秒增量>10%)
 *    立即触发独立重扫。
 *
 * 淡雾(F=true)模式不在防护范围（用户确认无人游玩），原生重扫自会运行。
 */
@InjectClass(b::class)
object FogPatrolInject {
    private val settings by lazy { appKoin.get<Settings>() }
    private val game by lazy { appKoin.get<Game>() }

    private class Baseline(val e: Boolean, val f: Boolean, val g: Boolean)

    /** 每个地图实例的巡逻状态（游戏线程内访问，无需锁） */
    private val baseline = WeakHashMap<b, Baseline>()
    private val mapAge = WeakHashMap<b, Float>()
    private val rescanTimer = WeakHashMap<b, Float>()
    private val sampleTimer = WeakHashMap<b, Float>()
    private val lastVisible = WeakHashMap<b, Int>()

    @Inject("f", injectMode = InjectMode.InsertBefore)
    fun b.onFogPatrol(delta: Float): Any {
        if (delta <= 0f) return Unit
        val level = settings.fogPatrolLevel
        val multiplayer = runCatching { !game.gameRoom.isSinglePlayerGame }.getOrDefault(false)
        if (!multiplayer && level == "关闭") return Unit

        // 1. 基线巡逻：E/F/G 翻转立即写回
        val base = baseline.getOrPut(this) { Baseline(E, F, G) }
        if (E != base.e) E = base.e
        if (F != base.f) F = base.f
        if (G != base.g) G = base.g

        // 无雾基线（E=false）下引擎不维护 N 数组，无物可防
        if (!E) return Unit

        val age = (mapAge[this] ?: 0f) + delta
        mapAge[this] = age
        val steady = if (multiplayer || level == "严格") STEADY_STRICT else STEADY_STANDARD
        val interval = if (age < BURST_DURATION) BURST_INTERVAL else steady

        // 2. 周期独立重扫
        val t = (rescanTimer[this] ?: 0f) + delta
        if (t >= interval) {
            rescanTimer[this] = 0f
            resetLocalFog()
        } else {
            rescanTimer[this] = t
        }

        // 3. 采样检测（突发期本身高频重扫，无需采样）
        if (age >= BURST_DURATION) {
            val s = (sampleTimer[this] ?: 0f) + delta
            if (s >= SAMPLE_PERIOD) {
                sampleTimer[this] = 0f
                if (visibleCountSpike()) resetLocalFog()
            } else {
                sampleTimer[this] = s
            }
        }
        return Unit
    }

    /**
     * 独立重扫：把本机玩家 N 数组的可见格(<5)全部压回 5。
     * 与原生重扫同款动作；随后引擎逐 tick 揭示会在下一 tick 重建合法视野。
     */
    private fun resetLocalFog() {
        runCatching {
            val local = game.gameRoom.localPlayer as? com.corrodinggames.rts.game.n ?: return
            val fog = local.N ?: return
            for (column in fog) {
                val col = column ?: continue
                for (i in col.indices) {
                    if (col[i] < VISIBLE_LIMIT) col[i] = VISIBLE_LIMIT
                }
            }
            GameEngine.B().bW.O = true
        }
    }

    /** 可见格计数突变检测（CE 全图透视的典型特征） */
    private fun b.visibleCountSpike(): Boolean {
        return runCatching {
            val local = game.gameRoom.localPlayer as? com.corrodinggames.rts.game.n ?: return false
            val fog = local.N ?: return false

            var visible = 0
            var total = 0
            for (column in fog) {
                val col = column ?: continue
                total += col.size
                for (v in col) if (v < VISIBLE_LIMIT) visible++
            }
            if (total <= 0) return false

            val last = lastVisible[this]
            lastVisible[this] = visible
            if (last == null || last == 0) return false

            (visible > last * 3 && visible > total * 3 / 10) || (visible - last) > total / 10
        }.getOrDefault(false)
    }

    private const val BURST_DURATION = 30.0f
    private const val BURST_INTERVAL = 1.5f
    private const val STEADY_STRICT = 20.0f
    private const val STEADY_STANDARD = 65.0f
    private const val SAMPLE_PERIOD = 1.0f
    private const val VISIBLE_LIMIT = 5.toByte()
}
