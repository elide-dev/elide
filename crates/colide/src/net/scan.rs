//! WiFi Scanning State Machine
//!
//! Implements active and passive scanning with channel management,
//! scan result caching, and BSS selection.

use crate::net::regulatory::ChannelFlags;

/// Scan type
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ScanType {
    #[default]
    Passive,
    Active,
    /// Passive on DFS channels, active on others
    Mixed,
}

/// Scan state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ScanState {
    #[default]
    Idle,
    Starting,
    Scanning,
    Processing,
    Complete,
    Aborted,
}

/// Channel to scan
#[derive(Debug, Clone, Copy)]
pub struct ScanChannel {
    pub frequency: u32,
    pub flags: ChannelFlags,
    /// Dwell time in ms (0 = use default)
    pub dwell_time: u16,
}

impl ScanChannel {
    pub fn new(frequency: u32) -> Self {
        Self {
            frequency,
            flags: ChannelFlags::new(),
            dwell_time: 0,
        }
    }
    
    /// Create from channel number (2.4GHz)
    pub fn from_2ghz_channel(channel: u8) -> Self {
        let freq = if channel <= 13 {
            2407 + (channel as u32) * 5
        } else if channel == 14 {
            2484
        } else {
            2412
        };
        Self::new(freq)
    }
    
    /// Create from channel number (5GHz)
    pub fn from_5ghz_channel(channel: u8) -> Self {
        let freq = 5000 + (channel as u32) * 5;
        Self::new(freq)
    }
    
    /// Is this a DFS channel?
    pub fn is_dfs(&self) -> bool {
        self.flags.contains(ChannelFlags::RADAR)
    }
}

/// Scan request parameters
#[derive(Debug, Clone)]
pub struct ScanRequest {
    /// Scan type
    pub scan_type: ScanType,
    /// Channels to scan (empty = all supported)
    pub channels: Vec<ScanChannel>,
    /// SSIDs to probe for (active scan)
    pub ssids: Vec<Vec<u8>>,
    /// BSSID filter (broadcast = any)
    pub bssid: [u8; 6],
    /// Minimum dwell time per channel (ms)
    pub min_dwell: u16,
    /// Maximum dwell time per channel (ms)
    pub max_dwell: u16,
    /// Number of probe requests per channel (active)
    pub n_probes: u8,
    /// Scan priority (lower = higher priority)
    pub priority: u8,
}

impl Default for ScanRequest {
    fn default() -> Self {
        Self {
            scan_type: ScanType::Active,
            channels: Vec::new(),
            ssids: Vec::new(),
            bssid: [0xFF; 6],  // Broadcast
            min_dwell: 20,
            max_dwell: 100,
            n_probes: 2,
            priority: 0,
        }
    }
}

impl ScanRequest {
    /// Create request for specific SSID
    pub fn for_ssid(ssid: &[u8]) -> Self {
        Self {
            ssids: vec![ssid.to_vec()],
            ..Default::default()
        }
    }
    
    /// Create background scan request
    pub fn background() -> Self {
        Self {
            scan_type: ScanType::Passive,
            min_dwell: 10,
            max_dwell: 50,
            priority: 10,
            ..Default::default()
        }
    }
    
    /// Add all 2.4GHz channels
    pub fn add_2ghz_channels(&mut self) {
        for ch in 1..=14 {
            self.channels.push(ScanChannel::from_2ghz_channel(ch));
        }
    }
    
    /// Add all 5GHz channels (UNII-1, 2, 3)
    pub fn add_5ghz_channels(&mut self) {
        // UNII-1 (5150-5250 MHz)
        for ch in [36, 40, 44, 48] {
            self.channels.push(ScanChannel::from_5ghz_channel(ch));
        }
        // UNII-2A (5250-5350 MHz) - DFS
        for ch in [52, 56, 60, 64] {
            let mut sc = ScanChannel::from_5ghz_channel(ch);
            sc.flags = ChannelFlags::RADAR;
            self.channels.push(sc);
        }
        // UNII-2C (5470-5725 MHz) - DFS
        for ch in [100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144] {
            let mut sc = ScanChannel::from_5ghz_channel(ch);
            sc.flags = ChannelFlags::RADAR;
            self.channels.push(sc);
        }
        // UNII-3 (5725-5850 MHz)
        for ch in [149, 153, 157, 161, 165] {
            self.channels.push(ScanChannel::from_5ghz_channel(ch));
        }
    }
}

