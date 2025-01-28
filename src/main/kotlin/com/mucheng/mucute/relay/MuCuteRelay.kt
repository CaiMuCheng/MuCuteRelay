package com.mucheng.mucute.relay

import com.mucheng.mucute.relay.MuCuteRelaySession.ClientSession
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption
import org.cloudburstmc.netty.handler.codec.raknet.server.RakServerRateLimiter
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockPong
import org.cloudburstmc.protocol.bedrock.PacketDirection
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer
import java.net.InetSocketAddress
import kotlin.random.Random

class MuCuteRelay(
    private val localAddress: InetSocketAddress = InetSocketAddress("0.0.0.0", 19132),
    private val authSession: StepFullBedrockSession.FullBedrockSession? = null,
    private val advertisement: BedrockPong = DefaultAdvertisement
) {

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        val DefaultCodec: BedrockCodec by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Bedrock_v766.CODEC
        }

        val DefaultAdvertisement: BedrockPong by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BedrockPong()
                .edition("MCPE")
                .gameType("Survival")
                .version(DefaultCodec.minecraftVersion)
                .protocolVersion(DefaultCodec.protocolVersion)
                .motd("MuCuteRelay")
                .playerCount(0)
                .maximumPlayerCount(20)
                .subMotd("Join QQ Group: 542992134")
                .nintendoLimited(false)
        }

    }

    @Suppress("MemberVisibilityCanBePrivate")
    val isRunning: Boolean
        get() = channelFuture != null

    private var channelFuture: ChannelFuture? = null

    private var muCuteRelaySession: MuCuteRelaySession? = null

    fun capture(
        remoteAddress: InetSocketAddress = InetSocketAddress("geo.hivebedrock.network", 19132),
        onSessionCreated: MuCuteRelaySession.() -> Unit
    ) {
        if (isRunning) {
            return
        }

        advertisement
            .ipv4Port(localAddress.port)
            .ipv6Port(localAddress.port)



        ServerBootstrap()
            .group(NioEventLoopGroup())
            .channelFactory(RakChannelFactory.server(NioDatagramChannel::class.java))
            .option(RakChannelOption.RAK_ADVERTISEMENT, advertisement.toByteBuf())
            .option(RakChannelOption.RAK_GUID, Random.nextLong())
            .option(RakChannelOption.RAK_PACKET_LIMIT, Int.MAX_VALUE)
            .option(RakChannelOption.RAK_GLOBAL_PACKET_LIMIT, Int.MAX_VALUE)
            .option(RakChannelOption.RAK_FLUSH_INTERVAL, 1)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(object : BedrockChannelInitializer<MuCuteRelaySession.ServerSession>() {

                override fun createSession0(peer: BedrockPeer, subClientId: Int): MuCuteRelaySession.ServerSession {
                    return MuCuteRelaySession(peer, subClientId, this@MuCuteRelay, authSession, remoteAddress)
                        .also {
                            muCuteRelaySession = it
                            it.onSessionCreated()
                        }
                        .server
                }

                override fun initSession(session: MuCuteRelaySession.ServerSession) {

                }

                override fun preInitChannel(channel: Channel) {
                    channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.CLIENT_BOUND)
                    super.preInitChannel(channel)
                }

            })
            .localAddress(localAddress)
            .bind()
            .syncUninterruptibly()
            .also {
                it.channel().pipeline().remove(RakServerRateLimiter.NAME)
                channelFuture = it
            }
    }

    internal fun connectToServer(onSessionCreated: ClientSession.() -> Unit) {
        val clientGUID = Random.nextLong()
        Bootstrap()
            .group(NioEventLoopGroup())
            .channelFactory(RakChannelFactory.client(NioDatagramChannel::class.java))
            .option(RakChannelOption.RAK_PROTOCOL_VERSION, muCuteRelaySession!!.server.codec.raknetProtocolVersion)
            .option(RakChannelOption.RAK_GUID, clientGUID)
            .option(RakChannelOption.RAK_REMOTE_GUID, clientGUID)
            .option(RakChannelOption.RAK_MTU, 5980)
            .option(RakChannelOption.RAK_MTU_SIZES, arrayOf(5980, 2990, 1993))
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .handler(object : BedrockChannelInitializer<ClientSession>() {

                override fun createSession0(peer: BedrockPeer, subClientId: Int): ClientSession {
                    return muCuteRelaySession!!.ClientSession(peer, subClientId)
                }

                override fun initSession(clientSession: ClientSession) {
                    muCuteRelaySession!!.client = clientSession
                    onSessionCreated(clientSession)
                }

                override fun preInitChannel(channel: Channel) {
                    channel.attr(PacketDirection.ATTRIBUTE).set(PacketDirection.SERVER_BOUND)
                    super.preInitChannel(channel)
                }

            })
            .remoteAddress(muCuteRelaySession!!.remoteAddress)
            .connect()
            .syncUninterruptibly()
    }

    fun disconnect() {
        if (!isRunning) {
            return
        }

        channelFuture?.channel()?.also {
            it.close().syncUninterruptibly()
            it.parent().close().syncUninterruptibly()
        }
        channelFuture = null
        muCuteRelaySession = null
    }

}