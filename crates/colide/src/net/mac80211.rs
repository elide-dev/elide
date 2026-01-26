// mac80211 Shim for Colide OS
// This module provides a Linux mac80211-compatible API for SoftMAC WiFi drivers
// SoftMAC devices handle 802.11 frame management in software (most consumer WiFi cards)

use super::cfg80211::{
    Band, Channel, SupportedBand, HtCapabilities, VhtCapabilities, HeCapabilities,
    Wiphy, WirelessDev, InterfaceType, BssInfo, KeyParams, StationInfo, Cfg80211Ops,
};
use super::linux_compat::{SkBuff, SpinLock, Kref};
use core::sync::atomic::{AtomicU32, AtomicU64, AtomicBool, Ordering};

/// Hardware flags indicating device capabilities
pub mod hw_flags {
    pub const HAS_RATE_CONTROL: u32 = 1 << 0;
    pub const RX_INCLUDES_FCS: u32 = 1 << 1;
    pub const HOST_BROADCAST_PS_BUFFERING: u32 = 1 << 2;
    pub const SIGNAL_UNSPEC: u32 = 1 << 3;
    pub const SIGNAL_DBM: u32 = 1 << 4;
    pub const NEED_DTIM_BEFORE_ASSOC: u32 = 1 << 5;
    pub const SPECTRUM_MGMT: u32 = 1 << 6;
    pub const AMPDU_AGGREGATION: u32 = 1 << 7;
    pub const SUPPORTS_PS: u32 = 1 << 8;
    pub const PS_NULLFUNC_STACK: u32 = 1 << 9;
    pub const SUPPORTS_DYNAMIC_PS: u32 = 1 << 10;
    pub const MFP_CAPABLE: u32 = 1 << 11;
    pub const WANT_MONITOR_VIF: u32 = 1 << 12;
    pub const NO_AUTO_VIF: u32 = 1 << 13;
    pub const SW_CRYPTO_CONTROL: u32 = 1 << 14;
    pub const SUPPORT_FAST_XMIT: u32 = 1 << 15;
    pub const REPORTS_TX_ACK_STATUS: u32 = 1 << 16;
    pub const CONNECTION_MONITOR: u32 = 1 << 17;
    pub const QUEUE_CONTROL: u32 = 1 << 18;
    pub const SUPPORTS_PER_STA_GTK: u32 = 1 << 19;
    pub const AP_LINK_PS: u32 = 1 << 20;
    pub const TX_AMPDU_SETUP_IN_HW: u32 = 1 << 21;
    pub const SUPPORTS_RC_TABLE: u32 = 1 << 22;
    pub const CHANCTX_STA_CSA: u32 = 1 << 23;
    pub const SUPPORTS_CLONED_SKBS: u32 = 1 << 24;
    pub const SINGLE_SCAN_ON_ALL_BANDS: u32 = 1 << 25;
    pub const TDLS_WIDER_BW: u32 = 1 << 26;
    pub const SUPPORTS_AMSDU_IN_AMPDU: u32 = 1 << 27;
    pub const BEACON_TX_STATUS: u32 = 1 << 28;
    pub const NEEDS_UNIQUE_STA_ADDR: u32 = 1 << 29;
    pub const SUPPORTS_REORDERING_BUFFER: u32 = 1 << 30;
    pub const USES_RSS: u32 = 1 << 31;
}

/// TX control flags
pub mod tx_flags {
    pub const NO_ACK: u32 = 1 << 0;
    pub const PS_RESPONSE: u32 = 1 << 1;
    pub const CTL_ASSIGN_SEQ: u32 = 1 << 2;
    pub const CTL_FIRST_FRAGMENT: u32 = 1 << 3;
    pub const CTL_AMPDU: u32 = 1 << 4;
    pub const CTL_NO_PS_BUFFER: u32 = 1 << 5;
    pub const CTL_RATE_CTRL_PROBE: u32 = 1 << 6;
    pub const CTL_CLEAR_PS_FILT: u32 = 1 << 7;
    pub const CTL_REQ_TX_STATUS: u32 = 1 << 8;
    pub const CTL_LDPC: u32 = 1 << 9;
    pub const CTL_STBC: u32 = 1 << 10;
    pub const CTL_HW_80211_ENCAP: u32 = 1 << 11;
}

