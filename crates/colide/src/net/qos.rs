//! QoS and WMM (Wi-Fi Multimedia) Support
//!
//! Implements IEEE 802.11e QoS with WMM access categories,
//! EDCA parameters, and traffic classification.

/// Access Category (AC) - traffic priority classes
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
#[repr(u8)]
pub enum AccessCategory {
    /// Background (lowest priority) - bulk data, file transfers
    Background = 1,
    /// Best Effort (default) - web browsing, email
    BestEffort = 0,
    /// Video - streaming video, video conferencing
    Video = 2,
    /// Voice (highest priority) - VoIP, real-time audio
    Voice = 3,
}

impl AccessCategory {
    /// Get AC from 802.1D user priority (0-7)
    pub fn from_user_priority(up: u8) -> Self {
        match up & 0x07 {
            1 | 2 => Self::Background,
            0 | 3 => Self::BestEffort,
            4 | 5 => Self::Video,
            6 | 7 => Self::Voice,
            _ => Self::BestEffort,
        }
    }
    
    /// Get default user priority for this AC
    pub fn default_user_priority(&self) -> u8 {
        match self {
            Self::Background => 1,
            Self::BestEffort => 0,
            Self::Video => 5,
            Self::Voice => 6,
        }
    }
    
    /// Get TID (Traffic Identifier) for this AC
    pub fn tid(&self) -> u8 {
        match self {
            Self::Background => 1,
            Self::BestEffort => 0,
            Self::Video => 5,
            Self::Voice => 6,
        }
    }
    
    /// Get ACI (Access Category Index) for WMM IE
    pub fn aci(&self) -> u8 {
        match self {
            Self::BestEffort => 0,
            Self::Background => 1,
            Self::Video => 2,
            Self::Voice => 3,
        }
    }
}

impl Default for AccessCategory {
    fn default() -> Self {
        Self::BestEffort
    }
}

/// EDCA (Enhanced Distributed Channel Access) parameters
#[derive(Debug, Clone, Copy)]
pub struct EdcaParams {
    /// Arbitration Inter-Frame Space Number
    pub aifsn: u8,
    /// Minimum Contention Window exponent
    pub cw_min: u8,
    /// Maximum Contention Window exponent
    pub cw_max: u8,
    /// TXOP Limit (in 32µs units, 0 = one frame)
    pub txop_limit: u16,
    /// ACM (Admission Control Mandatory)
    pub acm: bool,
}

impl EdcaParams {
    /// Default EDCA parameters per AC (AP perspective)
    pub fn default_ap(ac: AccessCategory) -> Self {
        match ac {
            AccessCategory::Background => Self {
                aifsn: 7,
                cw_min: 4,   // 2^4 - 1 = 15
                cw_max: 10,  // 2^10 - 1 = 1023
                txop_limit: 0,
                acm: false,
            },
            AccessCategory::BestEffort => Self {
                aifsn: 3,
                cw_min: 4,
                cw_max: 10,
                txop_limit: 0,
                acm: false,
            },
            AccessCategory::Video => Self {
                aifsn: 2,
                cw_min: 3,   // 2^3 - 1 = 7
                cw_max: 4,
                txop_limit: 94,  // 3.008ms
                acm: false,
            },
            AccessCategory::Voice => Self {
                aifsn: 2,
                cw_min: 2,   // 2^2 - 1 = 3
                cw_max: 3,
                txop_limit: 47,  // 1.504ms
                acm: false,
            },
        }
    }
    
    /// Default EDCA parameters per AC (STA perspective)
    pub fn default_sta(ac: AccessCategory) -> Self {
        match ac {
            AccessCategory::Background => Self {
                aifsn: 7,
                cw_min: 4,
                cw_max: 10,
                txop_limit: 0,
                acm: false,
            },
            AccessCategory::BestEffort => Self {
                aifsn: 3,
                cw_min: 4,
                cw_max: 6,
                txop_limit: 0,
                acm: false,
            },
            AccessCategory::Video => Self {
                aifsn: 2,
                cw_min: 3,
                cw_max: 4,
                txop_limit: 94,
                acm: false,
            },
            AccessCategory::Voice => Self {
                aifsn: 2,
                cw_min: 2,
                cw_max: 3,
                txop_limit: 47,
                acm: false,
            },
        }
    }
    
