//! Unified WiFi Driver Traits for Colide OS
//!
//! Provides a common interface for all WiFi drivers (MT7601U, RTL8188EU, iwlwifi).
//! This enables driver-agnostic networking code.

use heapless::Vec as HVec;

/// Maximum networks in scan results
pub const MAX_SCAN_RESULTS: usize = 32;

/// Maximum SSID length
pub const MAX_SSID_LEN: usize = 32;

/// WiFi driver error types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WifiError {
    /// Driver not initialized
    NotInitialized,
    /// Hardware not found
    HardwareNotFound,
    /// Firmware load failed
    FirmwareError,
    /// USB communication error
    UsbError,
    /// PCIe communication error
    PcieError,
    /// Scan failed
    ScanFailed,
    /// Authentication failed
    AuthFailed,
    /// Association failed
    AssocFailed,
    /// WPA handshake failed
    WpaFailed,
    /// Connection timeout
    Timeout,
    /// Already connected
    AlreadyConnected,
    /// Not connected
    NotConnected,
    /// Invalid parameter
    InvalidParam,
    /// Buffer too small
    BufferTooSmall,
    /// Channel not supported
    InvalidChannel,
    /// Rate not supported
    InvalidRate,
    /// Hardware busy
    Busy,
    /// Generic I/O error
    IoError,
}

/// WiFi network security type
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum SecurityType {
    #[default]
    Open,
    Wep,
    WpaPsk,
    Wpa2Psk,
    Wpa3Sae,
    Enterprise,
}

/// WiFi band
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum WifiBand {
    #[default]
    Band2_4GHz,
    Band5GHz,
    Band6GHz,
}

/// WiFi channel width
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ChannelWidth {
    #[default]
    Width20MHz,
    Width40MHz,
    Width80MHz,
    Width160MHz,
}

/// Scan result for a single network
#[derive(Debug, Clone, Default)]
pub struct ScanResult {
    pub ssid: [u8; MAX_SSID_LEN],
    pub ssid_len: usize,
    pub bssid: [u8; 6],
    pub channel: u8,
    pub rssi: i8,
    pub security: SecurityType,
    pub band: WifiBand,
    pub ht_capable: bool,
    pub vht_capable: bool,
    pub he_capable: bool,
}

impl ScanResult {
    pub fn ssid_str(&self) -> &str {
        core::str::from_utf8(&self.ssid[..self.ssid_len]).unwrap_or("")
    }
}

/// Connection state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ConnectionState {
    #[default]
    Disconnected,
    Scanning,
    Authenticating,
    Associating,
    WpaHandshake,
    Connected,
    Disconnecting,
}

/// Connection info
#[derive(Debug, Clone, Default)]
pub struct ConnectionInfo {
    pub state: ConnectionState,
    pub ssid: [u8; MAX_SSID_LEN],
    pub ssid_len: usize,
    pub bssid: [u8; 6],
    pub channel: u8,
    pub rssi: i8,
    pub tx_rate_mbps: u16,
    pub rx_rate_mbps: u16,
}

/// Driver capabilities
#[derive(Debug, Clone)]
pub struct DriverCapabilities {
    pub name: &'static str,
    pub supports_2_4ghz: bool,
    pub supports_5ghz: bool,
    pub supports_6ghz: bool,
    pub supports_ht: bool,
    pub supports_vht: bool,
    pub supports_he: bool,
    pub supports_wpa3: bool,
    pub max_tx_power_dbm: u8,
}

impl Default for DriverCapabilities {
    fn default() -> Self {
        Self {
            name: "unknown",
            supports_2_4ghz: false,
            supports_5ghz: false,
            supports_6ghz: false,
            supports_ht: false,
            supports_vht: false,
            supports_he: false,
            supports_wpa3: false,
            max_tx_power_dbm: 20,
        }
    }
}

/// Unified WiFi driver trait
pub trait WifiDriver {
    /// Initialize the driver and hardware
    fn init(&mut self) -> Result<(), WifiError>;
    
    /// Deinitialize and release hardware
    fn deinit(&mut self) -> Result<(), WifiError>;
    
    /// Get driver capabilities
    fn capabilities(&self) -> &DriverCapabilities;
    
    /// Start a scan for networks
    fn scan(&mut self) -> Result<(), WifiError>;
    
    /// Get scan results (call after scan completes)
    fn get_scan_results(&self) -> Result<HVec<ScanResult, MAX_SCAN_RESULTS>, WifiError>;
    