/// Scan result (BSS entry)
#[derive(Debug, Clone)]
pub struct ScanResult {
    /// BSSID
    pub bssid: [u8; 6],
    /// SSID
    pub ssid: Vec<u8>,
    /// Frequency (MHz)
    pub frequency: u32,
    /// Signal strength (dBm)
    pub signal: i8,
    /// Noise floor (dBm)
    pub noise: i8,
    /// Beacon interval (TU)
    pub beacon_interval: u16,
    /// Capability info
    pub capability: u16,
    /// Timestamp from beacon/probe response
    pub tsf: u64,
    /// Last seen timestamp (local ms)
    pub last_seen: u64,
    /// Seen count
    pub seen_count: u32,
    /// Security type
    pub security: SecurityType,
    /// WMM/QoS supported
    pub wmm: bool,
    /// HT (802.11n) supported
    pub ht: bool,
    /// VHT (802.11ac) supported
    pub vht: bool,
    /// HE (802.11ax) supported
    pub he: bool,
    /// Channel width (MHz)
    pub channel_width: u8,
    /// Raw IEs for detailed parsing
    pub ies: Vec<u8>,
}

/// Security type
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum SecurityType {
    #[default]
    Open,
    Wep,
    WpaPsk,
    Wpa2Psk,
    Wpa3Sae,
    WpaEnterprise,
    Wpa2Enterprise,
    Wpa3Enterprise,
}

impl ScanResult {
    pub fn new(bssid: [u8; 6], frequency: u32) -> Self {
        Self {
            bssid,
            ssid: Vec::new(),
            frequency,
            signal: -100,
            noise: -95,
            beacon_interval: 100,
            capability: 0,
            tsf: 0,
            last_seen: 0,
            seen_count: 0,
            security: SecurityType::Open,
            wmm: false,
            ht: false,
            vht: false,
            he: false,
            channel_width: 20,
            ies: Vec::new(),
        }
    }
    
    /// Calculate SNR (Signal to Noise Ratio)
    pub fn snr(&self) -> i8 {
        self.signal.saturating_sub(self.noise)
    }
    
    /// Is this an ESS (infrastructure BSS)?
    pub fn is_ess(&self) -> bool {
        (self.capability & 0x0001) != 0
    }
    
    /// Is this an IBSS (ad-hoc)?
    pub fn is_ibss(&self) -> bool {
        (self.capability & 0x0002) != 0
    }
    
    /// Is privacy enabled (WEP/WPA)?
    pub fn is_privacy(&self) -> bool {
        (self.capability & 0x0010) != 0
    }
    
    /// Get channel number
    pub fn channel(&self) -> u8 {
        if self.frequency < 3000 {
            // 2.4 GHz
            if self.frequency == 2484 {
                14
            } else {
                ((self.frequency - 2407) / 5) as u8
            }
        } else {
            // 5 GHz
            ((self.frequency - 5000) / 5) as u8
        }
    }
    
    /// Match SSID
    pub fn matches_ssid(&self, ssid: &[u8]) -> bool {
        self.ssid == ssid
    }
    
    /// Compare signal strength for sorting
    pub fn better_than(&self, other: &Self) -> bool {
        self.signal > other.signal
    }
}

/// Scan result cache
pub struct ScanCache {
    results: Vec<ScanResult>,
    max_age_ms: u64,
    max_entries: usize,
}

impl ScanCache {
    pub fn new() -> Self {
        Self {
            results: Vec::new(),
            max_age_ms: 30000,  // 30 seconds
            max_entries: 64,
        }
    }
    
    /// Add or update scan result
    pub fn update(&mut self, mut result: ScanResult, timestamp: u64) {
        result.last_seen = timestamp;
        
        // Find existing entry
        if let Some(existing) = self.results.iter_mut()
            .find(|r| r.bssid == result.bssid) 
        {
            // Update existing
            existing.signal = result.signal;
            existing.last_seen = timestamp;
            existing.seen_count += 1;
            // Update other fields if beacon/probe
            if !result.ssid.is_empty() {
                existing.ssid = result.ssid;
            }
            existing.security = result.security;
            existing.ht = result.ht;
            existing.vht = result.vht;
            existing.he = result.he;
        } else {
            // Add new
            result.seen_count = 1;
            if self.results.len() >= self.max_entries {
                // Remove oldest
                self.expire(timestamp);
                if self.results.len() >= self.max_entries {
                    // Remove weakest signal
                    if let Some(idx) = self.results.iter()
                        .enumerate()
                        .min_by_key(|(_, r)| r.signal)
                        .map(|(i, _)| i)
                    {
                        self.results.remove(idx);
                    }
                }
            }
            self.results.push(result);
        }
    }
    
    /// Remove expired entries
    pub fn expire(&mut self, current_time: u64) {
        self.results.retain(|r| {
            current_time - r.last_seen < self.max_age_ms
        });
    }
    
    /// Get all results sorted by signal strength
    pub fn get_sorted(&self) -> Vec<&ScanResult> {
        let mut sorted: Vec<_> = self.results.iter().collect();
        sorted.sort_by(|a, b| b.signal.cmp(&a.signal));
        sorted
    }
    
    /// Find by SSID
    pub fn find_ssid(&self, ssid: &[u8]) -> Vec<&ScanResult> {
        self.results.iter()
            .filter(|r| r.ssid == ssid)
            .collect()
    }
    
    /// Find by BSSID
    pub fn find_bssid(&self, bssid: &[u8; 6]) -> Option<&ScanResult> {
        self.results.iter().find(|r| &r.bssid == bssid)
    }
    
