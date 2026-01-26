// cfg80211 Shim for Colide OS
// This module provides a Linux cfg80211-compatible API for WiFi drivers
// Inspired by the Linux kernel's cfg80211 subsystem

use super::linux_compat::{SkBuff, SpinLock};
use core::sync::atomic::AtomicU32;

/// WiFi frequency bands
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum Band {
    #[default]
    Band2GHz = 0,
    Band5GHz = 1,
    Band6GHz = 2,
}

/// Channel definition
#[derive(Debug, Clone)]
pub struct Channel {
    pub band: Band,
    pub center_freq: u32,  // MHz
    pub hw_value: u16,
    pub flags: u32,
    pub max_power: i32,    // dBm
}

impl Channel {
    pub const FLAG_DISABLED: u32 = 1 << 0;
    pub const FLAG_NO_IR: u32 = 1 << 1;      // No initiating radiation
    pub const FLAG_RADAR: u32 = 1 << 2;       // DFS required
    pub const FLAG_NO_HT40MINUS: u32 = 1 << 3;
    pub const FLAG_NO_HT40PLUS: u32 = 1 << 4;
    pub const FLAG_NO_80MHZ: u32 = 1 << 5;
    pub const FLAG_NO_160MHZ: u32 = 1 << 6;
}

/// Supported rates for a band
#[derive(Debug, Clone)]
pub struct Rate {
    pub bitrate: u16,      // In units of 100 Kbps
    pub hw_value: u16,
    pub hw_value_short: u16,
    pub flags: u16,
}

/// Band configuration
#[derive(Debug, Clone)]
pub struct SupportedBand {
    pub band: Band,
    pub channels: Vec<Channel>,
    pub bitrates: Vec<Rate>,
    pub ht_cap: Option<HtCapabilities>,
    pub vht_cap: Option<VhtCapabilities>,
    pub he_cap: Option<HeCapabilities>,
}

/// HT (802.11n) capabilities
#[derive(Debug, Clone, Default)]
pub struct HtCapabilities {
    pub cap: u16,
    pub ht_supported: bool,
    pub ampdu_factor: u8,
    pub ampdu_density: u8,
    pub mcs: HtMcsInfo,
}

#[derive(Debug, Clone, Default)]
pub struct HtMcsInfo {
    pub rx_mask: [u8; 10],
    pub rx_highest: u16,
    pub tx_params: u8,
}

/// VHT (802.11ac) capabilities
#[derive(Debug, Clone, Default)]
pub struct VhtCapabilities {
    pub vht_supported: bool,
    pub cap: u32,
    pub mcs: VhtMcsInfo,
}

#[derive(Debug, Clone, Default)]
pub struct VhtMcsInfo {
    pub rx_mcs_map: u16,
    pub rx_highest: u16,
    pub tx_mcs_map: u16,
    pub tx_highest: u16,
}

/// HE (802.11ax/WiFi 6) capabilities
#[derive(Debug, Clone, Default)]
pub struct HeCapabilities {
    pub has_he: bool,
    pub he_cap_elem: HeCapElem,
}

#[derive(Debug, Clone, Default)]
pub struct HeCapElem {
    pub mac_cap_info: [u8; 6],
    pub phy_cap_info: [u8; 11],
}

/// Interface types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum InterfaceType {
    Unspecified,
    AdHoc,
    Station,
    Ap,
    ApVlan,
    Wds,
    Monitor,
    MeshPoint,
    P2pClient,
    P2pGo,
    P2pDevice,
    Ocb,
    Nan,
}

/// Wiphy - represents a physical wireless device
pub struct Wiphy {
    pub name: String,
    pub perm_addr: [u8; 6],
    pub bands: [Option<SupportedBand>; 3],
    pub interface_modes: u32,
    pub max_scan_ssids: u8,
    pub max_scan_ie_len: u16,
    pub signal_type: SignalType,
    pub cipher_suites: Vec<u32>,
    pub n_cipher_suites: usize,
    pub retry_short: u8,
    pub retry_long: u8,
    pub frag_threshold: u32,
    pub rts_threshold: u32,
    pub priv_data: Option<Box<dyn core::any::Any + Send + Sync>>,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SignalType {
    None,
    Mbm,      // Signal in mBm (1/100 dBm)
    Unspec,   // Arbitrary units
}

/// Wireless interface (netdev)
pub struct WirelessDev {
    pub wiphy: *const Wiphy,
    pub iftype: InterfaceType,
    pub address: [u8; 6],
    pub netdev_index: u32,
    pub identifier: u64,
    pub current_bss: Option<Box<BssInfo>>,
    pub ssid: Vec<u8>,
    pub ssid_len: usize,
}

/// BSS (Basic Service Set) information
#[derive(Debug, Clone)]
pub struct BssInfo {
    pub bssid: [u8; 6],
    pub channel: Channel,
    pub beacon_interval: u16,
    pub capability: u16,
    pub signal: i32,
    pub tsf: u64,
    pub ies: Vec<u8>,           // Information Elements
    pub beacon_ies: Vec<u8>,
    pub proberesp_ies: Vec<u8>,
}

/// Scan request
#[derive(Debug, Clone)]
pub struct ScanRequest {
    pub ssids: Vec<Ssid>,
    pub channels: Vec<Channel>,
    pub ie: Vec<u8>,           // Extra IEs to add to probe request
    pub flags: u32,
    pub rates: [u32; 3],       // Bitrates per band
    pub duration: u16,
    pub duration_mandatory: bool,
}

#[derive(Debug, Clone)]
pub struct Ssid {
    pub ssid: [u8; 32],
    pub ssid_len: usize,
}

impl Ssid {
    pub fn new(name: &str) -> Self {
        let bytes = name.as_bytes();
        let len = bytes.len().min(32);
        let mut ssid = [0u8; 32];
        ssid[..len].copy_from_slice(&bytes[..len]);
        Self { ssid, ssid_len: len }
    }
    