    /// Connect to a network
    fn connect(&mut self, ssid: &[u8], password: Option<&str>) -> Result<(), WifiError>;
    
    /// Disconnect from current network
    fn disconnect(&mut self) -> Result<(), WifiError>;
    
    /// Get current connection info
    fn connection_info(&self) -> Result<ConnectionInfo, WifiError>;
    
    /// Get current connection state
    fn state(&self) -> ConnectionState;
    
    /// Set channel (for monitor mode or AP)
    fn set_channel(&mut self, channel: u8) -> Result<(), WifiError>;
    
    /// Get current channel
    fn channel(&self) -> u8;
    
    /// Set TX power (in dBm)
    fn set_tx_power(&mut self, power_dbm: u8) -> Result<(), WifiError>;
    
    /// Get current RSSI
    fn rssi(&self) -> i8;
    
    /// Send raw 802.11 frame
    fn send_frame(&mut self, frame: &[u8]) -> Result<(), WifiError>;
    
    /// Receive raw 802.11 frame (returns frame length, 0 if no frame)
    fn recv_frame(&mut self, buffer: &mut [u8]) -> Result<usize, WifiError>;
    
    /// Poll for events (call periodically)
    fn poll(&mut self) -> Result<(), WifiError>;
}

/// Driver type enum
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DriverType {
    Mt7601u,
    Rtl8188eu,
    Iwlwifi,
}

/// WiFi driver registry for dynamic driver selection
/// NOTE: Driver implementations will implement WifiDriver trait when ready
pub struct DriverRegistry {
    active: Option<DriverType>,
    state: ConnectionState,
}

impl DriverRegistry {
    /// Create new empty registry
    pub const fn new() -> Self {
        Self {
            active: None,
            state: ConnectionState::Disconnected,
        }
    }
    
    /// Probe for available hardware and initialize appropriate driver
    /// Returns the driver type if hardware is found
    pub fn probe(&mut self) -> Result<DriverType, WifiError> {
        // Check USB devices for WiFi dongles
        if let Some(driver_type) = self.probe_usb_wifi() {
            self.active = Some(driver_type);
            return Ok(driver_type);
        }
        
        // Check PCIe devices for Intel WiFi
        if let Some(driver_type) = self.probe_pcie_wifi() {
            self.active = Some(driver_type);
            return Ok(driver_type);
        }
        
        Err(WifiError::HardwareNotFound)
    }
    
    /// Probe USB bus for WiFi devices
    fn probe_usb_wifi(&self) -> Option<DriverType> {
        // Known USB WiFi VID/PIDs
        const MT7601U_IDS: [(u16, u16); 3] = [
            (0x148f, 0x7601), // MediaTek default
            (0x148f, 0x760b), // MediaTek alternate
            (0x0e8d, 0x7610), // MediaTek
        ];
        
        const RTL8188EU_IDS: [(u16, u16); 5] = [
            (0x0bda, 0x8179), // Realtek default
            (0x2357, 0x010c), // TP-Link TL-WN725N v2
            (0x2357, 0x0111), // TP-Link TL-WN725N v3
            (0x0df6, 0x0076), // Sitecom
            (0x0b05, 0x18f0), // ASUS
        ];
        
        // Scan USB bus (simplified - would use USB subsystem)
        for bus in 0..8u8 {
            for port in 0..8u8 {
                if let Some((vid, pid)) = read_usb_device_ids(bus, port) {
                    for &(known_vid, known_pid) in &MT7601U_IDS {
                        if vid == known_vid && pid == known_pid {
                            return Some(DriverType::Mt7601u);
                        }
                    }
                    for &(known_vid, known_pid) in &RTL8188EU_IDS {
                        if vid == known_vid && pid == known_pid {
                            return Some(DriverType::Rtl8188eu);
                        }
                    }
                }
            }
        }
        None
    }
    
    /// Probe PCIe bus for Intel WiFi
    fn probe_pcie_wifi(&self) -> Option<DriverType> {
        // Intel WiFi PCI IDs (subset)
        const INTEL_WIFI_IDS: [(u16, u16); 8] = [
            (0x8086, 0x2723), // Wi-Fi 6 AX200
            (0x8086, 0x2725), // Wi-Fi 6E AX210
            (0x8086, 0x51F0), // Wi-Fi 6E AX211
            (0x8086, 0x51F1), // Wi-Fi 6E AX211
            (0x8086, 0x54F0), // Wi-Fi 6E AX211
            (0x8086, 0x7AF0), // Wi-Fi 6E AX211
            (0x8086, 0x4DF0), // Wi-Fi 6 AX201
            (0x8086, 0x06F0), // Wi-Fi 6 AX201
        ];
        
        // Scan PCI bus for Intel WiFi
        for bus in 0..256u16 {
            for device in 0..32u8 {
                if let Some((vid, pid)) = read_pci_device_ids(bus as u8, device) {
                    for &(known_vid, known_pid) in &INTEL_WIFI_IDS {
                        if vid == known_vid && pid == known_pid {
                            return Some(DriverType::Iwlwifi);
                        }
                    }
                }
            }
        }
        None
    }
    
