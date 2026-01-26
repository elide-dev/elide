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
        // Check for MT7601U (USB VID:PID 0x148f:0x7601)
        // In real impl, would enumerate USB devices
        // For now, return NotFound - hardware detection is stubbed
        
        // TODO: Enumerate USB devices and match against known WiFi VID/PIDs
        // TODO: Enumerate PCIe devices for Intel WiFi
        
        Err(WifiError::HardwareNotFound)
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
