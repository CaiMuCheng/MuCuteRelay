package com.mucheng.mucute.relay.listener

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.mucheng.mucute.relay.MuCuteRelaySession
import com.mucheng.mucute.relay.definition.CameraPresetDefinition
import com.mucheng.mucute.relay.definition.DataEntry
import com.mucheng.mucute.relay.definition.Definitions
import com.mucheng.mucute.relay.util.ForgeryUtils
import com.mucheng.mucute.relay.util.ForgeryUtils.forgeMojangPublicKey
import com.mucheng.mucute.relay.util.ForgeryUtils.forgeOnlineAuthData
import com.mucheng.mucute.relay.util.ForgeryUtils.forgeOnlineSkinData
import net.raphimc.minecraftauth.step.bedrock.StepMCChain.MCChain
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat
import org.cloudburstmc.protocol.bedrock.codec.v291.Bedrock_v291
import org.cloudburstmc.protocol.bedrock.codec.v313.Bedrock_v313
import org.cloudburstmc.protocol.bedrock.codec.v332.Bedrock_v332
import org.cloudburstmc.protocol.bedrock.codec.v340.Bedrock_v340
import org.cloudburstmc.protocol.bedrock.codec.v354.Bedrock_v354
import org.cloudburstmc.protocol.bedrock.codec.v361.Bedrock_v361
import org.cloudburstmc.protocol.bedrock.codec.v388.Bedrock_v388
import org.cloudburstmc.protocol.bedrock.codec.v389.Bedrock_v389
import org.cloudburstmc.protocol.bedrock.codec.v390.Bedrock_v390
import org.cloudburstmc.protocol.bedrock.codec.v407.Bedrock_v407
import org.cloudburstmc.protocol.bedrock.codec.v408.Bedrock_v408
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419
import org.cloudburstmc.protocol.bedrock.codec.v422.Bedrock_v422
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428
import org.cloudburstmc.protocol.bedrock.codec.v431.Bedrock_v431
import org.cloudburstmc.protocol.bedrock.codec.v440.Bedrock_v440
import org.cloudburstmc.protocol.bedrock.codec.v448.Bedrock_v448
import org.cloudburstmc.protocol.bedrock.codec.v465.Bedrock_v465
import org.cloudburstmc.protocol.bedrock.codec.v471.Bedrock_v471
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475
import org.cloudburstmc.protocol.bedrock.codec.v486.Bedrock_v486
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544
import org.cloudburstmc.protocol.bedrock.codec.v545.Bedrock_v545
import org.cloudburstmc.protocol.bedrock.codec.v554.Bedrock_v554
import org.cloudburstmc.protocol.bedrock.codec.v557.Bedrock_v557
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567
import org.cloudburstmc.protocol.bedrock.codec.v568.Bedrock_v568
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575
import org.cloudburstmc.protocol.bedrock.codec.v582.Bedrock_v582
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594
import org.cloudburstmc.protocol.bedrock.codec.v618.Bedrock_v618
import org.cloudburstmc.protocol.bedrock.codec.v622.Bedrock_v622
import org.cloudburstmc.protocol.bedrock.codec.v630.Bedrock_v630
import org.cloudburstmc.protocol.bedrock.codec.v649.Bedrock_v649
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671
import org.cloudburstmc.protocol.bedrock.codec.v685.Bedrock_v685
import org.cloudburstmc.protocol.bedrock.codec.v686.Bedrock_v686
import org.cloudburstmc.protocol.bedrock.codec.v712.Bedrock_v712
import org.cloudburstmc.protocol.bedrock.codec.v729.Bedrock_v729
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventoryContentSerializer_v729
import org.cloudburstmc.protocol.bedrock.codec.v729.serializer.InventorySlotSerializer_v729
import org.cloudburstmc.protocol.bedrock.codec.v748.Bedrock_v748
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766
import org.cloudburstmc.protocol.bedrock.data.EncodingSettings
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition
import org.cloudburstmc.protocol.bedrock.packet.*
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import org.cloudburstmc.protocol.bedrock.util.JsonUtils
import org.cloudburstmc.protocol.common.NamedDefinition
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry
import org.jose4j.json.JsonUtil
import org.jose4j.json.internal.json_simple.JSONObject
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.HeaderParameterNames
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.util.*