/// RX flags
pub mod rx_flags {
    pub const MMIC_ERROR: u32 = 1 << 0;
    pub const DECRYPTED: u32 = 1 << 1;
    pub const MMIC_STRIPPED: u32 = 1 << 2;
    pub const IV_STRIPPED: u32 = 1 << 3;
    pub const FAILED_FCS_CRC: u32 = 1 << 4;
    pub const FAILED_PLCP_CRC: u32 = 1 << 5;
    pub const MACTIME_START: u32 = 1 << 6;
    pub const MACTIME_END: u32 = 1 << 7;
    pub const SHORTPRE: u32 = 1 << 8;
    pub const HT: u32 = 1 << 9;
    pub const VHT: u32 = 1 << 10;
    pub const HE: u32 = 1 << 11;
    pub const NO_SIGNAL_VAL: u32 = 1 << 12;
    pub const AMPDU_LAST_KNOWN: u32 = 1 << 13;
    pub const AMPDU_IS_LAST: u32 = 1 << 14;
    pub const AMPDU_DELIM_CRC_ERROR: u32 = 1 << 15;
    pub const AMPDU_DELIM_CRC_KNOWN: u32 = 1 << 16;
    pub const MIC_STRIPPED: u32 = 1 << 17;
    pub const AMSDU: u32 = 1 << 18;
    pub const CSUM_UNNECESSARY: u32 = 1 << 19;
}

/// IEEE 802.11 hardware abstraction
pub struct Ieee80211Hw {
    pub wiphy: *mut Wiphy,
    pub priv_data: Option<Box<dyn core::any::Any + Send + Sync>>,
    pub flags: u32,
    pub extra_tx_headroom: u32,
    pub extra_beacon_tailroom: u32,
    pub max_signal: i32,
    pub max_listen_interval: u16,
    pub queues: u8,
    pub max_rates: u8,
    pub max_report_rates: u8,
    pub max_rate_tries: u8,
    pub max_rx_aggregation_subframes: u8,
    pub max_tx_aggregation_subframes: u8,
    pub max_tx_fragments: u8,
    pub offchannel_tx_hw_queue: u8,
    pub radiotap_mcs_details: u8,
    pub radiotap_vht_details: u16,
    pub radiotap_timestamp: Option<RadiotapTimestamp>,
    pub netdev_features: u64,
    pub uapsd_queues: u8,
    pub uapsd_max_sp_len: u8,
    pub n_cipher_schemes: usize,
    pub weight_multiplier: u8,
    pub max_mtu: u32,
}

#[derive(Debug, Clone, Default)]
pub struct RadiotapTimestamp {
    pub units_pos: u16,
    pub accuracy: u16,
}

/// Virtual interface
pub struct Ieee80211Vif {
    pub type_: InterfaceType,
    pub bss_conf: BssConf,
    pub addr: [u8; 6],
    pub p2p: bool,
    pub csa_active: bool,
    pub mu_mimo_owner: bool,
    pub driver_flags: u32,
    pub hw_queue: [u8; 4],
    pub cab_queue: u8,
    pub chanctx_conf: Option<Box<ChanCtxConf>>,
    pub drv_priv: Option<Box<dyn core::any::Any + Send + Sync>>,
}