    pub fn as_str(&self) -> &str {
        core::str::from_utf8(&self.ssid[..self.ssid_len]).unwrap_or("")
    }
}

/// Connect parameters
#[derive(Debug, Clone)]
pub struct ConnectParams {
    pub channel: Option<Channel>,
    pub bssid: Option<[u8; 6]>,
    pub ssid: Ssid,
    pub auth_type: AuthType,
    pub ie: Vec<u8>,
    pub privacy: bool,
    pub crypto: CryptoSettings,
    pub key_idx: u8,
    pub key: Vec<u8>,
    pub flags: u32,
    pub bg_scan_period: i32,
    pub ht_capa: Option<HtCapabilities>,
    pub vht_capa: Option<VhtCapabilities>,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AuthType {
    OpenSystem,
    SharedKey,
    Ft,
    NetworkEap,
    Sae,
    FilsSk,
    FilsSkPfs,
    FilsPk,
}

#[derive(Debug, Clone, Default)]
pub struct CryptoSettings {
    pub wpa_versions: u32,
    pub cipher_group: u32,
    pub n_ciphers_pairwise: usize,
    pub ciphers_pairwise: [u32; 4],
    pub n_akm_suites: usize,
    pub akm_suites: [u32; 2],
    pub control_port: bool,
    pub control_port_ethertype: u16,
    pub control_port_no_encrypt: bool,
    pub psk: Option<[u8; 32]>,
    pub sae_pwd: Option<String>,
}

/// Key parameters
#[derive(Debug, Clone)]
pub struct KeyParams {
    pub key: Vec<u8>,
    pub key_len: usize,
    pub cipher: u32,
    pub seq: Vec<u8>,
    pub seq_len: usize,
    pub mode: KeyMode,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum KeyMode {
    RxTx,
    Rx,
    Tx,
}

/// Cipher suite constants (IEEE OUI + suite type)
pub mod cipher {
    pub const WEP40: u32 = 0x000FAC01;
    pub const TKIP: u32 = 0x000FAC02;
    pub const CCMP: u32 = 0x000FAC04;
    pub const WEP104: u32 = 0x000FAC05;
    pub const CMAC: u32 = 0x000FAC06;
    pub const GCMP: u32 = 0x000FAC08;
    pub const GCMP_256: u32 = 0x000FAC09;
    pub const CCMP_256: u32 = 0x000FAC0A;
    pub const GMAC: u32 = 0x000FAC0B;
    pub const GMAC_256: u32 = 0x000FAC0C;
}

/// AKM suite constants
pub mod akm {
    pub const PSK: u32 = 0x000FAC02;
    pub const FT_PSK: u32 = 0x000FAC04;
    pub const PSK_SHA256: u32 = 0x000FAC06;
    pub const SAE: u32 = 0x000FAC08;
    pub const FT_SAE: u32 = 0x000FAC09;
}

/// cfg80211 operations - the main interface that drivers implement
pub trait Cfg80211Ops: Send + Sync {
    /// Scan for networks
    fn scan(&self, request: &ScanRequest) -> Result<(), i32>;
    
    /// Abort a running scan
    fn abort_scan(&self, wdev: &WirelessDev);
    
    /// Connect to a network
    fn connect(&self, params: &ConnectParams) -> Result<(), i32>;
    
    /// Disconnect from current network
    fn disconnect(&self, reason_code: u16) -> Result<(), i32>;
    
    /// Add a key
    fn add_key(&self, key_idx: u8, pairwise: bool, mac_addr: Option<&[u8; 6]>, params: &KeyParams) -> Result<(), i32>;
    
    /// Delete a key
    fn del_key(&self, key_idx: u8, pairwise: bool, mac_addr: Option<&[u8; 6]>) -> Result<(), i32>;
    
    /// Set default key
    fn set_default_key(&self, key_idx: u8, unicast: bool, multicast: bool) -> Result<(), i32>;
    
