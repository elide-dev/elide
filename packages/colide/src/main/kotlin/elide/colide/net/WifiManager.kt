package elide.colide.net

/**
 * WiFi Manager for Colide OS
 * 
 * Provides high-level WiFi connectivity through multiple backend implementations:
 * - Bridge mode: Uses external device (Pi/host) for WiFi via USB/Serial
 * - USB mode: Direct RTL8188EU/MT7601U USB dongle support
 * - Native mode: Intel AX211 PCIe WiFi (future)
 */
public object WifiManager {
    
    private var backend: WifiBackend = WifiBackend.NONE
    private var connected: Boolean = false
    private var currentNetwork: WifiNetwork? = null
    private var onPacketReceived: ((ByteArray) -> Unit)? = null
    
    /** Available WiFi backend implementations */
    public enum class WifiBackend {
        NONE,
        BRIDGE,     // USB/Serial bridge to external device
        USB_DONGLE, // Direct USB WiFi dongle (RTL8188EU)
        NATIVE      // Intel AX211 PCIe (future)
    }
    
    /** Initialize WiFi subsystem with specified backend */
    public fun initialize(backend: WifiBackend): Boolean {
        this.backend = backend
        return when (backend) {
            WifiBackend.BRIDGE -> initBridge()
            WifiBackend.USB_DONGLE -> initUsbDongle()
            WifiBackend.NATIVE -> initNative()
            WifiBackend.NONE -> true
        }
    }
    
    /** Scan for available WiFi networks */
    public fun scan(): List<WifiNetwork> {
        return when (backend) {
            WifiBackend.BRIDGE -> bridgeScan()
            WifiBackend.USB_DONGLE -> usbScan()
            WifiBackend.NATIVE -> nativeScan()
            WifiBackend.NONE -> emptyList()
        }
    }
    
    /** Connect to a WiFi network */
    public fun connect(ssid: String, password: String = ""): Boolean {
        val result = when (backend) {
            WifiBackend.BRIDGE -> bridgeConnect(ssid, password)
            WifiBackend.USB_DONGLE -> usbConnect(ssid, password)
            WifiBackend.NATIVE -> nativeConnect(ssid, password)
            WifiBackend.NONE -> false
        }
        
        if (result) {
            connected = true
            currentNetwork = WifiNetwork(
                ssid = ssid,
                bssid = ByteArray(6),
                signal = 0,
                security = if (password.isEmpty()) WifiSecurity.OPEN else WifiSecurity.WPA2,
                channel = 0,
                frequency = 0
            )
        }
        return result
    }
    
    /** Disconnect from current network */
    public fun disconnect(): Boolean {
        val result = when (backend) {
            WifiBackend.BRIDGE -> bridgeDisconnect()
            WifiBackend.USB_DONGLE -> usbDisconnect()
            WifiBackend.NATIVE -> nativeDisconnect()
            WifiBackend.NONE -> true
        }
        
        if (result) {
            connected = false
            currentNetwork = null
        }
        return result
    }
    
    /** Get current connection status */
    public fun getStatus(): WifiStatus {
        return WifiStatus(
            connected = connected,
            network = currentNetwork,
            ipAddress = if (connected) getIpAddress() else null,
            signal = if (connected) getSignalStrength() else 0
        )
    }
    
    /** Send raw Ethernet frame */
    public fun sendFrame(frame: ByteArray): Boolean {
        if (!connected) return false
        return when (backend) {
            WifiBackend.BRIDGE -> bridgeSendFrame(frame)
            WifiBackend.USB_DONGLE -> usbSendFrame(frame)
            WifiBackend.NATIVE -> nativeSendFrame(frame)
            WifiBackend.NONE -> false
        }
    }
    
    /** Set callback for received frames */
    public fun setOnFrameReceived(callback: (ByteArray) -> Unit) {
        onPacketReceived = callback
    }
    
    /** Poll for received frame (non-blocking) */
    public fun receiveFrame(): ByteArray? {
        if (!connected) return null
        return when (backend) {
            WifiBackend.BRIDGE -> bridgeReceiveFrame()
            WifiBackend.USB_DONGLE -> usbReceiveFrame()
            WifiBackend.NATIVE -> nativeReceiveFrame()
            WifiBackend.NONE -> null
        }
    }
    
    // Bridge mode native calls
    private external fun initBridge(): Boolean
    private external fun bridgeScan(): List<WifiNetwork>
    private external fun bridgeConnect(ssid: String, password: String): Boolean
    private external fun bridgeDisconnect(): Boolean
    private external fun bridgeSendFrame(frame: ByteArray): Boolean
    private external fun bridgeReceiveFrame(): ByteArray?
    
    // USB dongle native calls
    private external fun initUsbDongle(): Boolean
    private external fun usbScan(): List<WifiNetwork>
    private external fun usbConnect(ssid: String, password: String): Boolean
    private external fun usbDisconnect(): Boolean
    private external fun usbSendFrame(frame: ByteArray): Boolean
    private external fun usbReceiveFrame(): ByteArray?
    
    // Native Intel WiFi calls (future)
    private external fun initNative(): Boolean
    private external fun nativeScan(): List<WifiNetwork>
    private external fun nativeConnect(ssid: String, password: String): Boolean
    private external fun nativeDisconnect(): Boolean
    private external fun nativeSendFrame(frame: ByteArray): Boolean
    private external fun nativeReceiveFrame(): ByteArray?
    
    // Utility native calls
    private external fun getIpAddress(): String?
    private external fun getSignalStrength(): Int
}
