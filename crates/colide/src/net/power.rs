//! WiFi Power Management Module for Colide OS
//!
//! Implements IEEE 802.11 power save modes including:
//! - Legacy Power Save Mode (PSM)
//! - Unscheduled Automatic Power Save Delivery (U-APSD)
//! - Target Wake Time (TWT) for WiFi 6
//!
//! Based on IEEE 802.11-2020 power management specifications.

/// Power save mode
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PowerSaveMode {
    /// Active mode - always listening
    Active,
    /// Legacy Power Save Mode
    LegacyPsm,
    /// U-APSD (WMM Power Save)
    Uapsd,
    /// Target Wake Time (WiFi 6)
    Twt,
}

/// Power save state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PowerState {
    /// Awake - radio active
    Awake,
    /// Doze - radio off, periodic wake
    Doze,
    /// Deep sleep - extended doze
    DeepSleep,
}

/// U-APSD Access Category configuration
#[derive(Debug, Clone, Copy, Default)]
pub struct UapsdConfig {
    /// Voice (AC_VO) trigger/delivery enabled
    pub voice_enabled: bool,
    /// Video (AC_VI) trigger/delivery enabled
    pub video_enabled: bool,
    /// Best Effort (AC_BE) trigger/delivery enabled
    pub best_effort_enabled: bool,
    /// Background (AC_BK) trigger/delivery enabled
    pub background_enabled: bool,
    /// Maximum Service Period length (0=all, 1=2, 2=4, 3=6)
    pub max_sp_length: u8,
}

impl UapsdConfig {
    pub fn all_enabled() -> Self {
        Self {
            voice_enabled: true,
            video_enabled: true,
            best_effort_enabled: true,
            background_enabled: true,
            max_sp_length: 0,
        }
    }
    
    pub fn to_qos_info(&self) -> u8 {
        let mut info = 0u8;
        if self.voice_enabled { info |= 0x01; }
        if self.video_enabled { info |= 0x02; }
        if self.best_effort_enabled { info |= 0x04; }
        if self.background_enabled { info |= 0x08; }
        info |= (self.max_sp_length & 0x03) << 5;
        info
    }
    
    pub fn from_qos_info(info: u8) -> Self {
        Self {
            voice_enabled: (info & 0x01) != 0,
            video_enabled: (info & 0x02) != 0,
            best_effort_enabled: (info & 0x04) != 0,
            background_enabled: (info & 0x08) != 0,
            max_sp_length: (info >> 5) & 0x03,
        }
    }
}

/// Target Wake Time (TWT) parameters for WiFi 6
#[derive(Debug, Clone, Copy)]
pub struct TwtParams {
    /// TWT wake interval exponent
    pub wake_interval_exp: u8,
    /// TWT wake interval mantissa
    pub wake_interval_mantissa: u16,
    /// Minimum TWT wake duration (256 µs units)
    pub min_wake_duration: u8,
    /// TWT channel
    pub twt_channel: u8,
    /// Implicit TWT (vs explicit)
    pub implicit: bool,
    /// Flow type (0=announced, 1=unannounced)
    pub flow_type: u8,
    /// TWT protection enabled
    pub protection: bool,
}

impl TwtParams {
    pub fn default_params() -> Self {
        Self {
            wake_interval_exp: 10,      // ~1 second intervals
            wake_interval_mantissa: 1000,
            min_wake_duration: 255,     // ~65ms minimum wake
            twt_channel: 0,
            implicit: true,
            flow_type: 0,
            protection: false,
        }
    }
    
    pub fn wake_interval_us(&self) -> u64 {
        (self.wake_interval_mantissa as u64) << self.wake_interval_exp
    }
}

/// Listen interval for power save
#[derive(Debug, Clone, Copy)]
pub struct ListenInterval {
    /// Interval in beacon periods
    pub beacon_periods: u16,
    /// DTIM period multiplier
    pub dtim_periods: u8,
}

impl Default for ListenInterval {
    fn default() -> Self {
        Self {
            beacon_periods: 10,  // Wake every 10 beacons (~1 second)
            dtim_periods: 1,     // Wake every DTIM
        }
    }
}

/// Power management statistics
#[derive(Debug, Clone, Copy, Default)]
pub struct PowerStats {
    /// Time spent in awake state (ms)
    pub awake_time_ms: u64,
    /// Time spent in doze state (ms)
    pub doze_time_ms: u64,
    /// Number of wake-ups
    pub wake_count: u32,
    /// Buffered frames received after wake
    pub buffered_frames_rx: u32,
    /// Null frames sent for PS poll
    pub null_frames_tx: u32,
    /// TWT sessions completed
    pub twt_sessions: u32,
}

impl PowerStats {
    pub fn duty_cycle_percent(&self) -> f32 {
        let total = self.awake_time_ms + self.doze_time_ms;
        if total == 0 {
            100.0
        } else {
            (self.awake_time_ms as f32 / total as f32) * 100.0
        }
    }
}

