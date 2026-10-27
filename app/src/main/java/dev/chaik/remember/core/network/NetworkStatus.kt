package dev.chaik.remember.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkType { WIFI, CELLULAR, ETHERNET, OTHER, NONE }

/** Через что сейчас интернет. [operator] — имя оператора для мобильной сети. */
data class NetworkStatus(
    val type: NetworkType,
    val vpn: Boolean = false,
    /** Трафик платный (мобильная сеть, точка доступа) — перед замером предупредим. */
    val metered: Boolean = false,
    val operator: String? = null,
)

/** Текущая сеть телефона и её смена — для страницы «Сеть». */
class NetworkMonitor(private val context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun current(): NetworkStatus = status(connectivity.getNetworkCapabilities(connectivity.activeNetwork))

    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(status(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(current())
            }
        }
        trySend(current())
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun status(caps: NetworkCapabilities?): NetworkStatus {
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return NetworkStatus(NetworkType.NONE)
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
        val operator = if (type == NetworkType.CELLULAR) {
            context.getSystemService(TelephonyManager::class.java)?.networkOperatorName?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        return NetworkStatus(
            type = type,
            vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            operator = operator,
        )
    }
}