    /// Get active driver type
    pub fn active_driver(&self) -> Option<DriverType> {
        self.active
    }
    
    /// Get connection state
    pub fn state(&self) -> ConnectionState {
        self.state
    }
    
    /// Scan for networks (stub)
    pub fn scan(&mut self) -> Result<HVec<ScanResult, MAX_SCAN_RESULTS>, WifiError> {
        if self.active.is_none() {
            return Err(WifiError::NotInitialized);
        }
        // Would delegate to active driver
        Ok(HVec::new())
    }
    
    /// Connect to network (stub)
    pub fn connect(&mut self, _ssid: &[u8], _password: Option<&str>) -> Result<(), WifiError> {
        if self.active.is_none() {
            return Err(WifiError::NotInitialized);
        }
        // Would delegate to active driver
        self.state = ConnectionState::Connected;
        Ok(())
    }
    
    /// Disconnect (stub)
    pub fn disconnect(&mut self) -> Result<(), WifiError> {
        self.state = ConnectionState::Disconnected;
        Ok(())
    }
}

impl Default for DriverRegistry {
    fn default() -> Self {
        Self::new()
    }
}

/// Global driver registry (using UnsafeCell for interior mutability)
use core::cell::UnsafeCell;

struct RegistryHolder(UnsafeCell<DriverRegistry>);
unsafe impl Sync for RegistryHolder {}

static DRIVER_REGISTRY: RegistryHolder = RegistryHolder(UnsafeCell::new(DriverRegistry::new()));

/// Get global driver registry
pub fn registry() -> &'static mut DriverRegistry {
    unsafe { &mut *DRIVER_REGISTRY.0.get() }
}

/// High-level WiFi API functions
pub mod api {
    use super::*;
    
    /// Initialize WiFi subsystem - probes for hardware
    pub fn init() -> Result<DriverType, WifiError> {
        registry().probe()
    }
    
    /// Scan for networks
    pub fn scan() -> Result<HVec<ScanResult, MAX_SCAN_RESULTS>, WifiError> {
        registry().scan()
    }
    
    /// Connect to network
    pub fn connect(ssid: &str, password: Option<&str>) -> Result<(), WifiError> {
        registry().connect(ssid.as_bytes(), password)
    }
    
    /// Disconnect
    pub fn disconnect() -> Result<(), WifiError> {
        registry().disconnect()
    }
    
    /// Get connection state
    pub fn state() -> ConnectionState {
        registry().state()
    }
    
    /// Check if connected
    pub fn is_connected() -> bool {
        state() == ConnectionState::Connected
    }
}

/// Read USB device VID/PID (placeholder - would use USB subsystem)
fn read_usb_device_ids(_bus: u8, _port: u8) -> Option<(u16, u16)> {
    // This would interface with the USB host controller to read device descriptors
    // For now, returns None (no devices)
    None
}

/// Read PCI device VID/PID
fn read_pci_device_ids(bus: u8, device: u8) -> Option<(u16, u16)> {
    let address = 0x80000000u32
        | ((bus as u32) << 16)
        | ((device as u32) << 11)
        | 0; // function 0, offset 0
    
    #[cfg(target_arch = "x86_64")]
    unsafe {
        // Write address to CONFIG_ADDRESS (0xCF8)
        core::arch::asm!("out dx, eax", in("dx") 0xCF8u16, in("eax") address);
        // Read from CONFIG_DATA (0xCFC)
        let value: u32;
        core::arch::asm!("in eax, dx", out("eax") value, in("dx") 0xCFCu16);
        
        let vid = (value & 0xFFFF) as u16;
        let pid = ((value >> 16) & 0xFFFF) as u16;
        
        if vid == 0xFFFF {
            return None;
        }
        
        Some((vid, pid))
    }
    #[cfg(not(target_arch = "x86_64"))]
    { None }
}
