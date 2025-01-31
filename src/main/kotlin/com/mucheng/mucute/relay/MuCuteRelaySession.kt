package com.mucheng.mucute.relay

import com.mucheng.mucute.relay.handler.SessionCloseHandler
import com.mucheng.mucute.relay.listener.MuCuteRelayPacketListener
import io.netty.util.ReferenceCountUtil
import io.netty.util.internal.PlatformDependent
import kotlinx.coroutines.*
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import org.cloudburstmc.netty.channel.raknet.RakReliability
import org.cloudburstmc.protocol.bedrock.BedrockClientSession
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockServerSession
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.Executors


class MuCuteRelaySession internal constructor(
    peer: BedrockPeer,
    subClientId: Int,
    val muCuteRelay: MuCuteRelay,
    val authSession: StepFullBedrockSession.FullBedrockSession?,
    val remoteAddress: InetSocketAddress
) {

    val server = ServerSession(peer, subClientId)

    var client: ClientSession? = null
        internal set(value) {
            value?.let {
                it.codec = server.codec
                it.peer.codecHelper.blockDefinitions = server.peer.codecHelper.blockDefinitions
                it.peer.codecHelper.itemDefinitions = server.peer.codecHelper.itemDefinitions

                var pair: Pair<BedrockPacket, Boolean>
                while (packetQueue.poll().also { packetPair -> pair = packetPair } != null) {
                    if (pair.second) {
                        it.sendPacketImmediately(pair.first)
                    } else {
                        it.sendPacket(pair.first)
                    }
                }
            }
            field = value
        }

    val listeners: MutableList<MuCuteRelayPacketListener> = ArrayList()

    private val packetQueue: Queue<Pair<BedrockPacket, Boolean>> = PlatformDependent.newMpscQueue()

    var multiThreadEnabled = false

    private val scope = CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher() + SupervisorJob() + CoroutineName("RakRelay"))

    fun clientBound(packet: BedrockPacket) {
        server.sendPacket(packet)
    }

    fun clientBoundImmediately(packet: BedrockPacket) {
        server.sendPacketImmediately(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        if (client != null) {
            client!!.sendPacket(packet)
        } else {
            packetQueue.add(packet to false)
        }
    }

    fun serverBoundImmediately(packet: BedrockPacket) {
        if (client != null) {
            client!!.sendPacketImmediately(packet)
        } else {
            packetQueue.add(packet to true)
        }
    }

    inner class ServerSession(peer: BedrockPeer, subClientId: Int) :
        BedrockServerSession(peer, subClientId) {

        init {
            packetHandler = SessionCloseHandler {
                println("Client disconnect: $it")
                try {
                    client?.disconnect()
                    listeners.forEach { listener ->
                        try {
                            listener.onDisconnect(it)
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }

        override fun onPacket(wrapper: BedrockPacketWrapper) {
            val packet = wrapper.packet
            ReferenceCountUtil.retain(packet)

            if (multiThreadEnabled) {
                scope.launch {
                    handlePacket(packet)
                }
            } else {
                handlePacket(packet)
            }
        }

        private fun handlePacket(packet: BedrockPacket) {
            listeners.forEach { listener ->
                try {
                    if (listener.beforeClientBound(packet)) {
                        return
                    }
                } catch (e: Throwable) {
                    println("Before client bound error: ${e.stackTraceToString()}")
                }
            }

            serverBound(packet)

            listeners.forEach { listener ->
                try {
                    listener.afterClientBound(packet)
                } catch (e: Throwable) {
                    println("After client bound error: ${e.stackTraceToString()}")
                }
            }
        }

    }

    inner class ClientSession(peer: BedrockPeer, subClientId: Int) :
        BedrockClientSession(peer, subClientId) {

        init {
            packetHandler = SessionCloseHandler {
                println("Server disconnect: $it")
                try {
                    server.disconnect()
                    listeners.forEach { listener ->
                        try {
                            listener.onDisconnect(it)
                        } catch (_: Throwable) {
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }

        override fun onPacket(wrapper: BedrockPacketWrapper) {
            val packet = wrapper.packet
            ReferenceCountUtil.retain(packet)

            if (multiThreadEnabled) {
                scope.launch {
                    handlePacket(packet)
                }
            } else {
                handlePacket(packet)
            }
        }

        private fun handlePacket(packet: BedrockPacket) {
            listeners.forEach { listener ->
                try {
                    if (listener.beforeServerBound(packet)) {
                        return
                    }
                } catch (e: Throwable) {
                    println("Before server bound error: ${e.stackTraceToString()}")
                }
            }

            clientBound(packet)

            listeners.forEach { listener ->
                try {
                    listener.afterServerBound(packet)
                } catch (e: Throwable) {
                    println("After server bound error: ${e.stackTraceToString()}")
                }
            }
        }

    }

}