import com.mucheng.mucute.relay.definition.Definitions
import com.mucheng.mucute.relay.listener.NecessaryPacketListener
import com.mucheng.mucute.relay.util.captureMuCuteRelay
import com.mucheng.mucute.relay.util.fetchAuthSession
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Paths

fun main() {
    val path = Paths.get(".").resolve("auth.json")
    val authSession = fetchAuthSession(path.toFile())
    val remoteAddress = InetSocketAddress("sg.hivebedrock.network", 19132)

    Definitions.loadBlockPalette()
    captureMuCuteRelay(
        remoteAddress = remoteAddress,
        authSession = authSession
    ) {
        listeners.add(NecessaryPacketListener(this))
    }

    println("MuCuteRelay started on 0.0.0.0:19132")
    println("RemoteAddress: ${remoteAddress.hostName}:${remoteAddress.port}")
}