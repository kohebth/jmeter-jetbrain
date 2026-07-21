package com.github.kohebth.jmeterviewer.execution

import com.intellij.util.concurrency.AppExecutorUtil
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections

internal class JMeterResultBridge(
    val token: String,
    val journalPath: Path,
    private val onSample: (ByteArray) -> Unit,
) : AutoCloseable {
    private val server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
    private val receivedIds = Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile
    private var closed = false

    val port: Int
        get() = server.localPort

    init {
        AppExecutorUtil.getAppExecutorService().execute(::acceptConnections)
    }

    private fun acceptConnections() {
        while (!closed) {
            try {
                server.accept().use(::readNetworkFrame)
            } catch (_: SocketException) {
                if (!closed) {
                    continue
                }
            } catch (_: Exception) {
                // The fallback journal is authoritative and is replayed when
                // the process terminates, so a malformed connection is safe.
            }
        }
    }

    private fun readNetworkFrame(socket: Socket) {
        socket.soTimeout = SOCKET_TIMEOUT_MS
        DataInputStream(BufferedInputStream(socket.getInputStream())).use { input ->
            if (input.readUTF() != token) {
                return
            }
            val (id, payload) = readFrame(input)
            if (receivedIds.add(id)) {
                onSample(payload)
            }
        }
    }

    fun finishAndReplayJournal() {
        close()
        if (!Files.isRegularFile(journalPath)) {
            return
        }
        DataInputStream(BufferedInputStream(Files.newInputStream(journalPath))).use { input ->
            while (true) {
                val frame = try {
                    readFrame(input)
                } catch (_: EOFException) {
                    break
                }
                if (receivedIds.add(frame.first)) {
                    onSample(frame.second)
                }
            }
        }
    }

    private fun readFrame(input: DataInputStream): Pair<String, ByteArray> {
        val idLength = input.readInt()
        require(idLength in 1..MAX_ID_BYTES) { "Invalid JMeter result id length: $idLength" }
        val idBytes = ByteArray(idLength)
        input.readFully(idBytes)
        val payloadLength = input.readInt()
        require(payloadLength in 1..MAX_SAMPLE_BYTES) {
            "Invalid JMeter result payload length: $payloadLength"
        }
        val payload = ByteArray(payloadLength)
        input.readFully(payload)
        return String(idBytes, StandardCharsets.UTF_8) to payload
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        server.close()
    }

    private companion object {
        const val MAX_ID_BYTES = 256
        const val MAX_SAMPLE_BYTES = 128 * 1024 * 1024
        const val SOCKET_TIMEOUT_MS = 30_000
    }
}
