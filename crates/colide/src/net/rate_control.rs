//! Rate Control Algorithm for WiFi
//!
//! Implements a Minstrel-like rate control algorithm that dynamically
//! adjusts transmission rates based on success/failure statistics.
//!
//! Based on the Linux mac80211 Minstrel rate control algorithm.

/// MCS (Modulation and Coding Scheme) index for HT/VHT/HE
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct McsIndex(pub u8);

impl McsIndex {
    pub const MCS0: Self = Self(0);
    pub const MCS1: Self = Self(1);
    pub const MCS2: Self = Self(2);
    pub const MCS3: Self = Self(3);
    pub const MCS4: Self = Self(4);
    pub const MCS5: Self = Self(5);
    pub const MCS6: Self = Self(6);
    pub const MCS7: Self = Self(7);
    pub const MCS8: Self = Self(8);  // 2 spatial streams
    pub const MCS9: Self = Self(9);
    
    /// Get data rate in Kbps for 20MHz channel
    pub fn rate_20mhz_kbps(&self, short_gi: bool) -> u32 {
        let base = match self.0 {
            0 => 6500,
            1 => 13000,
            2 => 19500,
            3 => 26000,
            4 => 39000,
            5 => 52000,
            6 => 58500,
            7 => 65000,
            8 => 13000,  // 2SS MCS0
            9 => 26000,  // 2SS MCS1
            _ => 6500,
        };
        if short_gi { base * 10 / 9 } else { base }
    }
    
    /// Get data rate in Kbps for 40MHz channel
    pub fn rate_40mhz_kbps(&self, short_gi: bool) -> u32 {
        self.rate_20mhz_kbps(short_gi) * 2
    }
}

/// Legacy (802.11a/b/g) rate
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct LegacyRate(pub u8);

impl LegacyRate {
    pub const RATE_1M: Self = Self(2);    // 1 Mbps (0.5 Mbps units)
    pub const RATE_2M: Self = Self(4);
    pub const RATE_5_5M: Self = Self(11);
    pub const RATE_11M: Self = Self(22);
    pub const RATE_6M: Self = Self(12);   // OFDM
    pub const RATE_9M: Self = Self(18);
    pub const RATE_12M: Self = Self(24);
    pub const RATE_18M: Self = Self(36);
    pub const RATE_24M: Self = Self(48);
    pub const RATE_36M: Self = Self(72);
    pub const RATE_48M: Self = Self(96);
    pub const RATE_54M: Self = Self(108);
    
    /// Get rate in Kbps
    pub fn rate_kbps(&self) -> u32 {
        (self.0 as u32) * 500
    }
}

/// Rate type (legacy or MCS)
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum RateType {
    Legacy(LegacyRate),
    HtMcs(McsIndex),
    VhtMcs(McsIndex, u8),  // MCS, NSS
    HeMcs(McsIndex, u8),   // MCS, NSS
}

/// Statistics for a single rate
#[derive(Debug, Clone, Copy, Default)]
pub struct RateStats {
    /// Attempts at this rate
    pub attempts: u32,
    /// Successful transmissions
    pub success: u32,
    /// Last update timestamp (ms)
    pub last_update: u64,
    /// Throughput estimate (Kbps)
    pub throughput: u32,
    /// Probability of success (0-1000, scaled)
    pub probability: u32,
    /// Perfect TX time (us)
    pub perfect_tx_time: u32,
}

impl RateStats {
    /// Calculate success probability (0-1000 scale)
    pub fn calc_probability(&self) -> u32 {
        if self.attempts == 0 {
            return 0;
        }
        ((self.success as u64 * 1000) / self.attempts as u64) as u32
    }
    
    /// Update statistics with exponential moving average
    pub fn update(&mut self, success: bool, timestamp: u64) {
        self.attempts += 1;
        if success {
            self.success += 1;
        }
        
        // Update probability with EWMA (alpha = 0.25)
        let new_prob = if success { 1000 } else { 0 };
        self.probability = (self.probability * 3 + new_prob) / 4;
        
        // Update throughput estimate
        if self.perfect_tx_time > 0 && self.probability > 0 {
            self.throughput = (self.probability * 1000) / self.perfect_tx_time;
        }
        
        self.last_update = timestamp;
    }
}

/// Rate table entry
#[derive(Debug, Clone, Copy)]
pub struct RateEntry {
    pub rate: RateType,
    pub stats: RateStats,
    /// Number of retries at this rate
    pub retry_count: u8,
    /// Is this rate currently usable?
    pub enabled: bool,
}

impl RateEntry {
    pub fn new(rate: RateType, tx_time_us: u32) -> Self {
        Self {
            rate,
            stats: RateStats {
                perfect_tx_time: tx_time_us,
                ..Default::default()
            },
            retry_count: 2,
            enabled: true,
        }
    }
}

