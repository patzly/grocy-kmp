package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request

/** Real connections on the loopback device: the proxies and servers are plain sockets. */
class ProxyRoutingTest {

    private val servers = mutableListOf<ServerSocket>()

    @AfterTest
    fun closeServers() = servers.forEach { it.close() }

    @Test
    fun httpProxyGetsTheRequestWithTheServersName() {
        val proxy = server { socket, requests ->
            requests += socket.requestLine()
            socket.respondOk()
        }
        val routing = ProxyRouting({ NetworkProxy.Http("127.0.0.1", proxy.port) })

        val body = client(routing).get("http://grocy.invalid/api/system/info")

        assertEquals("ok", body)
        assertEquals(listOf("GET http://grocy.invalid/api/system/info HTTP/1.1"), proxy.requests)
    }

    @Test
    fun torGetsTheServersNameSoNoNameIsResolvedOnTheDevice() {
        val socks = server { socket, requests ->
            requests += socket.socks5ConnectTarget()
            socket.requestLine()
            socket.respondOk()
        }
        val routing = ProxyRouting(
            proxy = { NetworkProxy.Tor },
            torAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), socks.port),
        )

        val body = client(routing).get("http://grocyabcdefghijklmnop.onion/api/system/info")

        assertEquals("ok", body)
        assertEquals(listOf("grocyabcdefghijklmnop.onion:80"), socks.requests)
    }

    @Test
    fun torThatIsNotRunningFailsInsteadOfGoingDirectly() {
        val direct = server { socket, requests ->
            requests += socket.requestLine()
            socket.respondOk()
        }
        val closedPort = ServerSocket(0).use { it.localPort }
        val routing = ProxyRouting(
            proxy = { NetworkProxy.Tor },
            system = DirectSelector,
            torAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), closedPort),
        )

        assertFailsWith<IOException> { client(routing).get("http://127.0.0.1:${direct.port}/") }
        assertTrue(direct.requests.isEmpty())
    }

    @Test
    fun blockedProxyFailsBeforeAnythingConnects() {
        val direct = server { socket, requests ->
            requests += socket.requestLine()
            socket.respondOk()
        }
        val routing = ProxyRouting(proxy = { NetworkProxy.Blocked }, system = DirectSelector)

        assertFailsWith<ProxyBlockedException> {
            client(routing).get("http://127.0.0.1:${direct.port}/")
        }
        assertTrue(direct.requests.isEmpty())
    }

    @Test
    fun systemProxyIsUsedWithoutOwnProxy() {
        val direct = server { socket, requests ->
            requests += socket.requestLine()
            socket.respondOk()
        }
        val routing = ProxyRouting(proxy = { NetworkProxy.System }, system = DirectSelector)

        client(routing).get("http://127.0.0.1:${direct.port}/api")

        assertEquals(listOf("GET /api HTTP/1.1"), direct.requests)
    }

    @Test
    fun proxyIsReadOnceForTheWholeProcess() {
        var reads = 0
        val routing = ProxyRouting({
            reads++
            NetworkProxy.System
        })

        routing.select(URI("http://a.invalid/"))
        routing.select(URI("http://b.invalid/"))

        assertEquals(1, reads)
    }

    private class TestServer(val port: Int, val requests: List<String>)

    private fun server(handle: (Socket, MutableList<String>) -> Unit): TestServer {
        val socket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        servers += socket
        val requests = CopyOnWriteArrayList<String>()
        thread(isDaemon = true) {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: IOException) {
                    break
                }
                client.use { handle(it, requests) }
            }
        }
        return TestServer(socket.localPort, requests)
    }

    private fun client(routing: ProxyRouting) = OkHttpClient.Builder()
        .proxySelector(routing)
        .addInterceptor(routing)
        .callTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private fun OkHttpClient.get(url: String): String =
        newCall(Request.Builder().url(url).build()).execute().use { it.body.string() }

    private object DirectSelector : ProxySelector() {
        override fun select(uri: URI) = listOf(Proxy.NO_PROXY)

        override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) = Unit
    }
}

/** Reads the request's first line and its headers, returns the line. */
private fun Socket.requestLine(): String {
    val reader = getInputStream().bufferedReader()
    val line = reader.readLine()
    // The headers up to the empty line
    do {
        val header = reader.readLine()
    } while (!header.isNullOrEmpty())
    return line
}

private fun Socket.respondOk() {
    getOutputStream().write(
        "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray(),
    )
    getOutputStream().flush()
}

/** Answers a SOCKS5 greeting and CONNECT, returns the target as the client sent it. */
private fun Socket.socks5ConnectTarget(): String {
    val input = DataInputStream(getInputStream())
    val output = getOutputStream()
    check(input.readUnsignedByte() == 5)
    input.skipBytes(input.readUnsignedByte())
    output.write(byteArrayOf(5, 0))
    check(input.readUnsignedByte() == 5 && input.readUnsignedByte() == 1)
    input.readUnsignedByte()
    // Type 3 is a name; an address (1, 4) would mean the device resolved it itself
    val type = input.readUnsignedByte()
    val host = if (type == 3) {
        ByteArray(input.readUnsignedByte()).also(input::readFully).decodeToString()
    } else {
        "resolved address of type $type"
    }
    val port = input.readUnsignedShort()
    output.write(byteArrayOf(5, 0, 0, 1, 127, 0, 0, 1, 0, 80))
    output.flush()
    return "$host:$port"
}