/// BSS configuration
#[derive(Debug, Clone, Default)]
pub struct BssConf {
    pub bssid: [u8; 6],
    pub aid: u16,
    pub assoc: bool,
    pub ibss_joined: bool,
    pub ibss_creator: bool,
    pub use_cts_prot: bool,
    pub use_short_preamble: bool,
    pub use_short_slot: bool,
    pub dtim_period: u8,
    pub beacon_int: u16,
    pub assoc_capability: u16,
    pub timestamp: u64,
    pub basic_rates: u32,
    pub mcast_rate: [i32; 3],  // Per band
    pub bssid_index: u8,
    pub bssid_indicator: u8,
    pub ema_ap: bool,
    pub sync_tsf: u64,
    pub sync_device_ts: u32,
    pub sync_dtim_count: u8,
    pub qos: bool,
    pub hidden_ssid: bool,
    pub txpower: i32,
    pub txpower_type: TxPowerSetting,
    pub p2p_noa_attr: P2pNoaAttr,
    pub ht_operation_mode: u16,
    pub cqm_rssi_thold: i32,
    pub cqm_rssi_hyst: u32,
    pub channel_width: ChannelWidth,
    pub center_freq1: u32,
    pub center_freq2: u32,
    pub arp_addr_list: Vec<[u8; 4]>,
    pub arp_addr_cnt: i32,
    pub arp_filter_enabled: bool,
    pub qos_map: Option<Vec<u8>>,
    pub enable_beacon: bool,
    pub ssid: [u8; 32],
    pub ssid_len: usize,
    pub s1g: bool,
    pub twt_requester: bool,
    pub twt_responder: bool,
    pub twt_protected: bool,
    pub twt_broadcast: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum TxPowerSetting {
    #[default]
    NotConfigured,
    Automatic,
    Limited,
    Fixed,
}

#[derive(Debug, Clone, Default)]
pub struct P2pNoaAttr {
    pub index: u8,
    pub oppps_ctwindow: u8,
    pub desc: [P2pNoaDesc; 4],
}

#[derive(Debug, Clone, Default)]
pub struct P2pNoaDesc {
    pub count: u8,
    pub duration: u32,
    pub interval: u32,
    pub start_time: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ChannelWidth {
    #[default]
    Width20NoHt,
    Width20,
    Width40,
    Width80,
    Width80P80,
    Width160,
    Width5,
    Width10,
    Width1,
    Width2,
    Width4,
    Width8,
    Width16,
    Width320,
}

/// Channel context configuration
#[derive(Debug, Clone)]
pub struct ChanCtxConf {
    pub def: ChanDef,
    pub min_def: ChanDef,
    pub rx_chains_static: u8,
    pub rx_chains_dynamic: u8,
    pub radar_enabled: bool,
}

#[derive(Debug, Clone)]
pub struct ChanDef {
    pub chan: Channel,
    pub width: ChannelWidth,
    pub center_freq1: u32,
    pub center_freq2: u32,
}

/// Station structure
pub struct Ieee80211Sta {
    pub addr: [u8; 6],
    pub aid: u16,
    pub supp_rates: [u32; 3],  // Per band
    pub ht_cap: HtCapabilities,
    pub vht_cap: VhtCapabilities,
    pub he_cap: HeCapabilities,
    pub max_rx_aggregation_subframes: u8,
    pub wme: bool,
    pub uapsd_queues: u8,
    pub max_sp: u8,
    pub bandwidth: StaBandwidth,
    pub rx_nss: u8,
    pub smps_mode: SmpsMode,
    pub tdls: bool,
    pub tdls_initiator: bool,
    pub mfp: bool,
    pub max_amsdu_subframes: u8,
    pub max_amsdu_len: u16,
    pub support_p2p_ps: bool,
    pub max_rc_amsdu_len: u16,
    pub txq: [Option<Box<Ieee80211Txq>>; 16],
    pub drv_priv: Option<Box<dyn core::any::Any + Send + Sync>>,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum StaBandwidth {
    #[default]
    Bw20,
    Bw40,
    Bw80,
    Bw160,
    Bw320,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum SmpsMode {
    Off,
    #[default]
    Static,
    Dynamic,
    AutomaticMode,
    NumModes,
}

/// TX queue
pub struct Ieee80211Txq {
    pub vif: *mut Ieee80211Vif,
    pub sta: *mut Ieee80211Sta,
    pub tid: u8,
    pub ac: u8,
    pub drv_priv: Option<Box<dyn core::any::Any + Send + Sync>>,
}

/// Key configuration
#[derive(Debug, Clone)]
pub struct Ieee80211KeyConf {
    pub hw_key_idx: u32,
    pub cipher: u32,
    pub tx_pn: u64,
    pub flags: u8,
    pub keyidx: u8,
    pub keylen: u8,
    pub key: [u8; 32],
    pub icv_len: u8,
    pub iv_len: u8,
}

pub mod key_flags {
    pub const GROUP: u8 = 1 << 0;
    pub const PAIRWISE: u8 = 1 << 1;
    pub const STATIC_TX_PN: u8 = 1 << 2;
    pub const RX_MGMT: u8 = 1 << 3;
    pub const RESERVE_TAILROOM: u8 = 1 << 4;
    pub const SET_TX_PN: u8 = 1 << 5;
    pub const GENERATE_IV: u8 = 1 << 6;
    pub const GENERATE_MMIC: u8 = 1 << 7;
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SetKeyCmd {
    SetKey,
    DisableKey,
}

/// TX information attached to each frame
#[derive(Debug, Clone, Default)]
pub struct Ieee80211TxInfo {
    pub flags: u32,
    pub band: u8,
    pub hw_queue: u8,
    pub ack_frame_id: u16,
    pub control: TxControl,
    pub status: TxStatus,
}

#[derive(Debug, Clone, Default)]
pub struct TxControl {
    pub rates: [TxRate; 4],
    pub rts_cts_rate_idx: i8,
    pub use_rts: bool,
    pub use_cts_prot: bool,
    pub short_preamble: bool,
    pub skip_table: bool,
}

#[derive(Debug, Clone, Default)]
pub struct TxRate {
    pub idx: i8,
    pub count: u8,
    pub flags: u16,
}

pub mod rate_flags {
    pub const SHORT_GI: u16 = 1 << 0;
    pub const MCS: u16 = 1 << 1;
    pub const BW_40: u16 = 1 << 2;
    pub const VHT_MCS: u16 = 1 << 3;
    pub const BW_80: u16 = 1 << 4;
    pub const BW_160: u16 = 1 << 5;
    pub const HE_MCS: u16 = 1 << 6;
}

#[derive(Debug, Clone, Default)]
pub struct TxStatus {
    pub rates: [TxRate; 4],
    pub ack_signal: i8,
    pub ampdu_ack_len: u8,
    pub ampdu_len: u8,
    pub antenna: u8,
    pub tx_time: u16,
    pub is_valid_ack_signal: bool,
}

/// RX status information
#[derive(Debug, Clone, Default)]
pub struct Ieee80211RxStatus {
    pub mactime: u64,
    pub device_timestamp: u32,
    pub band: Band,
    pub freq: u32,
    pub freq_offset: i32,
    pub signal: i32,
    pub chains: u8,
    pub chain_signal: [i8; 4],
    pub antenna: u8,
    pub rate_idx: u8,
    pub nss: u8,
    pub flag: u32,
    pub rx_flags: u16,
    pub ampdu_reference: u32,
    pub ampdu_delimiter_crc: u8,
    pub zero_length_psdu_type: u8,
    pub enc_flags: u8,
    pub encoding: RxEncoding,
    pub bw: RxBandwidth,
    pub he_ru: HeRuAlloc,
    pub he_gi: HeGi,
    pub he_dcm: u8,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum RxEncoding {
    #[default]
    Legacy,
    Ht,
    Vht,
    He,
    Eht,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum RxBandwidth {
    #[default]
    Bw20,
    Bw40,
    Bw80,
    Bw160,
    Bw320,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum HeRuAlloc {
    #[default]
    Ru26,
    Ru52,
    Ru106,
    Ru242,
    Ru484,
    Ru996,
    Ru2x996,
}

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum HeGi {
    #[default]
    Gi08,
    Gi16,
    Gi32,
}

/// mac80211 operations - the main interface that drivers implement
pub trait Ieee80211Ops: Send + Sync {
    /// Called before first netdevice is enabled
    fn start(&self, hw: &Ieee80211Hw) -> Result<(), i32>;
    
    /// Called after last netdevice is disabled
    fn stop(&self, hw: &Ieee80211Hw);
    
    /// Suspend device (for PM)
    fn suspend(&self, hw: &Ieee80211Hw) -> Result<(), i32> {
        Ok(())
    }
    
    /// Resume device (for PM)
    fn resume(&self, hw: &Ieee80211Hw) -> Result<(), i32> {
        Ok(())
    }
    
    /// Add a virtual interface
    fn add_interface(&self, hw: &Ieee80211Hw, vif: &mut Ieee80211Vif) -> Result<(), i32>;
    
    /// Remove a virtual interface
    fn remove_interface(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif);
    
    /// Change virtual interface type
    fn change_interface(&self, hw: &Ieee80211Hw, vif: &mut Ieee80211Vif, new_type: InterfaceType, p2p: bool) -> Result<(), i32>;
    
    /// Configure the hardware
    fn config(&self, hw: &Ieee80211Hw, changed: u32) -> Result<(), i32>;
    
    /// BSS information changed
    fn bss_info_changed(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, info: &BssConf, changed: u64);
    
    /// Transmit a frame
    fn tx(&self, hw: &Ieee80211Hw, control: &TxControl, skb: Box<SkBuff>);
    
    /// Wake TX queues
    fn wake_tx_queue(&self, hw: &Ieee80211Hw, txq: &Ieee80211Txq) {
        // Default: do nothing, driver uses push model
    }
    
    /// Configure TX queue parameters
    fn conf_tx(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, link_id: u32, queue: u16, params: &TxqParams) -> Result<(), i32>;
    
    /// Start software scan
    fn sw_scan_start(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, mac_addr: &[u8; 6]);
    
    /// Complete software scan
    fn sw_scan_complete(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif);
    
    /// Hardware scan request
    fn hw_scan(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, req: &HwScanRequest) -> Result<(), i32> {
        Err(super::linux_compat::errno::EOPNOTSUPP)
    }
    
    /// Cancel hardware scan
    fn cancel_hw_scan(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif) {}
    
    /// Set encryption key
    fn set_key(&self, hw: &Ieee80211Hw, cmd: SetKeyCmd, vif: &Ieee80211Vif, sta: Option<&Ieee80211Sta>, key: &mut Ieee80211KeyConf) -> Result<(), i32>;
    
    /// Update TKIP key
    fn update_tkip_key(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, key: &Ieee80211KeyConf, sta: &Ieee80211Sta, iv32: u32, phase1key: &[u16; 5]) {}
    
    /// Configure RX filters
    fn configure_filter(&self, hw: &Ieee80211Hw, changed_flags: u32, total_flags: &mut u32, mc_count: i32);
    
    /// Station added
    fn sta_add(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, sta: &mut Ieee80211Sta) -> Result<(), i32> {
        Ok(())
    }
    
    /// Station removed
    fn sta_remove(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, sta: &Ieee80211Sta) -> Result<(), i32> {
        Ok(())
    }
    
    /// Station notify (state change)
    fn sta_notify(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, cmd: StaNotifyCmd, sta: &Ieee80211Sta) {}
    
    /// Station state transition
    fn sta_state(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, sta: &mut Ieee80211Sta, old_state: StaState, new_state: StaState) -> Result<(), i32> {
        Ok(())
    }
    
    /// Get TX stats for a station
    fn get_txpower(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, power: &mut i32) -> Result<(), i32> {
        Err(super::linux_compat::errno::EOPNOTSUPP)
    }
    
    /// Flush TX queues
    fn flush(&self, hw: &Ieee80211Hw, vif: Option<&Ieee80211Vif>, queues: u32, drop: bool);
    
    /// AMPDU action
    fn ampdu_action(&self, hw: &Ieee80211Hw, vif: &Ieee80211Vif, params: &AmpduParams) -> Result<(), i32> {
        Err(super::linux_compat::errno::EOPNOTSUPP)
    }
}

#[derive(Debug, Clone, Default)]
pub struct TxqParams {
    pub txop: u16,
    pub cw_min: u16,
    pub cw_max: u16,
    pub aifs: u8,
    pub acm: bool,
    pub uapsd: bool,
    pub mu_edca: bool,
    pub mu_edca_param_rec: MuEdcaParamRec,
}

#[derive(Debug, Clone, Default)]
pub struct MuEdcaParamRec {
    pub aifsn: u8,
    pub ecw_min_max: u8,
    pub mu_edca_timer: u8,
}

#[derive(Debug, Clone)]
pub struct HwScanRequest {
    pub ssids: Vec<super::cfg80211::Ssid>,
    pub n_ssids: usize,
    pub channels: Vec<Channel>,
    pub n_channels: usize,
    pub ie: Vec<u8>,
    pub ie_len: usize,
    pub flags: u32,
    pub rates: [u32; 3],
    pub duration: u16,
    pub duration_mandatory: bool,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StaNotifyCmd {
    Sleep,
    Awake,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StaState {
    NotExist,
    None,
    Auth,
    Assoc,
    Authorized,
}

#[derive(Debug, Clone)]
pub struct AmpduParams {
    pub action: AmpduAction,
    pub sta: *mut Ieee80211Sta,
    pub tid: u8,
    pub ssn: u16,
    pub buf_size: u16,
    pub amsdu: bool,
    pub timeout: u16,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AmpduAction {
    RxStart,
    RxStop,
    TxStart,
    TxStartImmediate,
    TxStop,
    TxOperational,
}

/// Allocate a new Ieee80211Hw structure
pub fn ieee80211_alloc_hw(priv_size: usize) -> Option<Box<Ieee80211Hw>> {
    Some(Box::new(Ieee80211Hw {
        wiphy: core::ptr::null_mut(),
        priv_data: None,
        flags: 0,
        extra_tx_headroom: 0,
        extra_beacon_tailroom: 0,
        max_signal: 0,
        max_listen_interval: 0,
        queues: 4,
        max_rates: 4,
        max_report_rates: 0,
        max_rate_tries: 0,
        max_rx_aggregation_subframes: 0,
        max_tx_aggregation_subframes: 0,
        max_tx_fragments: 0,
        offchannel_tx_hw_queue: 0,
        radiotap_mcs_details: 0,
        radiotap_vht_details: 0,
        radiotap_timestamp: None,
        netdev_features: 0,
        uapsd_queues: 0,
        uapsd_max_sp_len: 0,
        n_cipher_schemes: 0,
        weight_multiplier: 1,
        max_mtu: 1500,
    }))
}

/// Register hardware with mac80211
pub fn ieee80211_register_hw(hw: &mut Ieee80211Hw) -> Result<(), i32> {
    // TODO: Register with cfg80211, create default VIF
    Ok(())
}

/// Unregister hardware from mac80211
pub fn ieee80211_unregister_hw(hw: &Ieee80211Hw) {
    // TODO: Cleanup
}

/// Free hardware structure
pub fn ieee80211_free_hw(_hw: Box<Ieee80211Hw>) {
    // Rust will automatically drop
}

/// Report received frame to mac80211
pub fn ieee80211_rx(hw: &Ieee80211Hw, skb: Box<SkBuff>) {
    // TODO: Process received frame
}

/// Report received frame with status
pub fn ieee80211_rx_irqsafe(hw: &Ieee80211Hw, skb: Box<SkBuff>) {
    // TODO: IRQ-safe RX processing
}

/// Get TX info from skb
pub fn ieee80211_get_tx_info(skb: &SkBuff) -> &Ieee80211TxInfo {
    // TX info is stored in skb->cb
    unsafe {
        &*(skb.cb.as_ptr() as *const Ieee80211TxInfo)
    }
}

/// Report TX status
pub fn ieee80211_tx_status(hw: &Ieee80211Hw, skb: Box<SkBuff>) {
    // TODO: Process TX completion
}

/// Free transmitted skb
pub fn ieee80211_free_txskb(hw: &Ieee80211Hw, skb: Box<SkBuff>) {
    // Rust will automatically drop
}

/// Queue frame for transmission
pub fn ieee80211_queue_work(hw: &Ieee80211Hw, work: impl FnOnce() + Send + 'static) {
    // TODO: Queue to workqueue
}

/// Report scan completed
pub fn ieee80211_scan_completed(hw: &Ieee80211Hw, info: ScanInfo) {
    // TODO: Notify cfg80211
}

#[derive(Debug, Clone, Default)]
pub struct ScanInfo {
    pub aborted: bool,
    pub scan_start_tsf: u64,
    pub tsf_bssid: [u8; 6],
}

/// Report BSS information from scan
pub fn ieee80211_report_bss(hw: &Ieee80211Hw, bss: &BssInfo) {
    // TODO: Report to cfg80211
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_alloc_hw() {
        let hw = ieee80211_alloc_hw(0);
        assert!(hw.is_some());
        let hw = hw.unwrap();
        assert_eq!(hw.queues, 4);
        assert_eq!(hw.max_rates, 4);
    }
}
