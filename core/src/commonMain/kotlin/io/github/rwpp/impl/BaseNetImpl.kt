/*
 * Copyright 2023-2025 RWPP contributors
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 * https://github.com/Minxyzgo/RWPP/blob/main/LICENSE
 */

package io.github.rwpp.impl

import io.github.rwpp.appKoin
import io.github.rwpp.config.MultiplayerPreferences
import io.github.rwpp.config.ServerConfig
import io.github.rwpp.config.ServerType
import io.github.rwpp.net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.core.component.get
import java.io.DataInputStream
import java.util.concurrent.TimeUnit

abstract class BaseNetImpl : Net {
    override val packetDecoders: MutableMap<Int, (DataInputStream) -> Packet> = mutableMapOf()
    override val listeners: MutableMap<Int, MutableList<(Client?, Packet) -> Boolean>> = mutableMapOf()
    override val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(Interceptor { chain ->
            val request = chain.request()
            request.newBuilder()
                .removeHeader("Accept-Encoding")
                .build()
            chain.proceed(request)
        })
        .readTimeout(5000L, TimeUnit.MILLISECONDS)
        .build()

    override val scope: CoroutineScope = CoroutineScope(SupervisorJob())
    override val bbsProtocols: MutableList<BBSProtocol> = mutableListOf()
    override val roomListProvider: MutableMap<String, suspend () -> List<RoomDescription>> = mutableMapOf()
    override val roomListHostProtocol: MutableMap<String, (maxPlayer: Int, isPublic: Boolean) -> String> = mutableMapOf()

    override fun init() {
        val allServerConfig = get<MultiplayerPreferences>().allServerConfig
        if (allServerConfig.none { it.name == "Official Room List" }) {
            allServerConfig.add(officialRoomList)
        }

        roomListHostProtocol["RCN"] = { maxPlayer, isPublic ->
            if (isPublic) {
                "Rnewupp$maxPlayer"
            } else {
                "Rnewp$maxPlayer"
            }
        }
    }
}


val officialRoomList: ServerConfig
    get() = ServerConfig(
        "http://gs1.corrodinggames.com/masterserver/1.4/interface?action=list&game_version=176&game_version_beta=false;http://gs4.corrodinggames.net/masterserver/1.4/interface?action=list&game_version=176&game_version_beta=false",
        "Official Room List",
        ServerType.RoomList,
        editable = false,
        customRoomHostProtocol = "RCN"
    )