@Suppress("MemberVisibilityCanBePrivate")
open class NecessaryPacketListener(
    val muCuteRelaySession: MuCuteRelaySession,
    private val patchCodec: Boolean = true
) : MuCuteRelayPacketListener {

    companion object {

        private val JSON_MAPPER = ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private val protocols by lazy {
            arrayOf(
                Bedrock_v291.CODEC, Bedrock_v313.CODEC, Bedrock_v332.CODEC,
                Bedrock_v340.CODEC, Bedrock_v354.CODEC, Bedrock_v361.CODEC,
                Bedrock_v388.CODEC, Bedrock_v389.CODEC, Bedrock_v390.CODEC,
                Bedrock_v407.CODEC, Bedrock_v408.CODEC, Bedrock_v419.CODEC,
                Bedrock_v422.CODEC, Bedrock_v428.CODEC, Bedrock_v431.CODEC,
                Bedrock_v440.CODEC, Bedrock_v448.CODEC, Bedrock_v465.CODEC,
                Bedrock_v471.CODEC, Bedrock_v475.CODEC, Bedrock_v486.CODEC,
                Bedrock_v503.CODEC, Bedrock_v527.CODEC, Bedrock_v534.CODEC,
                Bedrock_v544.CODEC, Bedrock_v545.CODEC, Bedrock_v554.CODEC,
                Bedrock_v557.CODEC, Bedrock_v560.CODEC, Bedrock_v567.CODEC,
                Bedrock_v568.CODEC, Bedrock_v575.CODEC, Bedrock_v582.CODEC,
                Bedrock_v589.CODEC, Bedrock_v594.CODEC, Bedrock_v618.CODEC,
                Bedrock_v622.CODEC, Bedrock_v630.CODEC, Bedrock_v649.CODEC,
                Bedrock_v662.CODEC, Bedrock_v671.CODEC, Bedrock_v685.CODEC,
                Bedrock_v686.CODEC, Bedrock_v712.CODEC, Bedrock_v729.CODEC,
                Bedrock_v748.CODEC, Bedrock_v766.CODEC
            ).associateBy { it.protocolVersion }
        }

        private fun pickProtocolCodec(
            protocolVersion: Int
        ): BedrockCodec {
            var bedrockCodec = BedrockCompat.CODEC
            for ((version, codec) in protocols) {
                if (version > protocolVersion) break
                bedrockCodec = codec
            }

            val helper = bedrockCodec.createHelper()
            helper.encodingSettings = EncodingSettings.UNLIMITED

            return bedrockCodec
                .toBuilder()
                .helper { helper }
                .build()
        }

        private fun verifyJwt(jwt: String, key: PublicKey): Boolean {
            val jws = JsonWebSignature()
            jws.key = key
            jws.compactSerialization = jwt

            return jws.verifySignature()
        }

        private val offlineKeyPair by lazy { EncryptionUtils.createKeyPair() }

    }

    private var skinData2: String? = null

    private var onlineKeyPair: KeyPair? = null

    private var mojangPublicKey: ECPublicKey? = null

    private var onlineLoginChain: List<String>? = null

    private fun patchCodecIfNeeded(codec: BedrockCodec): BedrockCodec {
        return if (patchCodec && codec.protocolVersion > 729) {
            codec.toBuilder()
                .updateSerializer(InventoryContentPacket::class.java, InventoryContentSerializer_v729.INSTANCE)
                .updateSerializer(InventorySlotPacket::class.java, InventorySlotSerializer_v729.INSTANCE)
                .build()
        } else {
            codec
        }
    }

    override fun beforeClientBound(packet: BedrockPacket): Boolean {
        if (packet is RequestNetworkSettingsPacket) {
            val protocolVersion = packet.protocolVersion
            val bedrockCodec = patchCodecIfNeeded(pickProtocolCodec(protocolVersion))
            muCuteRelaySession.server.codec = bedrockCodec

            val networkSettingsPacket = NetworkSettingsPacket()
            networkSettingsPacket.compressionThreshold = 0
            networkSettingsPacket.compressionAlgorithm = PacketCompressionAlgorithm.ZLIB

            muCuteRelaySession.clientBoundImmediately(networkSettingsPacket)
            muCuteRelaySession.server.setCompression(PacketCompressionAlgorithm.ZLIB)
            return true
        }
        if (packet is LoginPacket) {
            val chain = EncryptionUtils.validateChain(packet.chain)
            val payload: JsonNode = JSON_MAPPER.valueToTree(chain.rawIdentityClaims())

            if (payload.get("extraData").nodeType !== JsonNodeType.OBJECT) {
                throw RuntimeException("AuthData was not found!")
            }

            val extraData = JSONObject(
                JsonUtils.childAsType(
                    chain.rawIdentityClaims(), "extraData",
                    Map::class.java
                )
            )

            if (payload.get("identityPublicKey").nodeType !== JsonNodeType.STRING) {
                throw RuntimeException("Identity Public Key was not found!")
            }
            val identityPublicKey = EncryptionUtils.parseKey(payload.get("identityPublicKey").textValue())

            val clientJwt = packet.extra
            verifyJwt(clientJwt, identityPublicKey)

            val jws = JsonWebSignature()
            jws.compactSerialization = clientJwt

            val skinData = JSONObject(JsonUtil.parseJson(jws.unverifiedPayload))

            if (muCuteRelaySession.authSession == null) {
                initializeOfflineProxySession(packet.chain, extraData, skinData)
            } else {
                initializeOnlineProxySession(skinData)
            }
            return true
        }
        return false
    }

    private fun initializeOfflineProxySession(
        chainData: MutableList<String>,
        extraData: JSONObject,
        skinData: JSONObject
    ) {
        println("Initializing offline proxy session")
        muCuteRelaySession.muCuteRelay.connectToServer {
            println("Connected to server")

            val jwt = chainData[chainData.size - 1]
            val jws = JsonWebSignature()
            jws.compactSerialization = jwt

            val authData: String = ForgeryUtils.forgeOfflineAuthData(offlineKeyPair, extraData)
            skinData2 = ForgeryUtils.forgeOfflineSkinData(offlineKeyPair, skinData)

            chainData.removeLast()
            chainData.add(authData)

            println("Sending RequestNetworkSettingsPacket")
            val packet = RequestNetworkSettingsPacket()
            packet.protocolVersion = muCuteRelaySession.server.codec.protocolVersion
            muCuteRelaySession.serverBoundImmediately(packet)
        }
    }

    private fun initializeOnlineProxySession(skinData: JSONObject) {
        println("Initializing online proxy session")
        muCuteRelaySession.muCuteRelay.connectToServer {
            println("Connected to server")
            val mcChain: MCChain = muCuteRelaySession.authSession!!.mcChain

            if (mojangPublicKey == null) {
                mojangPublicKey = forgeMojangPublicKey()
            }
            onlineLoginChain = forgeOnlineAuthData(mcChain, mojangPublicKey)

            onlineKeyPair = KeyPair(mcChain.publicKey, mcChain.privateKey)

            skinData2 = forgeOnlineSkinData(
                muCuteRelaySession.authSession,
                skinData,
                muCuteRelaySession.remoteAddress
            )

            println("Sending RequestNetworkSettingsPacket")
            val packet = RequestNetworkSettingsPacket()
            packet.protocolVersion = muCuteRelaySession.server.codec.protocolVersion
            muCuteRelaySession.serverBoundImmediately(packet)
        }
    }

    override fun beforeServerBound(packet: BedrockPacket): Boolean {
        if (packet is NetworkSettingsPacket) {
            val threshold = packet.compressionThreshold
            if (threshold > 0) {
                muCuteRelaySession.client!!.setCompression(packet.compressionAlgorithm)
                println("Compression threshold set to $threshold")
            } else {
                muCuteRelaySession.client!!.setCompression(PacketCompressionAlgorithm.NONE)
                println("Compression threshold set to 0")
            }

            val loginPacket = LoginPacket()
            loginPacket.chain.addAll(onlineLoginChain!!)
            loginPacket.extra = skinData2
            loginPacket.protocolVersion = muCuteRelaySession.server.codec.protocolVersion
            muCuteRelaySession.serverBoundImmediately(loginPacket)
            return true
        }
        if (packet is ServerToClientHandshakePacket) {
            val jws = JsonWebSignature()
            jws.compactSerialization = packet.jwt
            val saltJwt = JSONObject(JsonUtil.parseJson(jws.unverifiedPayload))
            val x5u = jws.getHeader(HeaderParameterNames.X509_URL)
            val serverKey = EncryptionUtils.parseKey(x5u)
            val key = EncryptionUtils.getSecretKey(
                onlineKeyPair!!.private, serverKey,
                Base64.getDecoder().decode(JsonUtils.childAsType(saltJwt, "salt", String::class.java))
            )

            muCuteRelaySession.client!!.enableEncryption(key)
            muCuteRelaySession.serverBoundImmediately(ClientToServerHandshakePacket())
            return true
        }
        if (packet is StartGamePacket) {
            for (entry in packet.itemDefinitions) {
                Definitions.itemDataList.add(DataEntry(entry.identifier, entry.runtimeId))
                Definitions.legacyIdMap[entry.runtimeId] = entry.identifier
            }

            Definitions.itemDefinitions = SimpleDefinitionRegistry.builder<ItemDefinition>()
                .addAll(packet.itemDefinitions)
                .build()

            muCuteRelaySession.client!!.peer.codecHelper.itemDefinitions = Definitions.itemDefinitions
            muCuteRelaySession.server.peer.codecHelper.itemDefinitions = Definitions.itemDefinitions

            if (packet.isBlockNetworkIdsHashed) {
                muCuteRelaySession.client!!.peer.codecHelper.blockDefinitions = Definitions.blockDefinitionsHashed
                muCuteRelaySession.server.peer.codecHelper.blockDefinitions = Definitions.blockDefinitionsHashed
            } else {
                muCuteRelaySession.client!!.peer.codecHelper.blockDefinitions = Definitions.blockDefinitions
                muCuteRelaySession.server.peer.codecHelper.blockDefinitions = Definitions.blockDefinitions
            }

            // muCuteRelaySession.multiThreadEnabled = true
            return false
        }
        if (packet is CameraPresetsPacket) {
            val cameraDefinitions =
                SimpleDefinitionRegistry.builder<NamedDefinition>()
                    .addAll(List(packet.presets.size) {
                        CameraPresetDefinition.fromCameraPreset(packet.presets[it], it)
                    })
                    .build()

            muCuteRelaySession.client!!.peer.codecHelper.cameraPresetDefinitions = cameraDefinitions
            muCuteRelaySession.server.peer.codecHelper.cameraPresetDefinitions = cameraDefinitions
            return false
        }
        return false
    }

}