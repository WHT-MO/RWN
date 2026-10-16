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
import io.github.rwpp.inject.Inject
import io.github.rwpp.inject.InjectClass
import io.github.rwpp.inject.InjectMode

/**
 * 迷雾巡逻反作弊。
 *
 * 游戏原生在地图类 b 的 f(float) 中累计 ar，超过 260 才做一次全图迷雾重扫
 * （重跑单位视野 -> 全部重置重上迷雾 -> 恢复合法视野），CE 等内存修改的
 * 视野值会在重扫时被重建覆盖，但原生间隔过长、作弊窗口大。
 *
 * 本注入在每 tick 前检查 ar：一旦超过巡逻阈值（65，约为原生的 1/4 间隔）
 * 就把 ar 抬到 261 迫使原生重扫在本 tick 触发。全程复用游戏自身逻辑，
 * 只改触发频率，不碰任何数据，零联机同步风险。
 */
@InjectClass(b::class)
object FogPatrolInject {
    private val settings by lazy { appKoin.get<Settings>() }
    private val arField by lazy {
        runCatching { b::class.java.getDeclaredField("ar").apply { isAccessible = true } }.getOrNull()
    }

    @Inject("f", injectMode = InjectMode.InsertBefore)
    fun b.onFogPatrol(delta: Float): Any {
        val field = arField
        if (settings.fogPatrol && field != null) {
            runCatching {
                if (field.getFloat(this) > PATROL_THRESHOLD) {
                    field.setFloat(this, NATIVE_RESCAN_TRIGGER)
                }
            }
        }
        return Unit
    }

    private const val PATROL_THRESHOLD = 65.0f
    private const val NATIVE_RESCAN_TRIGGER = 261.0f
}