    /// Calculate actual CW min value
    pub fn cw_min_value(&self) -> u16 {
        (1u16 << self.cw_min) - 1
    }
    
    /// Calculate actual CW max value
    pub fn cw_max_value(&self) -> u16 {
        (1u16 << self.cw_max) - 1
    }
    
    /// TXOP limit in microseconds
    pub fn txop_us(&self) -> u32 {
        (self.txop_limit as u32) * 32
    }
    
    /// Encode for WMM Parameter Element
    pub fn encode_wmm_ac(&self) -> [u8; 4] {
        let mut bytes = [0u8; 4];
        // ACI/AIFSN byte
        bytes[0] = self.aifsn & 0x0F;
        if self.acm { bytes[0] |= 0x10; }
        // ECW byte (CW min in lower, CW max in upper nibble)
        bytes[1] = (self.cw_min & 0x0F) | ((self.cw_max & 0x0F) << 4);
        // TXOP limit (little endian)
        bytes[2] = self.txop_limit as u8;
        bytes[3] = (self.txop_limit >> 8) as u8;
        bytes
    }
    
    /// Decode from WMM Parameter Element
    pub fn decode_wmm_ac(bytes: &[u8; 4]) -> Self {
        Self {
            aifsn: bytes[0] & 0x0F,
            acm: (bytes[0] & 0x10) != 0,
            cw_min: bytes[1] & 0x0F,
            cw_max: (bytes[1] >> 4) & 0x0F,
            txop_limit: u16::from_le_bytes([bytes[2], bytes[3]]),
        }
    }
}

/// WMM Information Element
#[derive(Debug, Clone)]
pub struct WmmInfoElement {
    /// OUI: 00:50:F2
    pub oui: [u8; 3],
    /// OUI Type: 2 (WMM)
    pub oui_type: u8,
    /// OUI Subtype: 0 (Info) or 1 (Parameter)
    pub oui_subtype: u8,
    /// Version
    pub version: u8,
    /// QoS Info field
    pub qos_info: u8,
}

impl WmmInfoElement {
    pub const OUI: [u8; 3] = [0x00, 0x50, 0xF2];
    pub const OUI_TYPE: u8 = 2;
    pub const SUBTYPE_INFO: u8 = 0;
    pub const SUBTYPE_PARAM: u8 = 1;
    pub const VERSION: u8 = 1;
    
    /// Create WMM Info Element for STA
    pub fn sta_info(uapsd_acs: u8) -> Self {
        Self {
            oui: Self::OUI,
            oui_type: Self::OUI_TYPE,
            oui_subtype: Self::SUBTYPE_INFO,
            version: Self::VERSION,
            qos_info: uapsd_acs & 0x0F,
        }
    }
    
    /// Create WMM Info Element for AP
    pub fn ap_info(param_set_count: u8, uapsd_supported: bool) -> Self {
        let mut qos_info = param_set_count & 0x0F;
        if uapsd_supported { qos_info |= 0x80; }
        Self {
            oui: Self::OUI,
            oui_type: Self::OUI_TYPE,
            oui_subtype: Self::SUBTYPE_INFO,
            version: Self::VERSION,
            qos_info,
        }
    }
    
    /// Encode to bytes
    pub fn encode(&self) -> [u8; 7] {
        [
            self.oui[0], self.oui[1], self.oui[2],
            self.oui_type,
            self.oui_subtype,
            self.version,
            self.qos_info,
        ]
    }
}

/// WMM Parameter Element
#[derive(Debug, Clone)]
pub struct WmmParamElement {
    pub info: WmmInfoElement,
    pub reserved: u8,
    pub ac_be: EdcaParams,
    pub ac_bk: EdcaParams,
    pub ac_vi: EdcaParams,
    pub ac_vo: EdcaParams,
}

