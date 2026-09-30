package com.burton.sonos.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredSpeaker(
    val ip: String,
    val port: Int = 1400,
)

@Singleton
class SpeakerDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun discover(timeoutMs: Long = 2_400): List<DiscoveredSpeaker> {
        val found = linkedSetOf<String>()
        ssdpDiscover(timeoutMs, found)
        if (found.isEmpty()) {
            mdnsDiscover(timeoutMs.coerceAtMost(1_600), found)
        }
        return found.map { DiscoveredSpeaker(it) }
    }

    private suspend fun ssdpDiscover(timeoutMs: Long, found: MutableSet<String>) =
        withContext(Dispatchers.IO) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("burton-sonos-ssdp").apply {
                setReferenceCounted(false)
                acquire()
            }
            try {
                DatagramSocket(null).use { socket ->
                    socket.reuseAddress = true
                    socket.broadcast = true
                    socket.soTimeout = 250
                    socket.bind(InetSocketAddress(0))
                    val payload = SEARCH.toByteArray(Charsets.UTF_8)
                    val group = InetAddress.getByName(SSDP_HOST)
                    socket.send(DatagramPacket(payload, payload.size, group, SSDP_PORT))
                    socket.send(DatagramPacket(SEARCH_ALL.toByteArray(Charsets.UTF_8), SEARCH_ALL.length, group, SSDP_PORT))
                    val deadline = SystemClock.elapsedRealtime() + timeoutMs
                    val buffer = ByteArray(2048)
                    while (SystemClock.elapsedRealtime() < deadline) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                            if (!isSonos(message)) continue
                            hostFromLocation(message)?.let { found += it }
                            if (found.isNotEmpty()) return@withContext
                        } catch (_: SocketTimeoutException) {
                            if (found.isNotEmpty()) return@withContext
                        }
                    }
                }
            } finally {
                if (lock.isHeld) lock.release()
            }
        }

    private suspend fun mdnsDiscover(timeoutMs: Long, found: MutableSet<String>) {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) = Unit
            override fun onDiscoveryStarted(serviceType: String?) = Unit
            override fun onDiscoveryStopped(serviceType: String?) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                nsd.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) = Unit
                        override fun onServiceResolved(resolved: NsdServiceInfo?) {
                            val host = resolved?.host?.hostAddress ?: return
                            synchronized(found) { found += host }
                        }
                    },
                )
            }
        }
        try {
            nsd.discoverServices(SONOS_MDNS, NsdManager.PROTOCOL_DNS_SD, listener)
            delay(timeoutMs)
        } finally {
            runCatching { nsd.stopServiceDiscovery(listener) }
        }
    }

    private fun isSonos(message: String): Boolean {
        val lower = message.lowercase()
        return "sonos" in lower || "zoneplayer" in lower || "rincon" in lower
    }

    private fun hostFromLocation(message: String): String? {
        val location = message.lineSequence()
            .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?: return null
        val hostPort = location.substringAfter("://").substringBefore("/")
        return hostPort.substringBefore(":")
    }

    private companion object {
        const val SSDP_HOST = "239.255.255.250"
        const val SSDP_PORT = 1900
        const val SONOS_MDNS = "_sonos._tcp."
        const val SEARCH =
            "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 1\r\n" +
                "ST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n" +
                "\r\n"
        const val SEARCH_ALL =
            "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 1\r\n" +
                "ST: ssdp:all\r\n" +
                "\r\n"
    }
}
