//! WiFi Roaming Support
//!
//! Implements 802.11r (Fast BSS Transition) and legacy roaming
//! for seamless handoff between access points.

use crate::net::scan::ScanResult;

/// Roaming trigger reason
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum RoamReason {
    /// Signal dropped below threshold
    LowSignal,
    /// Beacon loss detected
    BeaconLoss,
    /// Load balancing request from AP
    LoadBalance,
    /// User-initiated (e.g., preferred BSSID)
    UserRequest,
    /// Regulatory change requires channel change
    Regulatory,
    /// Better AP found during background scan
    BetterAp,
}

/// Roaming decision
#[derive(Debug, Clone)]
pub enum RoamDecision {
    /// Stay on current AP
    Stay,
    /// Roam to new AP
    Roam(ScanResult),
    /// Disconnect (no suitable AP)
    Disconnect,
}

/// Roaming configuration
#[derive(Debug, Clone, Copy)]
pub struct RoamConfig {
    /// Enable roaming
    pub enabled: bool,
    /// Minimum signal threshold (dBm) before considering roam
    pub signal_threshold: i8,
    /// Signal delta required to trigger roam to better AP
    pub signal_delta: i8,
    /// Beacon miss count before declaring beacon loss
    pub beacon_miss_threshold: u8,
    /// Minimum time between roam attempts (ms)
    pub roam_cooldown_ms: u64,
    /// Enable background scanning while connected
    pub background_scan: bool,
    /// Background scan interval (ms)
    pub bg_scan_interval_ms: u64,
    /// Enable 802.11r Fast BSS Transition
    pub fast_transition: bool,
    /// Prefer 5GHz over 2.4GHz
    pub prefer_5ghz: bool,
    /// 5GHz signal bonus (dB) for comparison
    pub band_5ghz_bonus: i8,
}

impl Default for RoamConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            signal_threshold: -75,
            signal_delta: 10,
            beacon_miss_threshold: 5,
            roam_cooldown_ms: 5000,
            background_scan: true,
            bg_scan_interval_ms: 30000,
            fast_transition: true,
            prefer_5ghz: true,
            band_5ghz_bonus: 5,
        }
    }
}

/// Roaming state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum RoamState {
    #[default]
    Idle,
    BackgroundScanning,
    Evaluating,
    PreparingTransition,
    Transitioning,
    Cooldown,
}

/// Roaming statistics
#[derive(Debug, Clone, Copy, Default)]
pub struct RoamStats {
    pub roam_count: u32,
    pub roam_success: u32,
    pub roam_failed: u32,
    pub last_roam_time: u64,
    pub avg_roam_time_ms: u32,
}

/// 802.11r Fast Transition info
#[derive(Debug, Clone)]
pub struct FtInfo {
    /// Mobility Domain ID
    pub mdid: [u8; 2],
    /// R0 Key Holder ID
    pub r0kh_id: Vec<u8>,
    /// R1 Key Holder ID
    pub r1kh_id: [u8; 6],
    /// PMK-R0 Name
    pub pmkr0_name: [u8; 16],
    /// PMK-R1 Name
    pub pmkr1_name: [u8; 16],
    /// FT Capability
    pub ft_capability: u8,
}

impl FtInfo {
    pub fn new(mdid: [u8; 2]) -> Self {
        Self {
            mdid,
            r0kh_id: Vec::new(),
            r1kh_id: [0; 6],
            pmkr0_name: [0; 16],
            pmkr1_name: [0; 16],
            ft_capability: 0,
        }
    }
    
    /// Check if FT over-the-air is supported
    pub fn supports_ft_ota(&self) -> bool {
        (self.ft_capability & 0x01) != 0
    }
    
    /// Check if FT over-DS is supported
    pub fn supports_ft_ds(&self) -> bool {
        (self.ft_capability & 0x02) != 0
    }
}