/// Minstrel-like rate control state
pub struct RateControl {
    /// Rate table
    rates: Vec<RateEntry>,
    /// Index of best throughput rate
    best_tp_idx: usize,
    /// Index of second best throughput rate
    second_tp_idx: usize,
    /// Index of highest probability rate
    best_prob_idx: usize,
    /// Index of lowest rate (fallback)
    lowest_idx: usize,
    /// Sample rate index
    sample_idx: usize,
    /// Sample counter
    sample_count: u32,
    /// Total packet count
    total_packets: u64,
    /// Update interval (ms)
    update_interval: u64,
    /// Last update timestamp
    last_update: u64,
}

impl RateControl {
    /// Create new rate control with default rates
    pub fn new() -> Self {
        let mut rates = Vec::new();
        
        // Add legacy OFDM rates
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_6M), 200));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_9M), 150));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_12M), 100));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_18M), 80));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_24M), 60));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_36M), 45));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_48M), 35));
        rates.push(RateEntry::new(RateType::Legacy(LegacyRate::RATE_54M), 30));
        
        // Add HT MCS rates
        for i in 0..8 {
            let tx_time = 100 - i * 10;  // Simplified TX time
            rates.push(RateEntry::new(RateType::HtMcs(McsIndex(i)), tx_time as u32));
        }
        
        Self {
            rates,
            best_tp_idx: 0,
            second_tp_idx: 0,
            best_prob_idx: 0,
            lowest_idx: 0,
            sample_idx: 0,
            sample_count: 0,
            total_packets: 0,
            update_interval: 100,  // 100ms
            last_update: 0,
        }
    }
    
    /// Add HT rates
    pub fn add_ht_rates(&mut self, max_mcs: u8) {
        for i in 0..=max_mcs {
            let tx_time = 100 - (i as u32) * 8;
            self.rates.push(RateEntry::new(RateType::HtMcs(McsIndex(i)), tx_time.max(20)));
        }
    }
    
    /// Get best rate for transmission
    pub fn get_tx_rate(&mut self) -> &RateEntry {
        // Minstrel sampling: 10% random sampling
        self.sample_count += 1;
        if self.sample_count % 10 == 0 {
            // Sample a random rate
            self.sample_idx = (self.sample_idx + 1) % self.rates.len();
            return &self.rates[self.sample_idx];
        }
        
        // Use best throughput rate
        &self.rates[self.best_tp_idx]
    }
    
    /// Get retry chain for transmission
    pub fn get_retry_chain(&self) -> [(usize, u8); 4] {
        [
            (self.best_tp_idx, 2),
            (self.second_tp_idx, 2),
            (self.best_prob_idx, 2),
            (self.lowest_idx, 4),
        ]
    }
    
    /// Report TX status
    pub fn tx_status(&mut self, rate_idx: usize, success: bool, timestamp: u64) {
        if rate_idx < self.rates.len() {
            self.rates[rate_idx].stats.update(success, timestamp);
        }
        
        self.total_packets += 1;
        
        // Update rate selection periodically
        if timestamp - self.last_update >= self.update_interval {
            self.update_rates();
            self.last_update = timestamp;
        }
    }
    
    /// Update rate selection based on statistics
    fn update_rates(&mut self) {
        let mut best_tp = 0u32;
        let mut second_tp = 0u32;
        let mut best_prob = 0u32;
        
        for (i, rate) in self.rates.iter().enumerate() {
            if !rate.enabled {
                continue;
            }
            
            let tp = rate.stats.throughput;
            let prob = rate.stats.probability;
            
            if tp > best_tp {
                second_tp = best_tp;
                self.second_tp_idx = self.best_tp_idx;
                best_tp = tp;
                self.best_tp_idx = i;
            } else if tp > second_tp {
                second_tp = tp;
                self.second_tp_idx = i;
            }
            
            if prob > best_prob {
                best_prob = prob;
                self.best_prob_idx = i;
            }
        }
    }
    
    /// Get current statistics
    pub fn get_stats(&self) -> RateControlStats {
        RateControlStats {
            total_packets: self.total_packets,
            best_rate_idx: self.best_tp_idx,
            best_throughput: self.rates.get(self.best_tp_idx)
                .map(|r| r.stats.throughput).unwrap_or(0),
            best_probability: self.rates.get(self.best_prob_idx)
                .map(|r| r.stats.probability).unwrap_or(0),
        }
    }
}

impl Default for RateControl {
    fn default() -> Self {
        Self::new()
    }
}

/// Rate control statistics summary
#[derive(Debug, Clone, Copy)]
pub struct RateControlStats {
    pub total_packets: u64,
    pub best_rate_idx: usize,
    pub best_throughput: u32,
    pub best_probability: u32,
}

/// Fixed rate configuration (disable auto rate control)
pub struct FixedRate {
    pub rate: RateType,
    pub retry_count: u8,
}

impl FixedRate {
    pub fn legacy(rate: LegacyRate) -> Self {
        Self {
            rate: RateType::Legacy(rate),
            retry_count: 4,
        }
    }
    
    pub fn ht_mcs(mcs: u8) -> Self {
        Self {
            rate: RateType::HtMcs(McsIndex(mcs)),
            retry_count: 4,
        }
    }
}
