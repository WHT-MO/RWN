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
 * 迷雾巡逻反作弊 v4：基线完整性巡逻 + 自研独立重扫。
 *
 * 全链排查结论（源码+运行时+UI 三重验证）：
 * - 房间 fog 设置(GameMapData.fog 0/1/2)经网络包分发，加载时译码为地图实例
 *   E(迷雾总开关)/F(原生重扫允许)；G 为 revealedMap(开局全图揭示)。
 *   UI 实锤：0=No Fog / 1=Basic Fog / 2=LOS Fog。
 * - 社区主玩法"浓雾"= LOS Fog(fog=2, E=F=true)：原生 260s rescan 在跑并
 *   维护 127 探索记忆；CE 改 N 数组的透视本应被周期压回——"透视持续
 *   一整局"的剩余解释是 E/G 翻转（判定短路 + rescan 门控连坐失效）。
 * - E/F/G 合法写入仅在加载译码；每次对局/回放加载创建新地图实例。
 *
 * 三层防御（全部本机玩家本地数据，零联机同步风险）：
 * 1. 基线巡逻：实例首 tick 记录 E/F/G 基线（兼容无雾房/沙盒/revealedMap/
 *    Basic Fog 的合法组合），翻转立即写回——主防线，封死开关翻转型。
 * 2. 自研独立重扫：复刻原生 rescan 完整序列（见 resetLocalFog），不等
 *   260s、不经门控；开局 30s 突发 1.5s（CE 改视野仅开局同步生效，
 *   重进=新实例=自动再突发），稳态按档位（严格20s/标准65s）。
 * 3. 采样检测：每秒可见格计数，突变立即触发重扫。
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
        val level = settings.fogPatrolLevel
        val multiplayer = runCatching { !game.gameRoom.isSinglePlayerGame }.getOrDefault(false)
        if (!multiplayer && level == "关闭") return Unit

        // 1. 基线巡逻：E/F/G 翻转立即写回。
        //    位于 delta 检查之前——暂停(delta=0)时计时器不走，
        //    但开关翻转型必须立即防御。
        val base = baseline.getOrPut(this) { Baseline(E, F, G) }
        if (E != base.e) E = base.e
        if (F != base.f) F = base.f
        if (G != base.g) G = base.g

        // 无雾基线（E=false）下引擎不维护 N 数组，无物可防
        if (!E) return Unit
        // 观战者守卫：本机无单位（观战队/部队全灭）时引擎保持全图观战态
        // （N=0 特殊值）——巡逻的压回(resetLocalFog)会把 0 改写成普通迷雾值 5，
        // 破坏观战视野（实测：地形轮廓在但单位全被迷雾遮住）。此类玩家
        // 无透视作弊动机与意义，整体跳过巡逻。
        if (localPlayerHasNoUnits()) return Unit
        // 暂停/零步长：跳过计时器累加
        if (delta <= 0f) return Unit

        val age = (mapAge[this] ?: 0f) + delta
        mapAge[this] = age
        val steady = if (multiplayer || level == "严格") STEADY_STRICT else STEADY_STANDARD
        val interval = if (age < BURST_DURATION) BURST_INTERVAL else steady

        // 2. 周期独立重扫
        val t = (rescanTimer[this] ?: 0f) + delta
        if (t >= interval) {
            rescanTimer[this] = 0f
            resetLocalFog(this)
        } else {
            rescanTimer[this] = t
        }

        // 3. 采样检测（突发期本身高频重扫，无需采样）。
        // 注：LOS Fog 下盟友推图会经视野共享让自己的可见格数合法暴涨，
        // 突变检测区分度不足，仅作深度防御：突变阈值收敛为唯一强特征
        // （可见格占全图过半，正常推进打不满），且触发后果仅是多一次
        // 与原生等价的重扫，误报无害。
        if (age >= BURST_DURATION) {
            val s = (sampleTimer[this] ?: 0f) + delta
            if (s >= SAMPLE_PERIOD) {
                sampleTimer[this] = 0f
                if (visibleCountSpike()) resetLocalFog(this)
            } else {
                sampleTimer[this] = s
            }
        }
        return Unit
    }

    /**
     * 独立重扫：复刻原生重扫（LOS Fog 的 260s rescan）完整动作序列，
     * 不经 E&&F 门控、不依赖 ar：
     * 1. 活跃单位 g(本机玩家)     —— 更新可见性缓存
     * 2. 本机玩家 N 可见格(<5)压回 5；本机玩家(q())同时写地图层 127
     *    （已探索记忆）+ 渲染缓存失效 al.a(x,y,true)——LOS Fog 探索
     *    记忆观感与原生一致，不可省略
     * 3. 存活单位 c(false) 重新揭示（事件驱动，静止单位必须主动补）
     * 4. 活跃单位 cX() 清理迷雾边缘幽灵
     * 5. 标记重绘
     * 单位列表：沿 am 继承链反射找字段 er(fastGameObjectList) 调 a()。
     */
    private fun resetLocalFog(map: b) {
        runCatching {
            val local = game.gameRoom.localPlayer as? com.corrodinggames.rts.game.n ?: return
            val fog = local.N ?: return
            val mapExplored = map.N
            val isLocalTeam = local.q()
            // 原生 rescan 仅处理 OrderableUnit（过滤树/装饰物），照抄
            val units = allUnits().filterIsInstance<com.corrodinggames.rts.game.units.y>()

            // 1. 更新可见性缓存（原生：instanceof + bI() 检查）
            for (u in units) if (u.bI()) u.g(local)

            // 2. 压回 + 127 记忆（本机玩家），照抄原生 rescan 压回段。
            //    维度防护：玩家数组(L/M)与地图层 N 维度在换图/重连时序下
            //    可能不一致，索引前校验列长度，越界则跳过该列的 127 写入。
            for (x in fog.indices) {
                val col = fog[x] ?: continue
                val exploredCol = if (isLocalTeam) mapExplored?.getOrNull(x) else null
                val exploredSafe = isLocalTeam && exploredCol != null && exploredCol.size >= col.size
                for (y in col.indices) {
                    if (col[y] < VISIBLE_LIMIT) {
                        col[y] = VISIBLE_LIMIT
                        if (exploredSafe) {
                            com.corrodinggames.rts.game.b.b.al.a(x, y, true)
                            exploredCol!![y] = EXPLORED_MEMORY
                        }
                    }
                }
            }

            // 3. 重新揭示 + 4. 幽灵清理
            for (u in units) {
                if (!u.bV) u.c(false)
                if (u.bI()) u.cX()
            }

            // 5. 重绘（原生 rescan 有 127 写入时同样仅置 bW.O）
            GameEngine.B().bW.O = true
        }
    }

    /** 全局单位列表（GameObject.er → 反射沿 am 继承链找字段，调用其 a()） */
    private val unitListField by lazy {
        var c: Class<*>? = com.corrodinggames.rts.game.units.am::class.java
        while (c != null) {
            val f = runCatching { c!!.getDeclaredField("er").apply { isAccessible = true } }.getOrNull()
            if (f != null) return@lazy f
            c = c.superclass
        }
        null
    }

    private fun allUnits(): List<com.corrodinggames.rts.game.units.am> = runCatching {
        val listObj = unitListField?.get(null) ?: return emptyList()
        val arr = listObj::class.java.getMethod("a").invoke(listObj) as? Array<*> ?: return emptyList()
        arr.filterIsInstance<com.corrodinggames.rts.game.units.am>()
    }.getOrDefault(emptyList())

    /** 本机玩家当前是否没有任何单位（观战队/部队全灭；owner=引擎bX字段） */
    private fun localPlayerHasNoUnits(): Boolean = runCatching {
        val local = game.gameRoom.localPlayer ?: return false
        allUnits().none { it.bX === local }
    }.getOrDefault(false)

    /**
     * 可见格计数突变检测（深度防御）。
     * 阈值收敛为唯一强特征：可见格占全图过半——CE 全图透视必然满足，
     * 正常玩法（自身+盟友推图）在 LOS Fog 下打不满；零可见格玩家
     * （无单位无建筑，原版即全迷雾）被 last==0 守卫排除，不会误判。
     */
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

            // 仅保留强特征：可见格占全图过半（3/10 阈值会被盟友推图触发）
            visible > total / 2
        }.getOrDefault(false)
    }

    private const val BURST_DURATION = 30.0f
    private const val BURST_INTERVAL = 1.5f
    private const val STEADY_STRICT = 20.0f
    private const val STEADY_STANDARD = 65.0f
    private const val SAMPLE_PERIOD = 1.0f
    private const val VISIBLE_LIMIT = 5.toByte()
    /** 地图已探索层标记（LOS Fog 的探索记忆） */
    private const val EXPLORED_MEMORY = 127.toByte()
}