/// Power management controller
pub struct PowerManager {
    mode: PowerSaveMode,
    state: PowerState,
    listen_interval: ListenInterval,
    uapsd_config: UapsdConfig,
    twt_params: Option<TwtParams>,
    stats: PowerStats,
    last_wake_time: u64,
    buffered_at_ap: bool,
}

impl PowerManager {
    pub fn new() -> Self {
        Self {
            mode: PowerSaveMode::Active,
            state: PowerState::Awake,
            listen_interval: ListenInterval::default(),
            uapsd_config: UapsdConfig::default(),
            twt_params: None,
            stats: PowerStats::default(),
            last_wake_time: 0,
            buffered_at_ap: false,
        }
    }
    
    pub fn set_mode(&mut self, mode: PowerSaveMode) {
        self.mode = mode;
        match mode {
            PowerSaveMode::Active => self.wake_up(),
            PowerSaveMode::Uapsd => {
                self.uapsd_config = UapsdConfig::all_enabled();
            }
            PowerSaveMode::Twt => {
                self.twt_params = Some(TwtParams::default_params());
            }
            _ => {}
        }
    }
    
    pub fn mode(&self) -> PowerSaveMode {
        self.mode
    }
    
    pub fn state(&self) -> PowerState {
        self.state
    }
    
    pub fn set_listen_interval(&mut self, interval: ListenInterval) {
        self.listen_interval = interval;
    }
    
    pub fn set_uapsd_config(&mut self, config: UapsdConfig) {
        self.uapsd_config = config;
    }
    
    pub fn set_twt_params(&mut self, params: TwtParams) {
        self.twt_params = Some(params);
    }
    
    pub fn wake_up(&mut self) {
        if self.state != PowerState::Awake {
            self.state = PowerState::Awake;
            self.stats.wake_count += 1;
        }
    }
    
    pub fn enter_doze(&mut self) {
        if self.mode != PowerSaveMode::Active {
            self.state = PowerState::Doze;
        }
    }
    
    pub fn enter_deep_sleep(&mut self) {
        if self.mode != PowerSaveMode::Active {
            self.state = PowerState::DeepSleep;
        }
    }
    
    pub fn set_buffered_at_ap(&mut self, buffered: bool) {
        self.buffered_at_ap = buffered;
        if buffered && self.state != PowerState::Awake {
            self.wake_up();
        }
    }
    
    pub fn should_wake_for_beacon(&self) -> bool {
        matches!(self.mode, PowerSaveMode::LegacyPsm)
    }
    
    pub fn should_wake_for_dtim(&self) -> bool {
        matches!(self.mode, PowerSaveMode::LegacyPsm | PowerSaveMode::Uapsd)
    }
    
    pub fn build_power_cap_ie(&self) -> [u8; 4] {
        // Power Capability IE for association
        // Element ID (33), Length (2), Min TX Power, Max TX Power
        [33, 2, 0, 20]  // 0-20 dBm range
    }
    
    pub fn build_ps_poll_frame(&self, aid: u16, bssid: [u8; 6], sta_addr: [u8; 6]) -> [u8; 16] {
        let mut frame = [0u8; 16];
        // Frame Control: PS-Poll (subtype 10, type 01)
        frame[0] = 0xA4;
        frame[1] = 0x00;
        // AID with bits 14-15 set
        let aid_field = aid | 0xC000;
        frame[2] = aid_field as u8;
        frame[3] = (aid_field >> 8) as u8;
        // BSSID
        frame[4..10].copy_from_slice(&bssid);
        // TA (STA address)
        frame[10..16].copy_from_slice(&sta_addr);
        frame
    }
    
    pub fn build_null_data_frame(&self, to_ds: bool, power_mgmt: bool, 
                                  bssid: [u8; 6], sta_addr: [u8; 6]) -> [u8; 24] {
        let mut frame = [0u8; 24];
        // Frame Control: Null Data
        frame[0] = 0x48;  // Subtype 4, Type 2
        let mut fc1 = 0u8;
        if to_ds { fc1 |= 0x01; }
        if power_mgmt { fc1 |= 0x10; }
        frame[1] = fc1;
        // Duration
        frame[2] = 0;
        frame[3] = 0;
        // Addresses depend on To DS / From DS
        if to_ds {
            frame[4..10].copy_from_slice(&bssid);    // Address 1 = BSSID
            frame[10..16].copy_from_slice(&sta_addr); // Address 2 = SA
            frame[16..22].copy_from_slice(&bssid);    // Address 3 = DA (BSSID for null)
        } else {
            frame[4..10].copy_from_slice(&sta_addr);  // Address 1 = DA
            frame[10..16].copy_from_slice(&bssid);    // Address 2 = BSSID
            frame[16..22].copy_from_slice(&sta_addr); // Address 3 = SA
        }
        // Sequence control
        frame[22] = 0;
        frame[23] = 0;
        frame
    }
    
    pub fn stats(&self) -> &PowerStats {
        &self.stats
    }
    
    pub fn update_stats(&mut self, awake_ms: u64, doze_ms: u64) {
        self.stats.awake_time_ms += awake_ms;
        self.stats.doze_time_ms += doze_ms;
    }
}

impl Default for PowerManager {
    fn default() -> Self {
        Self::new()
    }
}
