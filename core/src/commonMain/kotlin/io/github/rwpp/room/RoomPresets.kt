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
        /** 按玩家槽位(0基)的移动规则；空 = 不含队伍预设 */
        val teamChanges: List<TeamChange> = emptyList(),
    ) {
        /**
         * 一条移动规则：序号 slot 玩家 → 位置 targetSpawn（出生点，0基；-1=不换位置）
         * 与队伍 targetTeam（1基；独立于位置）。
         */
        @Serializable
        data class TeamChange(
            val slot: Int,
            val targetSpawn: Int = -1,
            val targetTeam: Int = 1,
        )
    }

    /**
     * 把规则解析为"玩家身份快照 + 最终位置/队伍"，解决多步交换的序号漂移：
     * 应用瞬间一次性解析（players 为当前房间玩家列表），之后交换基于
     * Player 引用，不受列表刷新影响。
     * 算法：串行处理，目标位被占则与占用者交换位置；占用者队伍由其自身
     * 规则决定（无规则则保持）；未出现在规则中的玩家若被挤出位置则移入空位。
     */
    fun resolveMoves(
        players: List<io.github.rwpp.game.Player>,
        rules: List<Preset.TeamChange>,
    ): List<Pair<io.github.rwpp.game.Player, Pair<Int, Int>>> {
        // 快照：每条规则锚定应用时刻的玩家（防前序交换污染序号）
        data class Move(val player: io.github.rwpp.game.Player, val spawn: Int, val team: Int)
        val moves = rules.mapNotNull { r ->
            val p = players.getOrNull(r.slot) ?: return@mapNotNull null
            if (r.targetSpawn < -1) return@mapNotNull null
            Move(p, r.targetSpawn, r.targetTeam.coerceAtLeast(1))
        }
        // 每玩家只保留首条规则（重复规则以第一条为准）
        val byPlayer = moves.associateBy { it.player }
        val occupied = HashMap<Int, io.github.rwpp.game.Player>() // spawn -> player（当前态，初始为实况）
        players.forEach { p ->
            if (p.spawnPoint in 0..9) occupied[p.spawnPoint] = p
        }
        val finalSpawn = HashMap<io.github.rwpp.game.Player, Int>()
        val finalTeam = HashMap<io.github.rwpp.game.Player, Int>()

        for (m in byPlayer.values) {
            if (m.spawn == -1) { // 只改队伍
                finalTeam[m.player] = m.team
                continue
            }
            val occupant = occupied[m.spawn]
            val oldSpawn = m.player.spawnPoint
            if (occupant == null || occupant == m.player) {
                // 空位或原地：直接移
                occupied.remove(oldSpawn)
                occupied[m.spawn] = m.player
                finalSpawn[m.player] = m.spawn
            } else {
                // 交换：占用者移到被移动者的原位置（若其原位无效则留给后续空位逻辑）
                occupied[oldSpawn] = occupant
                occupied[m.spawn] = m.player
                finalSpawn[m.player] = m.spawn
                if (oldSpawn in 0..9) finalSpawn[occupant] = oldSpawn
            }
            finalTeam[m.player] = m.team
        }
        return players.mapNotNull { p ->
            val sp = finalSpawn[p] ?: return@mapNotNull null
            val tm = finalTeam[p] ?: p.team
            p to (sp to tm)
        }
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
