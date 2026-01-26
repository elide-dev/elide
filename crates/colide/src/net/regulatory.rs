//! Regulatory Domain Support for WiFi
//!
//! Defines channel frequencies, power limits, and usage rules per country.
//! Based on IEEE 802.11 regulatory requirements and Linux CRDA database.

/// 2.4 GHz channel frequencies (MHz)
pub mod Channel2g {
    pub const CH1: u32 = 2412;
    pub const CH2: u32 = 2417;
    pub const CH3: u32 = 2422;
    pub const CH4: u32 = 2427;
    pub const CH5: u32 = 2432;
    pub const CH6: u32 = 2437;
    pub const CH7: u32 = 2442;
    pub const CH8: u32 = 2447;
    pub const CH9: u32 = 2452;
    pub const CH10: u32 = 2457;
    pub const CH11: u32 = 2462;
    pub const CH12: u32 = 2467;
    pub const CH13: u32 = 2472;
    pub const CH14: u32 = 2484;  // Japan only
}

/// 5 GHz UNII band channel frequencies (MHz)
pub mod Channel5g {
    // UNII-1 (5150-5250 MHz) - Indoor only in most countries
    pub const CH36: u32 = 5180;
    pub const CH40: u32 = 5200;
    pub const CH44: u32 = 5220;
    pub const CH48: u32 = 5240;
    
    // UNII-2A (5250-5350 MHz) - DFS required
    pub const CH52: u32 = 5260;
    pub const CH56: u32 = 5280;
    pub const CH60: u32 = 5300;
    pub const CH64: u32 = 5320;
    
    // UNII-2C (5470-5725 MHz) - DFS required
    pub const CH100: u32 = 5500;
    pub const CH104: u32 = 5520;
    pub const CH108: u32 = 5540;
    pub const CH112: u32 = 5560;
    pub const CH116: u32 = 5580;
    pub const CH120: u32 = 5600;
    pub const CH124: u32 = 5620;
    pub const CH128: u32 = 5640;
    pub const CH132: u32 = 5660;
    pub const CH136: u32 = 5680;
    pub const CH140: u32 = 5700;
    pub const CH144: u32 = 5720;
    
    // UNII-3 (5725-5850 MHz)
    pub const CH149: u32 = 5745;
    pub const CH153: u32 = 5765;
    pub const CH157: u32 = 5785;
    pub const CH161: u32 = 5805;
    pub const CH165: u32 = 5825;
}

/// 6 GHz channel frequencies for WiFi 6E (MHz)
pub mod Channel6g {
    pub const CH1: u32 = 5955;
    pub const CH5: u32 = 5975;
    pub const CH9: u32 = 5995;
    pub const CH13: u32 = 6015;
    pub const CH17: u32 = 6035;
    pub const CH21: u32 = 6055;
    pub const CH25: u32 = 6075;
    pub const CH29: u32 = 6095;
    pub const CH33: u32 = 6115;
    // ... continues to CH233 (7115 MHz)
    pub const CH233: u32 = 7115;
}

/// Channel flags
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct ChannelFlags(u32);

impl ChannelFlags {
    pub const DISABLED: Self = Self(1 << 0);
    pub const NO_IR: Self = Self(1 << 1);        // No initiating radiation (passive scan only)
    pub const RADAR: Self = Self(1 << 2);        // DFS required
    pub const NO_HT40_PLUS: Self = Self(1 << 3);
    pub const NO_HT40_MINUS: Self = Self(1 << 4);
    pub const NO_80MHZ: Self = Self(1 << 5);
    pub const NO_160MHZ: Self = Self(1 << 6);
    pub const INDOOR_ONLY: Self = Self(1 << 7);
    pub const IR_CONCURRENT: Self = Self(1 << 8);
    pub const NO_20MHZ: Self = Self(1 << 9);
    pub const NO_10MHZ: Self = Self(1 << 10);
    pub const NO_HE: Self = Self(1 << 11);       // No WiFi 6
    
    pub fn contains(&self, other: Self) -> bool {
        (self.0 & other.0) == other.0
    }
    
    pub fn union(&self, other: Self) -> Self {
        Self(self.0 | other.0)
    }
}

/// Regulatory rule for a frequency range
#[derive(Debug, Clone, Copy)]
pub struct RegRule {
    /// Start frequency (kHz)
    pub start_freq_khz: u32,
    /// End frequency (kHz)
    pub end_freq_khz: u32,
    /// Maximum bandwidth (kHz)
    pub max_bandwidth_khz: u32,
    /// Maximum antenna gain (mBi = 0.01 dBi)
    pub max_antenna_gain_mbi: u32,
    /// Maximum EIRP (mBm = 0.01 dBm)
    pub max_eirp_mbm: u32,
    /// DFS CAC time (ms), 0 if no DFS
    pub dfs_cac_ms: u32,
    /// Channel flags
    pub flags: ChannelFlags,
}

