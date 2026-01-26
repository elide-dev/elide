package elide.colide.net

/** WiFi network information from scan results */
public data class WifiNetwork(
    val ssid: String,
    val bssid: ByteArray,
    val signal: Int,           // Signal strength in dBm (-30 to -90)
    val security: WifiSecurity,
    val channel: Int,
    val frequency: Int         // MHz (2412-2484 for 2.4GHz, 5180-5825 for 5GHz)
) {
    /** Signal quality as percentage (0-100) */
    val quality: Int
        get() = when {
            signal >= -50 -> 100
            signal >= -60 -> 80
            signal >= -70 -> 60
            signal >= -80 -> 40
            signal >= -90 -> 20
            else -> 0
        }
    
    /** Band description */
    val band: String
        get() = when {
            frequency < 3000 -> "2.4 GHz"
            frequency < 6000 -> "5 GHz"
            else -> "6 GHz"
        }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WifiNetwork) return false
        return ssid == other.ssid && bssid.contentEquals(other.bssid)
    }
    
    override fun hashCode(): Int {
        var result = ssid.hashCode()
        result = 31 * result + bssid.contentHashCode()
        return result
    }
    
    override fun toString(): String {
        val securityStr = when (security) {
            WifiSecurity.OPEN -> "Open"
            WifiSecurity.WEP -> "WEP"
            WifiSecurity.WPA -> "WPA"
            WifiSecurity.WPA2 -> "WPA2"
            WifiSecurity.WPA3 -> "WPA3"
        }
        return "$ssid ($band, $securityStr, ${quality}%)"
    }
}

/** WiFi security types */
public enum class WifiSecurity {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA3
}

/** Current WiFi connection status */
public data class WifiStatus(
    val connected: Boolean,
    val network: WifiNetwork?,
    val ipAddress: String?,
    val signal: Int
) {
    override fun toString(): String {
        return if (connected && network != null) {
            "Connected to ${network.ssid} (${ipAddress ?: "No IP"}, ${network.quality}%)"
        } else {
            "Disconnected"
        }
    }
}

/** WiFi event types for callbacks */
public sealed class WifiEvent {
    public data class Connected(val network: WifiNetwork) : WifiEvent()
    public data class Disconnected(val reason: String) : WifiEvent()
    public data class ScanComplete(val networks: List<WifiNetwork>) : WifiEvent()
    public data class SignalChanged(val signal: Int) : WifiEvent()
    public data class Error(val message: String) : WifiEvent()
}