    /// Get station info
    fn get_station(&self, mac: &[u8; 6]) -> Result<StationInfo, i32>;
    
    /// Set power save mode
    fn set_power_mgmt(&self, enabled: bool, timeout: i32) -> Result<(), i32>;
    
    /// Set TX power
    fn set_tx_power(&self, wdev: &WirelessDev, type_: TxPowerType, mbm: i32) -> Result<(), i32>;
    
    /// Get TX power
    fn get_tx_power(&self, wdev: &WirelessDev) -> Result<i32, i32>;
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TxPowerType {
    Automatic,
    Limited,
    Fixed,
}

/// Station information
#[derive(Debug, Clone, Default)]
pub struct StationInfo {
    pub filled: u64,
    pub connected_time: u32,
    pub inactive_time: u32,
    pub rx_bytes: u64,
    pub tx_bytes: u64,
    pub rx_packets: u32,
    pub tx_packets: u32,
    pub signal: i8,
    pub signal_avg: i8,
    pub tx_bitrate: RateInfo,
    pub rx_bitrate: RateInfo,
}

#[derive(Debug, Clone, Default)]
pub struct RateInfo {
    pub flags: u16,
    pub mcs: u8,
    pub legacy: u16,
    pub nss: u8,
    pub bw: u8,
    pub he_gi: u8,
    pub he_dcm: u8,
    pub he_ru_alloc: u8,
}

/// Events from driver to cfg80211
pub enum Cfg80211Event {
    ScanDone { aborted: bool },
    ScanResult { bss: BssInfo },
    Connected { bssid: [u8; 6], req_ie: Vec<u8>, resp_ie: Vec<u8> },
    Disconnected { bssid: [u8; 6], reason: u16, locally_generated: bool },
    MichaelMicFailure { bssid: [u8; 6], key_type: KeyType, key_id: i8, tsc: Option<[u8; 6]> },
    RxMgmt { frame: Vec<u8>, freq: u32, sig_dbm: i32 },
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum KeyType {
    Group,
    Pairwise,
    PeerKey,
}

/// Registered wiphy with operations
pub struct RegisteredWiphy {
    pub wiphy: Wiphy,
    pub ops: Box<dyn Cfg80211Ops>,
}

/// Global registry of WiFi devices
static WIPHY_REGISTRY: SpinLock<Vec<RegisteredWiphy>> = SpinLock::new(Vec::new());

/// Register a new wiphy
pub fn wiphy_register(wiphy: Wiphy, ops: Box<dyn Cfg80211Ops>) -> Result<usize, i32> {
    let mut registry = WIPHY_REGISTRY.lock();
    let idx = registry.len();
    registry.push(RegisteredWiphy { wiphy, ops });
    Ok(idx)
}

/// Unregister a wiphy
pub fn wiphy_unregister(idx: usize) -> Result<(), i32> {
    let mut registry = WIPHY_REGISTRY.lock();
    if idx < registry.len() {
        registry.remove(idx);
        Ok(())
    } else {
        Err(super::linux_compat::errno::ENODEV)
    }
}

/// Create default 2.4GHz channels
pub fn create_2ghz_channels() -> Vec<Channel> {
    (1..=14).map(|ch| {
        let freq = if ch == 14 { 2484 } else { 2407 + ch * 5 };
        Channel {
            band: Band::Band2GHz,
            center_freq: freq,
            hw_value: ch as u16,
            flags: 0,
            max_power: 20,
        }
    }).collect()
}

/// Create default 5GHz channels (UNII-1, UNII-2, UNII-3)
pub fn create_5ghz_channels() -> Vec<Channel> {
    let channels = [
        36, 40, 44, 48,           // UNII-1
        52, 56, 60, 64,           // UNII-2A (DFS)
        100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, // UNII-2C (DFS)
        149, 153, 157, 161, 165,  // UNII-3
    ];
    
    channels.iter().map(|&ch| {
        let freq = 5000 + ch * 5;
        let flags = if (52..=144).contains(&ch) { Channel::FLAG_RADAR } else { 0 };
        Channel {
            band: Band::Band5GHz,
            center_freq: freq,
            hw_value: ch as u16,
            flags,
            max_power: 23,
        }
    }).collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_ssid() {
        let ssid = Ssid::new("MyNetwork");
        assert_eq!(ssid.as_str(), "MyNetwork");
        assert_eq!(ssid.ssid_len, 9);
    }
    
    #[test]
    fn test_channels() {
        let channels_2g = create_2ghz_channels();
        assert_eq!(channels_2g.len(), 14);
        assert_eq!(channels_2g[0].center_freq, 2412); // Channel 1
        assert_eq!(channels_2g[5].center_freq, 2437); // Channel 6
        
        let channels_5g = create_5ghz_channels();
        assert!(channels_5g.len() > 20);
        assert_eq!(channels_5g[0].center_freq, 5180); // Channel 36
    }
}
