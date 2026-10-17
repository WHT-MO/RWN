/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.stats

import io.github.rwpp.appKoin
import io.github.rwpp.event.events.GameOverEvent
import io.github.rwpp.game.Game
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import net.peanuuutz.tomlkt.Toml
import java.io.File

/**
 * 对局统计：多人对局结束时记录一条（单机/回放不记）。
 * 持久化到游戏目录 match_history.json（独立于 TOML 配置——历史无限追加）。
 * 上限 [MAX_RECORDS] 条，超出裁掉最旧的。
 */
object MatchStatistics {
    @Serializable
    data class MatchRecord(
        val timestamp: Long,
        val durationSec: Int,
        val mapName: String,
        val won: Boolean,
        val playerCount: Int,
    )

    private const val MAX_RECORDS = 200

    @Volatile
    var records: List<MatchRecord> = emptyList()
        private set

    private val toml = Toml
    private val serializer = ListSerializer(MatchRecord.serializer())
    private val file: File
        get() = File(System.getProperty("user.dir"), "match_history.toml")

    fun load() {
        runCatching {
            if (file.exists()) {
                records = toml.decodeFromString(serializer, file.readText())
            }
        }
    }

    /** GameOverEvent 订阅入口（仅多人对局，且本机玩家实际参战） */
    fun onGameOver(e: GameOverEvent) {
        runCatching {
            val room = appKoin.get<Game>().gameRoom
            if (room.isSinglePlayerGame) return
            // 观战者不记：观战局非"我的对局"，且 team=-3 永远判败会污染胜率
            if (room.localPlayer.isSpectator) return
            append(
                MatchRecord(
                    timestamp = System.currentTimeMillis(),
                    durationSec = e.gameTime / 1000,
                    mapName = room.selectedMap.displayName(),
                    won = e.winTeam == room.localPlayer.team,
                    playerCount = room.getAllPlayers().count { it != null },
                )
            )
        }
    }

    private fun append(record: MatchRecord) {
        records = (records + record).takeLast(MAX_RECORDS)
        runCatching {
            file.writeText(toml.encodeToString(serializer, records))
        }
    }
}
