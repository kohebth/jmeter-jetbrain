package com.github.kohebth.jmeterviewer.execution

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class JMeterResultBridgeTest {
    @Test
    fun acceptsAuthenticatedLengthFramedSamplesOnLoopback() {
        val journal = Files.createTempFile("jmeter-result-bridge", ".journal")
        val expected = "<sample t=\"1\"/>".toByteArray(StandardCharsets.UTF_8)
        val received = mutableListOf<ByteArray>()
        val latch = CountDownLatch(1)
        JMeterResultBridge("test-token", journal) { sample ->
            synchronized(received) { received.add(sample) }
            latch.countDown()
        }.use { bridge ->
            Socket(InetAddress.getLoopbackAddress(), bridge.port).use { socket ->
                DataOutputStream(BufferedOutputStream(socket.getOutputStream())).use { output ->
                    output.writeUTF("test-token")
                    writeFrame(output, "sample-1", expected)
                }
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS))
        }
        assertArrayEquals(expected, synchronized(received) { received.single() })
        Files.deleteIfExists(journal)
    }

    @Test
    fun replaysJournalSamplesThatWereNotReceivedLive() {
        val journal = Files.createTempFile("jmeter-result-bridge", ".journal")
        val expected = "<sample t=\"2\"/>".toByteArray(StandardCharsets.UTF_8)
        DataOutputStream(BufferedOutputStream(Files.newOutputStream(journal))).use { output ->
            writeFrame(output, "sample-2", expected)
        }
        val received = mutableListOf<ByteArray>()
        JMeterResultBridge("test-token", journal, received::add).use { bridge ->
            bridge.finishAndReplayJournal()
        }
        assertArrayEquals(expected, received.single())
        Files.deleteIfExists(journal)
    }

    private fun writeFrame(output: DataOutputStream, id: String, payload: ByteArray) {
        val idBytes = id.toByteArray(StandardCharsets.UTF_8)
        output.writeInt(idBytes.size)
        output.write(idBytes)
        output.writeInt(payload.size)
        output.write(payload)
        output.flush()
    }
}