/// Roaming manager
pub struct RoamManager {
    config: RoamConfig,
    state: RoamState,
    stats: RoamStats,
    current_bss: Option<ScanResult>,
    candidates: Vec<RoamCandidate>,
    ft_info: Option<FtInfo>,
    last_bg_scan: u64,
    last_signal: i8,
    beacon_miss_count: u8,
    cooldown_until: u64,
}

/// Roam candidate with score
#[derive(Debug, Clone)]
pub struct RoamCandidate {
    pub bss: ScanResult,
    pub score: i32,
    pub reason: RoamReason,
}

impl RoamManager {
    pub fn new(config: RoamConfig) -> Self {
        Self {
            config,
            state: RoamState::Idle,
            stats: RoamStats::default(),
            current_bss: None,
            candidates: Vec::new(),
            ft_info: None,
            last_bg_scan: 0,
            last_signal: -100,
            beacon_miss_count: 0,
            cooldown_until: 0,
        }
    }
    
    /// Set current BSS after association
    pub fn set_current_bss(&mut self, bss: ScanResult) {
        self.current_bss = Some(bss);
        self.beacon_miss_count = 0;
        self.candidates.clear();
    }
    
    /// Clear current BSS on disconnect
    pub fn clear_current_bss(&mut self) {
        self.current_bss = None;
        self.ft_info = None;
        self.state = RoamState::Idle;
    }
    
    /// Set FT info from association
    pub fn set_ft_info(&mut self, ft: FtInfo) {
        self.ft_info = Some(ft);
    }
    
    /// Update signal strength
    pub fn update_signal(&mut self, signal: i8, timestamp: u64) {
        self.last_signal = signal;
        self.beacon_miss_count = 0;
        
        // Check if signal dropped below threshold
        if self.config.enabled && signal < self.config.signal_threshold {
            if timestamp >= self.cooldown_until {
                self.state = RoamState::Evaluating;
            }
        }
    }
    
    /// Report beacon miss
    pub fn beacon_missed(&mut self) {
        self.beacon_miss_count += 1;
    }
    
    /// Check if should start background scan
    pub fn should_bg_scan(&self, timestamp: u64) -> bool {
        self.config.enabled && 
        self.config.background_scan &&
        self.current_bss.is_some() &&
        timestamp - self.last_bg_scan >= self.config.bg_scan_interval_ms &&
        self.state == RoamState::Idle
    }
    
    /// Start background scan
    pub fn start_bg_scan(&mut self, timestamp: u64) {
        self.state = RoamState::BackgroundScanning;
        self.last_bg_scan = timestamp;
    }
    
    /// Process scan results and decide on roaming
    pub fn evaluate_candidates(&mut self, results: &[ScanResult], timestamp: u64) -> RoamDecision {
        if !self.config.enabled {
            self.state = RoamState::Idle;
            return RoamDecision::Stay;
        }
        
        let current = match &self.current_bss {
            Some(b) => b,
            None => {
                self.state = RoamState::Idle;
                return RoamDecision::Stay;
            }
        };
        
        // Check beacon loss
        if self.beacon_miss_count >= self.config.beacon_miss_threshold {
            // Find any valid AP with same SSID
            if let Some(best) = results.iter()
                .filter(|r| r.ssid == current.ssid && r.bssid != current.bssid)
                .max_by_key(|r| self.score_candidate(r))
            {
                self.state = RoamState::PreparingTransition;
                return RoamDecision::Roam(best.clone());
            }
            self.state = RoamState::Idle;
            return RoamDecision::Disconnect;
        }
        
        // Build scored candidate list
        self.candidates.clear();
        for result in results {
            if result.ssid != current.ssid {
                continue;
            }
            if result.bssid == current.bssid {
                continue;
            }
            
            let score = self.score_candidate(result);
            let current_score = self.score_candidate(current);
            
            // Only consider if significantly better
            if score > current_score + self.config.signal_delta as i32 {
                self.candidates.push(RoamCandidate {
                    bss: result.clone(),
                    score,
                    reason: RoamReason::BetterAp,
                });
            }
        }
        
        // Sort by score
        self.candidates.sort_by(|a, b| b.score.cmp(&a.score));
        
        // Check cooldown
        if timestamp < self.cooldown_until {
            self.state = RoamState::Cooldown;
            return RoamDecision::Stay;
        }
        
        // Pick best candidate if any
        if let Some(candidate) = self.candidates.first() {
            self.state = RoamState::PreparingTransition;
            return RoamDecision::Roam(candidate.bss.clone());
        }
        
        self.state = RoamState::Idle;
        RoamDecision::Stay
    }
    