impl WmmParamElement {
    /// Create default WMM Parameter Element
    pub fn default_ap() -> Self {
        Self {
            info: WmmInfoElement::ap_info(0, true),
            reserved: 0,
            ac_be: EdcaParams::default_ap(AccessCategory::BestEffort),
            ac_bk: EdcaParams::default_ap(AccessCategory::Background),
            ac_vi: EdcaParams::default_ap(AccessCategory::Video),
            ac_vo: EdcaParams::default_ap(AccessCategory::Voice),
        }
    }
    
    /// Encode to bytes (24 bytes)
    pub fn encode(&self) -> [u8; 24] {
        let mut bytes = [0u8; 24];
        let info = self.info.encode();
        bytes[0..7].copy_from_slice(&info);
        // Change subtype to PARAM
        bytes[4] = WmmInfoElement::SUBTYPE_PARAM;
        bytes[7] = self.reserved;
        bytes[8..12].copy_from_slice(&self.ac_be.encode_wmm_ac());
        bytes[12..16].copy_from_slice(&self.ac_bk.encode_wmm_ac());
        bytes[16..20].copy_from_slice(&self.ac_vi.encode_wmm_ac());
        bytes[20..24].copy_from_slice(&self.ac_vo.encode_wmm_ac());
        bytes
    }
    
    /// Get EDCA params for access category
    pub fn get_ac(&self, ac: AccessCategory) -> &EdcaParams {
        match ac {
            AccessCategory::BestEffort => &self.ac_be,
            AccessCategory::Background => &self.ac_bk,
            AccessCategory::Video => &self.ac_vi,
            AccessCategory::Voice => &self.ac_vo,
        }
    }
}

/// QoS Control field (2 bytes)
#[derive(Debug, Clone, Copy, Default)]
pub struct QosControl {
    /// TID (Traffic Identifier)
    pub tid: u8,
    /// End of Service Period
    pub eosp: bool,
    /// Ack Policy
    pub ack_policy: AckPolicy,
    /// A-MSDU Present
    pub amsdu_present: bool,
    /// TXOP Limit / Queue Size
    pub txop_or_queue: u8,
}

/// Acknowledgment policy
#[derive(Debug, Clone, Copy, Default, PartialEq)]
pub enum AckPolicy {
    #[default]
    Normal = 0,
    NoAck = 1,
    NoExplicitAck = 2,
    BlockAck = 3,
}

impl QosControl {
    pub fn new(tid: u8) -> Self {
        Self {
            tid: tid & 0x0F,
            ..Default::default()
        }
    }
    
    pub fn to_u16(&self) -> u16 {
        let mut val = (self.tid as u16) & 0x0F;
        if self.eosp { val |= 1 << 4; }
        val |= ((self.ack_policy as u16) & 0x03) << 5;
        if self.amsdu_present { val |= 1 << 7; }
        val |= (self.txop_or_queue as u16) << 8;
        val
    }
    
    pub fn from_u16(val: u16) -> Self {
        Self {
            tid: (val & 0x0F) as u8,
            eosp: (val & (1 << 4)) != 0,
            ack_policy: match (val >> 5) & 0x03 {
                0 => AckPolicy::Normal,
                1 => AckPolicy::NoAck,
                2 => AckPolicy::NoExplicitAck,
                3 => AckPolicy::BlockAck,
                _ => AckPolicy::Normal,
            },
            amsdu_present: (val & (1 << 7)) != 0,
            txop_or_queue: (val >> 8) as u8,
        }
    }
}

/// Traffic classifier for automatic QoS tagging
pub struct TrafficClassifier {
    /// Enable DSCP-based classification
    pub use_dscp: bool,
    /// Enable port-based classification
    pub use_ports: bool,
}

impl TrafficClassifier {
    pub fn new() -> Self {
        Self {
            use_dscp: true,
            use_ports: true,
        }
    }
    