    /// Get best candidate for SSID
    pub fn best_for_ssid(&self, ssid: &[u8]) -> Option<&ScanResult> {
        self.results.iter()
            .filter(|r| r.ssid == ssid)
            .max_by_key(|r| r.signal)
    }
    
    /// Clear all results
    pub fn clear(&mut self) {
        self.results.clear();
    }
    
    /// Get result count
    pub fn len(&self) -> usize {
        self.results.len()
    }
    
    pub fn is_empty(&self) -> bool {
        self.results.is_empty()
    }
}

impl Default for ScanCache {
    fn default() -> Self {
        Self::new()
    }
}

/// Scan state machine
pub struct ScanStateMachine {
    state: ScanState,
    request: Option<ScanRequest>,
    current_channel_idx: usize,
    channel_start_time: u64,
    probes_sent: u8,
    cache: ScanCache,
    /// Callback for channel change
    on_channel_change: Option<Box<dyn Fn(u32) + Send>>,
}

impl ScanStateMachine {
    pub fn new() -> Self {
        Self {
            state: ScanState::Idle,
            request: None,
            current_channel_idx: 0,
            channel_start_time: 0,
            probes_sent: 0,
            cache: ScanCache::new(),
            on_channel_change: None,
        }
    }
    
    /// Start a new scan
    pub fn start(&mut self, request: ScanRequest) -> Result<(), &'static str> {
        if self.state != ScanState::Idle && self.state != ScanState::Complete {
            return Err("Scan already in progress");
        }
        
        self.request = Some(request);
        self.current_channel_idx = 0;
        self.state = ScanState::Starting;
        Ok(())
    }
    
    /// Abort current scan
    pub fn abort(&mut self) {
        if self.state == ScanState::Scanning || self.state == ScanState::Starting {
            self.state = ScanState::Aborted;
        }
    }
    
    /// Process scan state machine tick
    pub fn tick(&mut self, current_time: u64) -> ScanAction {
        match self.state {
            ScanState::Idle => ScanAction::None,
            
            ScanState::Starting => {
                self.state = ScanState::Scanning;
                self.channel_start_time = current_time;
                self.probes_sent = 0;
                
                if let Some(req) = &self.request {
                    if let Some(ch) = req.channels.get(0) {
                        return ScanAction::SetChannel(ch.frequency);
                    }
                }
                ScanAction::Complete
            }
            
            ScanState::Scanning => {
                let req = match &self.request {
                    Some(r) => r,
                    None => {
                        self.state = ScanState::Complete;
                        return ScanAction::Complete;
                    }
                };
                
                let channel = match req.channels.get(self.current_channel_idx) {
                    Some(c) => c,
                    None => {
                        self.state = ScanState::Processing;
                        return ScanAction::Process;
                    }
                };
                
                let dwell = if channel.dwell_time > 0 {
                    channel.dwell_time as u64
                } else {
                    req.max_dwell as u64
                };
                
                let elapsed = current_time - self.channel_start_time;
                
                // Send probe requests for active scan
                if req.scan_type != ScanType::Passive && !channel.is_dfs() {
                    if self.probes_sent < req.n_probes && elapsed >= (self.probes_sent as u64 * 10) {
                        self.probes_sent += 1;
                        return ScanAction::SendProbe(channel.frequency);
                    }
                }
                
                // Check if dwell time elapsed
                if elapsed >= dwell {
                    self.current_channel_idx += 1;
                    self.channel_start_time = current_time;
                    self.probes_sent = 0;
                    
                    if let Some(next_ch) = req.channels.get(self.current_channel_idx) {
                        return ScanAction::SetChannel(next_ch.frequency);
                    } else {
                        self.state = ScanState::Processing;
                        return ScanAction::Process;
                    }
                }
                
                ScanAction::Wait(dwell - elapsed)
            }
            
            ScanState::Processing => {
                self.cache.expire(current_time);
                self.state = ScanState::Complete;
                ScanAction::Complete
            }
            
            ScanState::Complete | ScanState::Aborted => ScanAction::None,
        }
    }
    
    /// Handle received beacon/probe response
    pub fn rx_beacon(&mut self, result: ScanResult, timestamp: u64) {
        self.cache.update(result, timestamp);
    }
    
    /// Get current state
    pub fn state(&self) -> ScanState {
        self.state
    }
    
    /// Get scan results
    pub fn results(&self) -> &ScanCache {
        &self.cache
    }
    
    /// Reset to idle
    pub fn reset(&mut self) {
        self.state = ScanState::Idle;
        self.request = None;
        self.current_channel_idx = 0;
    }
}

impl Default for ScanStateMachine {
    fn default() -> Self {
        Self::new()
    }
}

/// Action to take from scan state machine
#[derive(Debug, Clone, Copy)]
pub enum ScanAction {
    None,
    SetChannel(u32),
    SendProbe(u32),
    Wait(u64),
    Process,
    Complete,
}
