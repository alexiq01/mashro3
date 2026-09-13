package com.example.subscriptions.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

object SkylineNetworkDetector {
    // Replace with Skyline's verified DHCP ranges when provided by the network administrator.
    val SKYLINE_IP_RANGES = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")
    fun isSkylineNetwork(): Boolean { val ip = getLocalIpv4() ?: return false; return SKYLINE_IP_RANGES.any { isIpInCidr(ip, it) } }
    suspend fun confirmSkylineGateway(): Boolean = withContext(Dispatchers.IO) { try { (URL("http://admin.skylineiq.com/user/api/index.php/api/resources/logo").openConnection() as HttpURLConnection).run { connectTimeout=3000; readTimeout=3000; requestMethod="HEAD"; responseCode in 200..399 } } catch (_: Exception) { false } }
    private fun getLocalIpv4(): String? = try { NetworkInterface.getNetworkInterfaces().toList().asSequence().filter { it.isUp && !it.isLoopback }.flatMap { it.inetAddresses.toList().asSequence() }.filterIsInstance<Inet4Address>().firstOrNull { !it.isLoopbackAddress }?.hostAddress } catch (_: Exception) { null }
    private fun isIpInCidr(ip: String, cidr: String): Boolean { val parts=cidr.split('/'); val prefix=parts[1].toInt(); val mask=if(prefix==0)0 else (-1 shl (32-prefix)); return (ipToInt(ip) and mask)==(ipToInt(parts[0]) and mask) }
    private fun ipToInt(ip: String): Int = ip.split('.').map(String::toInt).fold(0) { acc, part -> (acc shl 8) or part }
}