    /// Score a BSS for roaming comparison
    fn score_candidate(&self, bss: &ScanResult) -> i32 {
        let mut score = bss.signal as i32;
        
        // 5GHz bonus
        if self.config.prefer_5ghz && bss.frequency >= 5000 {
            score += self.config.band_5ghz_bonus as i32;
        }
        
        // HT/VHT/HE capability bonus
        if bss.he { score += 5; }
        else if bss.vht { score += 3; }
        else if bss.ht { score += 1; }
        
        // Channel width bonus
        score += (bss.channel_width / 20) as i32;
        
        // Penalize high noise
        score += bss.snr() as i32 / 2;
        
        score
    }
    
    /// Report roam started
    pub fn roam_started(&mut self, timestamp: u64) {
        self.state = RoamState::Transitioning;
        self.stats.roam_count += 1;
        self.stats.last_roam_time = timestamp;
    }
    
    /// Report roam completed
    pub fn roam_completed(&mut self, success: bool, new_bss: Option<ScanResult>, timestamp: u64) {
        if success {
            self.stats.roam_success += 1;
            if let Some(bss) = new_bss {
                self.current_bss = Some(bss);
            }
        } else {
            self.stats.roam_failed += 1;
        }
        
        // Update average roam time
        let roam_time = (timestamp - self.stats.last_roam_time) as u32;
        self.stats.avg_roam_time_ms = 
            (self.stats.avg_roam_time_ms * (self.stats.roam_count - 1) + roam_time) 
            / self.stats.roam_count;
        
        // Enter cooldown
        self.cooldown_until = timestamp + self.config.roam_cooldown_ms;
        self.state = RoamState::Cooldown;
        self.beacon_miss_count = 0;
    }
    
    pub fn state(&self) -> RoamState {
        self.state
    }
    
    pub fn stats(&self) -> &RoamStats {
        &self.stats
    }
    
    pub fn config(&self) -> &RoamConfig {
        &self.config
    }
    
    pub fn config_mut(&mut self) -> &mut RoamConfig {
        &mut self.config
    }
}

impl Default for RoamManager {
    fn default() -> Self {
        Self::new(RoamConfig::default())
    }
}

/// FT Action frame builder
pub struct FtActionBuilder {
    pub category: u8,
    pub action: u8,
    pub sta_addr: [u8; 6],
    pub target_ap: [u8; 6],
}

impl FtActionBuilder {
    pub const CATEGORY_FT: u8 = 6;
    pub const ACTION_REQUEST: u8 = 1;
    pub const ACTION_RESPONSE: u8 = 2;
    pub const ACTION_CONFIRM: u8 = 3;
    pub const ACTION_ACK: u8 = 4;
    
    /// Build FT Request frame
    pub fn request(sta: [u8; 6], target_ap: [u8; 6], ft_info: &FtInfo) -> Vec<u8> {
        let mut frame = Vec::with_capacity(64);
        
        // Category
        frame.push(Self::CATEGORY_FT);
        // Action
        frame.push(Self::ACTION_REQUEST);
        // STA Address
        frame.extend_from_slice(&sta);
        // Target AP Address
        frame.extend_from_slice(&target_ap);
        
        // FT Info IE (MDIE)
        frame.push(54);  // Element ID: Mobility Domain
        frame.push(3);   // Length
        frame.extend_from_slice(&ft_info.mdid);
        frame.push(ft_info.ft_capability);
        
        // FTIE would go here (complex, simplified for now)
        
        frame
    }
}
