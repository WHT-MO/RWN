/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.config

import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Single
@Serializable
data class Settings(
    /**
     * Decide whether to allow game in full screen (Only PC)
     */
    var isFullscreen: Boolean = true,
    var showWelcomeMessage: Boolean? = null,
    var ignoreVersion: String? = null,
    var autoCheckUpdate: Boolean = false,
    var enhancedReinforceTroops: Boolean = true,
    var backgroundImagePath: String? = null,
    var selectedTheme: String? = null,
    var backgroundTransparency: Float = 0.7f,
    var backgroundImageTransparency: Float = 1f,
    var showBuildingAttackRange: Boolean = false,
    /** 选中己方侦察单位（类型名含 recon）时显示其视野圈 */
    var showScoutVision: Boolean = false,
    /** 迷雾巡逻反作弊档位：关闭=不巡逻, 标准=65s 重扫, 严格=20s 重扫（默认） */
    var fogPatrolLevel: String = "严格",
    var showExtraButton: Boolean = false,
    /** @see unitAttackRangeTypes */
    var showAttackRangeUnit: String = "Never",
    var enableAnimations: Boolean = true,
    var maxDisplayUnitGroupCount: Int = 7,
    var displayUnitGroupXOffset: Int = 0,
    var changeGameTheme: Boolean = false,
    var showUnitTargetLine: Boolean = false,
    var improvedHealthBar: Boolean = false,
    var mouseMoveView: Boolean = false,
    //var pathfindingOptimization: Boolean = false,
    var boldText: Boolean = false,
    var renderingBackend: String = "Default", // Default, Software, OpenGL
    var forceEnglish: Boolean = false,
    /** 单位方案选择快捷键（类型编组）：默认关闭 */
    var enableUnitSelectSchemes: Boolean = false,
    /** 工厂头顶显示待生产队列（仅己方工厂）：默认关闭 */
    var showFactoryQueue: Boolean = false,
    /** @see UnitSelectScheme */
    var unitSelectSchemes: List<UnitSelectScheme> = emptyList(),
) : Config {
    companion object {
        val unitAttackRangeTypes = listOf("Never", "Land", "Air", "All")
        val fogPatrolLevels = listOf("标准", "严格")
    }
}

/**
 * 单位方案：按单位类型集合绑快捷键，按键时实时选中本机玩家场上
 * 所有匹配类型的存活单位（含工厂新产出——按键时现查，非快照）。
 * 与原生 Ctrl+数字编组零接触（不碰引擎编组数组，只改当前选择）。
 */
@Serializable
data class UnitSelectScheme(
    var name: String,
    /** 键位（如 "Q"、"F5"——AWT KeyEvent 键名） */
    var key: String,
    /** 单位类型 ini 名集合 */
    var unitTypes: List<String> = emptyList(),
)