    /// Classify IP packet to Access Category
    pub fn classify(&self, ip_header: &[u8]) -> AccessCategory {
        if ip_header.len() < 20 {
            return AccessCategory::BestEffort;
        }
        
        // Check IP version
        let version = ip_header[0] >> 4;
        
        if version == 4 && self.use_dscp {
            // IPv4: DSCP is in TOS byte (bits 2-7)
            let dscp = (ip_header[1] >> 2) & 0x3F;
            return Self::dscp_to_ac(dscp);
        } else if version == 6 && self.use_dscp {
            // IPv6: Traffic Class spans bytes 0-1
            let tc = ((ip_header[0] & 0x0F) << 4) | (ip_header[1] >> 4);
            let dscp = (tc >> 2) & 0x3F;
            return Self::dscp_to_ac(dscp);
        }
        
        // Port-based classification for TCP/UDP
        if self.use_ports && ip_header.len() >= 24 {
            let protocol = if version == 4 { ip_header[9] } else { ip_header[6] };
            let header_len = if version == 4 { 
                ((ip_header[0] & 0x0F) * 4) as usize 
            } else { 
                40 
            };
            
            if (protocol == 6 || protocol == 17) && ip_header.len() >= header_len + 4 {
                let dst_port = u16::from_be_bytes([
                    ip_header[header_len + 2],
                    ip_header[header_len + 3],
                ]);
                return Self::port_to_ac(dst_port);
            }
        }
        
        AccessCategory::BestEffort
    }
    
    /// Map DSCP value to Access Category
    fn dscp_to_ac(dscp: u8) -> AccessCategory {
        match dscp {
            // EF (Expedited Forwarding) - Voice
            46 => AccessCategory::Voice,
            // AF4x (Assured Forwarding Class 4) - Video
            32..=39 => AccessCategory::Video,
            // AF3x, AF2x - Video
            24..=31 | 16..=23 => AccessCategory::Video,
            // CS1, AF1x - Background
            8..=15 => AccessCategory::Background,
            // Default and CS0
            _ => AccessCategory::BestEffort,
        }
    }
    
    /// Map well-known ports to Access Category
    fn port_to_ac(port: u16) -> AccessCategory {
        match port {
            // VoIP (SIP, RTP range)
            5060 | 5061 | 16384..=32767 => AccessCategory::Voice,
            // Video streaming
            554 | 1935 | 8554 => AccessCategory::Video,
            // Bulk transfer (FTP data)
            20 | 21 => AccessCategory::Background,
            // Everything else
            _ => AccessCategory::BestEffort,
        }
    }
}

impl Default for TrafficClassifier {
    fn default() -> Self {
        Self::new()
    }
}

/// Per-AC TX queue
pub struct AcQueue {
    pub ac: AccessCategory,
    pub params: EdcaParams,
    /// Current contention window
    pub cw: u16,
    /// Backoff counter
    pub backoff: u16,
    /// Frames pending transmission
    pub pending: Vec<Vec<u8>>,
    /// Frames transmitted, awaiting ACK
    pub in_flight: Vec<Vec<u8>>,
}

impl AcQueue {
    pub fn new(ac: AccessCategory) -> Self {
        let params = EdcaParams::default_sta(ac);
        Self {
            ac,
            cw: params.cw_min_value(),
            backoff: 0,
            params,
            pending: Vec::new(),
            in_flight: Vec::new(),
        }
    }
    
    /// Reset backoff after successful TX
    pub fn tx_success(&mut self) {
        self.cw = self.params.cw_min_value();
        self.backoff = 0;
    }
    
    /// Increase CW after collision/failure
    pub fn tx_failure(&mut self) {
        let max = self.params.cw_max_value();
        self.cw = ((self.cw + 1) * 2 - 1).min(max);
    }
    
    /// Generate new random backoff
    pub fn new_backoff(&mut self, rand: u16) {
        self.backoff = rand % (self.cw + 1);
    }
}
