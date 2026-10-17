/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.room

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import net.peanuuutz.tomlkt.Toml
import java.io.File

/**
 * 自定义房间预设：玩家自存自取的房间配置模板（仅房主在房间设置面板使用）。
 * 配置项为 applyRoomConfig 的玩家自定义子集（用户裁剪：不含最大玩家数/
 * 队伍单位上限/队伍锁定）；队伍分配按玩家槽位记录，应用时复用房主
 * 移动玩家指令（Player.applyConfigChange）逐个下发，同步由引擎保障。
 */
object RoomPresets {
    @Serializable
    data class Preset(
        val name: String,
        val sharedControl: Boolean,
        val startingCredits: Int,
        val startingUnits: Int,
        val fogMode: Int,          // FogMode.ordinal
        val aiDifficulty: Int,
        val incomeMultiplier: Float,
        val noNukes: Boolean,
        val allowSpectators: Boolean,
        val gameSpeed: Float,
        /** 按玩家槽位(0基)的队伍分配；空 = 不含队伍预设 */
        val teamChanges: List<TeamChange> = emptyList(),
    ) {
        @Serializable
        data class TeamChange(val slot: Int, val team: Int)
    }

    @Volatile
    var presets: List<Preset> = emptyList()
        private set

    private val toml = Toml
    private val serializer = ListSerializer(Preset.serializer())
    private val file: File
        get() = File(System.getProperty("user.dir"), "room_presets.toml")

    fun load() {
        runCatching {
            if (file.exists()) {
                presets = toml.decodeFromString(serializer, file.readText())
            }
        }
    }

    fun save(p: Preset) {
        presets = presets.filterNot { it.name == p.name } + p
        persist()
    }

    fun delete(name: String) {
        presets = presets.filterNot { it.name == name }
        persist()
    }

    private fun persist() {
        runCatching {
            file.writeText(toml.encodeToString(serializer, presets))
        }
    }
}