impl RegRule {
    pub const fn new(start_mhz: u32, end_mhz: u32, bw_mhz: u32, eirp_dbm: i32) -> Self {
        Self {
            start_freq_khz: start_mhz * 1000,
            end_freq_khz: end_mhz * 1000,
            max_bandwidth_khz: bw_mhz * 1000,
            max_antenna_gain_mbi: 0,
            max_eirp_mbm: (eirp_dbm * 100) as u32,
            dfs_cac_ms: 0,
            flags: ChannelFlags(0),
        }
    }
    
    pub const fn with_dfs(mut self, cac_ms: u32) -> Self {
        self.dfs_cac_ms = cac_ms;
        self.flags = ChannelFlags(self.flags.0 | ChannelFlags::RADAR.0);
        self
    }
    
    pub const fn with_flags(mut self, flags: ChannelFlags) -> Self {
        self.flags = ChannelFlags(self.flags.0 | flags.0);
        self
    }
}

/// Regulatory domain (country code)
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct RegDomain {
    pub alpha2: [u8; 2],
    pub dfs_region: DfsRegion,
}

impl RegDomain {
    pub const fn new(alpha2: [u8; 2], dfs_region: DfsRegion) -> Self {
        Self { alpha2, dfs_region }
    }
    
    pub fn as_str(&self) -> &str {
        core::str::from_utf8(&self.alpha2).unwrap_or("??")
    }
}

/// DFS (Dynamic Frequency Selection) region
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DfsRegion {
    Unset = 0,
    Fcc = 1,    // US, Canada, etc.
    Etsi = 2,   // Europe
    Jp = 3,     // Japan
}

/// Complete regulatory database entry
pub struct RegDb {
    pub domain: RegDomain,
    pub rules: &'static [RegRule],
}

/// World regulatory domain (most restrictive)
pub static REG_WORLD: RegDb = RegDb {
    domain: RegDomain::new([b'0', b'0'], DfsRegion::Unset),
    rules: &[
        // 2.4 GHz: channels 1-11 only, 20 dBm
        RegRule::new(2402, 2472, 40, 20),
        // 5 GHz UNII-1: passive scan only
        RegRule::new(5170, 5250, 80, 20).with_flags(ChannelFlags::NO_IR),
    ],
};

/// US regulatory domain (FCC)
pub static REG_US: RegDb = RegDb {
    domain: RegDomain::new([b'U', b'S'], DfsRegion::Fcc),
    rules: &[
        // 2.4 GHz: channels 1-11, 30 dBm
        RegRule::new(2402, 2472, 40, 30),
        // 5 GHz UNII-1: indoor only, 23 dBm
        RegRule::new(5170, 5250, 80, 23).with_flags(ChannelFlags::INDOOR_ONLY),
        // 5 GHz UNII-2A: DFS, 23 dBm
        RegRule::new(5250, 5330, 80, 23).with_dfs(60000),
        // 5 GHz UNII-2C: DFS, 23 dBm
        RegRule::new(5490, 5730, 160, 23).with_dfs(60000),
        // 5 GHz UNII-3: 30 dBm
        RegRule::new(5735, 5835, 80, 30),
    ],
};

/// EU regulatory domain (ETSI)
pub static REG_EU: RegDb = RegDb {
    domain: RegDomain::new([b'E', b'U'], DfsRegion::Etsi),
    rules: &[
        // 2.4 GHz: channels 1-13, 20 dBm
        RegRule::new(2402, 2482, 40, 20),
        // 5 GHz UNII-1: indoor only, 23 dBm
        RegRule::new(5170, 5250, 80, 23).with_flags(ChannelFlags::INDOOR_ONLY),
        // 5 GHz UNII-2A: DFS, 23 dBm
        RegRule::new(5250, 5330, 80, 23).with_dfs(60000),
        // 5 GHz UNII-2C: DFS, 30 dBm
        RegRule::new(5490, 5710, 160, 30).with_dfs(60000),
    ],
};

/// Japan regulatory domain
pub static REG_JP: RegDb = RegDb {
    domain: RegDomain::new([b'J', b'P'], DfsRegion::Jp),
    rules: &[
        // 2.4 GHz: channels 1-13 + 14 (DSSS only), 20 dBm
        RegRule::new(2402, 2482, 40, 20),
        RegRule::new(2474, 2494, 20, 20), // Channel 14
        // 5 GHz UNII-1: indoor only, 23 dBm
        RegRule::new(5170, 5250, 80, 23).with_flags(ChannelFlags::INDOOR_ONLY),
        // 5 GHz UNII-2A: DFS, 23 dBm
        RegRule::new(5250, 5330, 80, 23).with_dfs(60000),
        // 5 GHz UNII-2C: DFS, 23 dBm (wider CAC)
        RegRule::new(5490, 5710, 160, 23).with_dfs(60000),
    ],
};

/// Regulatory domain manager
pub struct RegManager {
    current: &'static RegDb,
    user_alpha2: Option<[u8; 2]>,
}

impl RegManager {
    pub const fn new() -> Self {
        Self {
            current: &REG_WORLD,
            user_alpha2: None,
        }
    }
    
