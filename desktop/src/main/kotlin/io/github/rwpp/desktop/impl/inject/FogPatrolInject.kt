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
import io.github.rwpp.game.Game
import io.github.rwpp.inject.Inject
import io.github.rwpp.inject.InjectClass
import io.github.rwpp.inject.InjectMode
import java.util.WeakHashMap

/**
 * 迷雾巡逻反作弊 v2：爆发窗口 + 稳态档位 + 采样检测 + 多人对局强制。
 *
 * 1. 爆发窗口：地图实例刚创建时（进服/重进对局）重扫间隔压到 BURST_INTERVAL，
 *    因为 CE 改视野仅在开局同步时生效、中局修改须重进对局才可见——开局是
 *    唯一作弊窗口，而开局单位少、重扫成本几乎为零。
 * 2. 稳态档位：30 秒后回落到设置档位（标准65s/严格20s），作为情报有误时的保险丝。
 * 3. 采样检测：稳态期间每秒随机抽 SAMPLE_COUNT 个"可见"格子，用引擎原生
 *    a(int,int,PlayerData) 可见性判定比对当前迷雾值——单位移动/新单位揭示
 *    走引擎同步路径不会产生分歧；发现不一致（CE 特征）立即触发原生重扫。
 *    采样异常只触发重扫不产生任何处罚，最坏结果是多扫一次，零误报代价。
 * 4. 多人对局强制：多人中无论设置为何（含"关闭"/被篡改）一律执行严格档+采样，
 *    单机尊重玩家选择——UI 锁挡不住改配置文件，逻辑层强制才有效。
 */
@InjectClass(b::class)
object FogPatrolInject {
    private val settings by lazy { appKoin.get<Settings>() }
    private val game by lazy { appKoin.get<Game>() }

    private val arField by lazy {
        runCatching { b::class.java.getDeclaredField("ar").apply { isAccessible = true } }.getOrNull()
    }

    /** 每个地图实例的存活计时（游戏线程内访问，无需锁） */
    private val mapAge = WeakHashMap<b, Float>()

    /** 采样累计计时 */
    private val sampleTimer = WeakHashMap<b, Float>()

    @Inject("f", injectMode = InjectMode.InsertBefore)
    fun b.onFogPatrol(delta: Float): Any {
        val field = arField
        if (field == null || delta <= 0f) return Unit

        val age = (mapAge[this] ?: 0f) + delta
        mapAge[this] = age

        val multiplayer = runCatching { !game.gameRoom.isSinglePlayerGame }.getOrDefault(false)
        val userLevel = settings.fogPatrolLevel
        // 多人对局强制严格；单机且用户关闭则完全退出
        if (!multiplayer && userLevel == "关闭") return Unit

        val steadyThreshold = if (multiplayer || userLevel == "严格") 20.0f else 65.0f
        val threshold = if (age < BURST_DURATION) BURST_INTERVAL else steadyThreshold

        runCatching {
            if (field.getFloat(this) > threshold) {
                field.setFloat(this, NATIVE_RESCAN_TRIGGER)
            }
        }

        // 稳态采样检测（爆发窗口本身已是高频重扫，无需采样）
        if (age >= BURST_DURATION) {
            val t = (sampleTimer[this] ?: 0f) + delta
            if (t >= SAMPLE_PERIOD) {
                sampleTimer[this] = 0f
                if (sampleFogMismatch(this)) {
                    runCatching { field.setFloat(this, NATIVE_RESCAN_TRIGGER) }
                }
            } else {
                sampleTimer[this] = t
            }
        }
        return Unit
    }

    /**
     * 可见格计数突变检测：每秒统计本机玩家迷雾中"可见"格子总数。
     * CE 透视的本质是把敌方区域大面积改可见，可见格数会瞬间暴涨
     * （突变 >3 倍且超过全图 30%）；合法的视野变化（单位移动/交战）
     * 每秒只有小幅渐进变化。只读 N 数组自身维度，零字段映射风险。
     * 注意：地图 reveal 触发器也会造成突变——误触发一次原生重扫，
     * 与原生 260s 重扫对 reveal 的撤销行为一致，无新增伤害。
     */
    private fun sampleFogMismatch(map: b): Boolean {
        return runCatching {
            val localPlayer = game.gameRoom.localPlayer ?: return false
            // localPlayer 本身就是被接口化的原生 n 实例（javassist @SetInterfaceOn）
            @Suppress("UNCHECKED_CAST")
            val player = localPlayer as? com.corrodinggames.rts.game.n ?: return false
            val fog = player.N ?: return false

            var visible = 0
            var total = 0
            for (column in fog) {
                val col = column ?: continue
                total += col.size
                for (v in col) if (v < VISIBLE_LIMIT) visible++
            }
            if (total <= 0) return false

            val last = lastVisibleCount[map]
            lastVisibleCount[map] = visible
            if (last == null || last == 0) return false

            val ratio = visible.toFloat() / total
            (visible > last * 3 && ratio > 0.3f) || (visible - last) > total / 10
        }.getOrDefault(false)
    }

    /** 每个地图实例上次采样的可见格数 */
    private val lastVisibleCount = WeakHashMap<b, Int>()

    private const val BURST_DURATION = 30.0f
    private const val BURST_INTERVAL = 1.5f
    private const val NATIVE_RESCAN_TRIGGER = 261.0f
    private const val VISIBLE_LIMIT = 5.toByte()
    private const val SAMPLE_PERIOD = 1.0f
}