    /// Set regulatory domain by country code
    pub fn set_country(&mut self, alpha2: [u8; 2]) -> bool {
        let db = match &alpha2 {
            b"US" => &REG_US,
            b"EU" => &REG_EU,
            b"JP" => &REG_JP,
            b"00" => &REG_WORLD,
            _ => return false,
        };
        self.current = db;
        self.user_alpha2 = Some(alpha2);
        true
    }
    
    /// Get current regulatory domain
    pub fn current(&self) -> &RegDb {
        self.current
    }
    
    /// Check if frequency is allowed
    pub fn is_freq_allowed(&self, freq_mhz: u32) -> bool {
        let freq_khz = freq_mhz * 1000;
        for rule in self.current.rules {
            if freq_khz >= rule.start_freq_khz && freq_khz <= rule.end_freq_khz {
                return !rule.flags.contains(ChannelFlags::DISABLED);
            }
        }
        false
    }
    
    /// Get maximum EIRP for frequency (mBm)
    pub fn max_eirp(&self, freq_mhz: u32) -> Option<u32> {
        let freq_khz = freq_mhz * 1000;
        for rule in self.current.rules {
            if freq_khz >= rule.start_freq_khz && freq_khz <= rule.end_freq_khz {
                return Some(rule.max_eirp_mbm);
            }
        }
        None
    }
    
    /// Check if DFS is required for frequency
    pub fn requires_dfs(&self, freq_mhz: u32) -> bool {
        let freq_khz = freq_mhz * 1000;
        for rule in self.current.rules {
            if freq_khz >= rule.start_freq_khz && freq_khz <= rule.end_freq_khz {
                return rule.flags.contains(ChannelFlags::RADAR);
            }
        }
        false
    }
    
    /// Get DFS CAC time for frequency (ms)
    pub fn dfs_cac_time(&self, freq_mhz: u32) -> u32 {
        let freq_khz = freq_mhz * 1000;
        for rule in self.current.rules {
            if freq_khz >= rule.start_freq_khz && freq_khz <= rule.end_freq_khz {
                return rule.dfs_cac_ms;
            }
        }
        0
    }
    
    /// Get list of allowed 2.4 GHz channels
    pub fn allowed_2g_channels(&self) -> Vec<u8> {
        let mut channels = Vec::new();
        let all_2g = [
            (1, Channel2g::CH1), (2, Channel2g::CH2), (3, Channel2g::CH3),
            (4, Channel2g::CH4), (5, Channel2g::CH5), (6, Channel2g::CH6),
            (7, Channel2g::CH7), (8, Channel2g::CH8), (9, Channel2g::CH9),
            (10, Channel2g::CH10), (11, Channel2g::CH11), (12, Channel2g::CH12),
            (13, Channel2g::CH13), (14, Channel2g::CH14),
        ];
        for (ch, freq) in all_2g {
            if self.is_freq_allowed(freq) {
                channels.push(ch);
            }
        }
        channels
    }
    
    /// Get list of allowed 5 GHz channels
    pub fn allowed_5g_channels(&self) -> Vec<u8> {
        let mut channels = Vec::new();
        let all_5g = [
            (36, Channel5g::CH36), (40, Channel5g::CH40), (44, Channel5g::CH44), (48, Channel5g::CH48),
            (52, Channel5g::CH52), (56, Channel5g::CH56), (60, Channel5g::CH60), (64, Channel5g::CH64),
            (100, Channel5g::CH100), (104, Channel5g::CH104), (108, Channel5g::CH108), (112, Channel5g::CH112),
            (116, Channel5g::CH116), (120, Channel5g::CH120), (124, Channel5g::CH124), (128, Channel5g::CH128),
            (132, Channel5g::CH132), (136, Channel5g::CH136), (140, Channel5g::CH140), (144, Channel5g::CH144),
            (149, Channel5g::CH149), (153, Channel5g::CH153), (157, Channel5g::CH157), (161, Channel5g::CH161),
            (165, Channel5g::CH165),
        ];
        for (ch, freq) in all_5g {
            if self.is_freq_allowed(freq) {
                channels.push(ch);
            }
        }
        channels
    }
}

/// Convert channel number to frequency (MHz)
pub fn channel_to_freq(channel: u8) -> Option<u32> {
    match channel {
        // 2.4 GHz
        1..=13 => Some(2407 + (channel as u32) * 5),
        14 => Some(2484),
        // 5 GHz
        36 | 40 | 44 | 48 => Some(5000 + (channel as u32) * 5),
        52 | 56 | 60 | 64 => Some(5000 + (channel as u32) * 5),
        100..=144 => Some(5000 + (channel as u32) * 5),
        149 | 153 | 157 | 161 | 165 => Some(5000 + (channel as u32) * 5),
        _ => None,
    }
}

/// Convert frequency (MHz) to channel number
pub fn freq_to_channel(freq: u32) -> Option<u8> {
    match freq {
        // 2.4 GHz
        2412..=2472 => Some(((freq - 2407) / 5) as u8),
        2484 => Some(14),
        // 5 GHz
        5180..=5320 => Some(((freq - 5000) / 5) as u8),
        5500..=5720 => Some(((freq - 5000) / 5) as u8),
        5745..=5825 => Some(((freq - 5000) / 5) as u8),
        _ => None,
    }
}
