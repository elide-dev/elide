//! WiFi Monitor Mode Support
//!
//! Enables raw 802.11 frame capture and injection for
//! network analysis, security testing, and debugging.

/// Monitor mode state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum MonitorState {
    #[default]
    Disabled,
    Enabling,
    Active,
    Disabling,
}

/// Monitor mode flags
#[derive(Debug, Clone, Copy, Default)]
pub struct MonitorFlags {
    /// Capture FCS (Frame Check Sequence)
    pub fcsfail: bool,
    /// Capture control frames
    pub control: bool,
    /// Capture other BSS frames
    pub other_bss: bool,
    /// Enable active monitor (TX allowed)
    pub active: bool,
    /// Capture ACKs
    pub cook_frames: bool,
}

impl MonitorFlags {
    pub fn all() -> Self {
        Self {
            fcsfail: true,
            control: true,
            other_bss: true,
            active: true,
            cook_frames: false,
        }
    }
    
    pub fn passive() -> Self {
        Self {
            fcsfail: false,
            control: true,
            other_bss: true,
            active: false,
            cook_frames: false,
        }
    }
    
    pub fn to_nl80211_flags(&self) -> u32 {
        let mut flags = 0u32;
        if self.fcsfail { flags |= 1 << 0; }
        if self.control { flags |= 1 << 1; }
        if self.other_bss { flags |= 1 << 2; }
        if self.cook_frames { flags |= 1 << 3; }
        if self.active { flags |= 1 << 4; }
        flags
    }
}

/// Radiotap header for captured frames
#[derive(Debug, Clone)]
pub struct RadiotapHeader {
    pub version: u8,
    pub length: u16,
    pub present: u32,
    pub data: Vec<u8>,
}

impl RadiotapHeader {
    pub const VERSION: u8 = 0;
    
    // Present flags
    pub const TSFT: u32 = 1 << 0;
    pub const FLAGS: u32 = 1 << 1;
    pub const RATE: u32 = 1 << 2;
    pub const CHANNEL: u32 = 1 << 3;
    pub const FHSS: u32 = 1 << 4;
    pub const DBM_ANTSIGNAL: u32 = 1 << 5;
    pub const DBM_ANTNOISE: u32 = 1 << 6;
    pub const LOCK_QUALITY: u32 = 1 << 7;
    pub const TX_ATTENUATION: u32 = 1 << 8;
    pub const DB_TX_ATTENUATION: u32 = 1 << 9;
    pub const DBM_TX_POWER: u32 = 1 << 10;
    pub const ANTENNA: u32 = 1 << 11;
    pub const DB_ANTSIGNAL: u32 = 1 << 12;
    pub const DB_ANTNOISE: u32 = 1 << 13;
    pub const RX_FLAGS: u32 = 1 << 14;
    pub const MCS: u32 = 1 << 19;
    pub const AMPDU_STATUS: u32 = 1 << 20;
    pub const VHT: u32 = 1 << 21;
    
    // Flags field values
    pub const F_CFP: u8 = 0x01;
    pub const F_SHORTPRE: u8 = 0x02;
    pub const F_WEP: u8 = 0x04;
    pub const F_FRAG: u8 = 0x08;
    pub const F_FCS: u8 = 0x10;
    pub const F_DATAPAD: u8 = 0x20;
    pub const F_BADFCS: u8 = 0x40;
    pub const F_SHORTGI: u8 = 0x80;
    
    /// Create minimal radiotap header
    pub fn minimal() -> Self {
        Self {
            version: Self::VERSION,
            length: 8,
            present: 0,
            data: Vec::new(),
        }
    }
    
    /// Create header with signal strength and channel
    pub fn with_rx_info(signal_dbm: i8, noise_dbm: i8, freq_mhz: u16, flags: u8) -> Self {
        let present = Self::FLAGS | Self::CHANNEL | Self::DBM_ANTSIGNAL | Self::DBM_ANTNOISE;
        let mut data = Vec::new();
        
        // Flags (1 byte)
        data.push(flags);
        // Padding for alignment
        data.push(0);
        // Channel frequency (2 bytes, little endian)
        data.extend_from_slice(&freq_mhz.to_le_bytes());
        // Channel flags (2 bytes) - simplified
        data.extend_from_slice(&[0x00, 0x00]);
        // Signal (1 byte, signed)
        data.push(signal_dbm as u8);
        // Noise (1 byte, signed)
        data.push(noise_dbm as u8);
        
        Self {
            version: Self::VERSION,
            length: 8 + data.len() as u16,
            present,
            data,
        }
    }
    
    /// Build radiotap header bytes
    pub fn build(&self) -> Vec<u8> {
        let mut header = Vec::with_capacity(self.length as usize);
        header.push(self.version);
        header.push(0);  // Padding
        header.extend_from_slice(&self.length.to_le_bytes());
        header.extend_from_slice(&self.present.to_le_bytes());
        header.extend_from_slice(&self.data);
        header
    }
    
    /// Parse radiotap header from bytes
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 8 {
            return None;
        }
        
        let version = data[0];
        if version != Self::VERSION {
            return None;
        }
        
        let length = u16::from_le_bytes([data[2], data[3]]) as usize;
        if data.len() < length {
            return None;
        }
        
        let present = u32::from_le_bytes([data[4], data[5], data[6], data[7]]);
        
        Some((Self {
            version,
            length: length as u16,
            present,
            data: data[8..length].to_vec(),
        }, length))
    }
}

/// Captured frame with metadata
#[derive(Debug, Clone)]
pub struct CapturedFrame {
    /// Timestamp (microseconds since capture start)
    pub timestamp_us: u64,
    /// Signal strength (dBm)
    pub signal_dbm: i8,
    /// Noise floor (dBm)
    pub noise_dbm: i8,
    /// Channel frequency (MHz)
    pub frequency: u16,
    /// Data rate (in 0.5 Mbps units, or MCS index if HT/VHT)
    pub rate: u8,
    /// Frame flags
    pub flags: CaptureFlags,
    /// Raw 802.11 frame (without radiotap)
    pub frame: Vec<u8>,
}

/// Frame capture flags
#[derive(Debug, Clone, Copy, Default)]
pub struct CaptureFlags {
    pub short_preamble: bool,
    pub wep: bool,
    pub fragmented: bool,
    pub fcs_included: bool,
    pub bad_fcs: bool,
    pub short_gi: bool,
    pub ht: bool,
    pub vht: bool,
}

impl CapturedFrame {
    /// Get frame type
    pub fn frame_type(&self) -> FrameType {
        if self.frame.len() < 2 {
            return FrameType::Unknown;
        }
        
        let fc = u16::from_le_bytes([self.frame[0], self.frame[1]]);
        let type_bits = (fc >> 2) & 0x03;
        let subtype_bits = (fc >> 4) & 0x0F;
        
        match type_bits {
            0 => match subtype_bits {
                0 => FrameType::AssocRequest,
                1 => FrameType::AssocResponse,
                2 => FrameType::ReassocRequest,
                3 => FrameType::ReassocResponse,
                4 => FrameType::ProbeRequest,
                5 => FrameType::ProbeResponse,
                8 => FrameType::Beacon,
                10 => FrameType::Disassoc,
                11 => FrameType::Auth,
                12 => FrameType::Deauth,
                13 => FrameType::Action,
                _ => FrameType::Management,
            },
            1 => match subtype_bits {
                8 => FrameType::BlockAckRequest,
                9 => FrameType::BlockAck,
                10 => FrameType::PsPoll,
                11 => FrameType::Rts,
                12 => FrameType::Cts,
                13 => FrameType::Ack,
                _ => FrameType::Control,
            },
            2 => FrameType::Data,
            _ => FrameType::Unknown,
        }
    }
    
    /// Extract BSSID from frame
    pub fn bssid(&self) -> Option<[u8; 6]> {
        if self.frame.len() < 24 {
            return None;
        }
        
        let fc = u16::from_le_bytes([self.frame[0], self.frame[1]]);
        let to_ds = (fc & 0x0100) != 0;
        let from_ds = (fc & 0x0200) != 0;
        
        let offset = match (to_ds, from_ds) {
            (false, false) => 16, // BSSID in Address 3
            (false, true) => 10,  // BSSID in Address 2
            (true, false) => 4,   // BSSID in Address 1
            (true, true) => return None, // WDS, no single BSSID
        };
        
        let mut bssid = [0u8; 6];
        bssid.copy_from_slice(&self.frame[offset..offset + 6]);
        Some(bssid)
    }
    
    /// Get source address
    pub fn src_addr(&self) -> Option<[u8; 6]> {
        if self.frame.len() < 16 {
            return None;
        }
        let mut addr = [0u8; 6];
        addr.copy_from_slice(&self.frame[10..16]);
        Some(addr)
    }
    
    /// Get destination address
    pub fn dst_addr(&self) -> Option<[u8; 6]> {
        if self.frame.len() < 10 {
            return None;
        }
        let mut addr = [0u8; 6];
        addr.copy_from_slice(&self.frame[4..10]);
        Some(addr)
    }
    
    /// Build frame with radiotap header for output
    pub fn with_radiotap(&self) -> Vec<u8> {
        let mut flags = 0u8;
        if self.flags.short_preamble { flags |= RadiotapHeader::F_SHORTPRE; }
        if self.flags.wep { flags |= RadiotapHeader::F_WEP; }
        if self.flags.fragmented { flags |= RadiotapHeader::F_FRAG; }
        if self.flags.fcs_included { flags |= RadiotapHeader::F_FCS; }
        if self.flags.bad_fcs { flags |= RadiotapHeader::F_BADFCS; }
        if self.flags.short_gi { flags |= RadiotapHeader::F_SHORTGI; }
        
        let rt = RadiotapHeader::with_rx_info(
            self.signal_dbm,
            self.noise_dbm,
            self.frequency,
            flags,
        );
        
        let mut output = rt.build();
        output.extend_from_slice(&self.frame);
        output
    }
}

/// Frame type classification
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum FrameType {
    // Management
    AssocRequest,
    AssocResponse,
    ReassocRequest,
    ReassocResponse,
    ProbeRequest,
    ProbeResponse,
    Beacon,
    Disassoc,
    Auth,
    Deauth,
    Action,
    Management,
    // Control
    BlockAckRequest,
    BlockAck,
    PsPoll,
    Rts,
    Cts,
    Ack,
    Control,
    // Data
    Data,
    // Unknown
    Unknown,
}

/// Capture filter
#[derive(Debug, Clone, Default)]
pub struct CaptureFilter {
    /// Filter by BSSID (None = all)
    pub bssid: Option<[u8; 6]>,
    /// Filter by frame types
    pub frame_types: Option<Vec<FrameType>>,
    /// Minimum signal strength
    pub min_signal: Option<i8>,
    /// Channel filter
    pub channel: Option<u8>,
}

impl CaptureFilter {
    pub fn matches(&self, frame: &CapturedFrame) -> bool {
        if let Some(bssid) = &self.bssid {
            if frame.bssid().as_ref() != Some(bssid) {
                return false;
            }
        }
        
        if let Some(types) = &self.frame_types {
            if !types.contains(&frame.frame_type()) {
                return false;
            }
        }
        
        if let Some(min) = self.min_signal {
            if frame.signal_dbm < min {
                return false;
            }
        }
        
        true
    }
}

/// Monitor mode manager
pub struct MonitorManager {
    state: MonitorState,
    flags: MonitorFlags,
    channel: u32,
    capture_start: u64,
    frame_count: u64,
    byte_count: u64,
    filter: CaptureFilter,
    /// Ring buffer for captured frames
    buffer: Vec<CapturedFrame>,
    buffer_size: usize,
    buffer_head: usize,
}

impl MonitorManager {
    pub fn new(buffer_size: usize) -> Self {
        Self {
            state: MonitorState::Disabled,
            flags: MonitorFlags::default(),
            channel: 2412,
            capture_start: 0,
            frame_count: 0,
            byte_count: 0,
            filter: CaptureFilter::default(),
            buffer: Vec::with_capacity(buffer_size),
            buffer_size,
            buffer_head: 0,
        }
    }
    
    /// Enable monitor mode
    pub fn enable(&mut self, flags: MonitorFlags, timestamp: u64) -> MonitorAction {
        self.flags = flags;
        self.capture_start = timestamp;
        self.frame_count = 0;
        self.byte_count = 0;
        self.state = MonitorState::Enabling;
        MonitorAction::EnableMonitor(flags)
    }
    
    /// Disable monitor mode
    pub fn disable(&mut self) -> MonitorAction {
        self.state = MonitorState::Disabling;
        MonitorAction::DisableMonitor
    }
    
    /// Set channel
    pub fn set_channel(&mut self, freq_mhz: u32) -> MonitorAction {
        self.channel = freq_mhz;
        MonitorAction::SetChannel(freq_mhz)
    }
    
    /// Set capture filter
    pub fn set_filter(&mut self, filter: CaptureFilter) {
        self.filter = filter;
    }
    
    /// Report monitor mode enabled
    pub fn on_enabled(&mut self) {
        self.state = MonitorState::Active;
    }
    
    /// Report monitor mode disabled
    pub fn on_disabled(&mut self) {
        self.state = MonitorState::Disabled;
        self.buffer.clear();
        self.buffer_head = 0;
    }
    
    /// Process received frame
    pub fn rx_frame(&mut self, frame: CapturedFrame) -> bool {
        if self.state != MonitorState::Active {
            return false;
        }
        
        if !self.filter.matches(&frame) {
            return false;
        }
        
        self.frame_count += 1;
        self.byte_count += frame.frame.len() as u64;
        
        // Add to ring buffer
        if self.buffer.len() < self.buffer_size {
            self.buffer.push(frame);
        } else {
            self.buffer[self.buffer_head] = frame;
            self.buffer_head = (self.buffer_head + 1) % self.buffer_size;
        }
        
        true
    }
    
    /// Get captured frames
    pub fn get_frames(&self) -> &[CapturedFrame] {
        &self.buffer
    }
    
    /// Clear capture buffer
    pub fn clear_buffer(&mut self) {
        self.buffer.clear();
        self.buffer_head = 0;
    }
    
    pub fn state(&self) -> MonitorState {
        self.state
    }
    
    pub fn is_active(&self) -> bool {
        self.state == MonitorState::Active
    }
    
    pub fn frame_count(&self) -> u64 {
        self.frame_count
    }
    
    pub fn byte_count(&self) -> u64 {
        self.byte_count
    }
    
    pub fn channel(&self) -> u32 {
        self.channel
    }
}

impl Default for MonitorManager {
    fn default() -> Self {
        Self::new(1024)
    }
}

/// Monitor mode action
#[derive(Debug, Clone)]
pub enum MonitorAction {
    EnableMonitor(MonitorFlags),
    DisableMonitor,
    SetChannel(u32),
    InjectFrame(Vec<u8>),
}

/// Frame injection helper
pub struct FrameInjector;

impl FrameInjector {
    /// Build deauth frame for injection
    pub fn deauth(bssid: [u8; 6], target: [u8; 6], reason: u16) -> Vec<u8> {
        let mut frame = Vec::with_capacity(26);
        // Frame Control: Deauth
        frame.extend_from_slice(&[0xC0, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (target)
        frame.extend_from_slice(&target);
        // SA (spoofed as BSSID)
        frame.extend_from_slice(&bssid);
        // BSSID
        frame.extend_from_slice(&bssid);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        // Reason code
        frame.extend_from_slice(&reason.to_le_bytes());
        frame
    }
    
    /// Build probe request for injection
    pub fn probe_request(src: [u8; 6], ssid: Option<&[u8]>) -> Vec<u8> {
        let mut frame = Vec::with_capacity(64);
        // Frame Control: Probe Request
        frame.extend_from_slice(&[0x40, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (broadcast)
        frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);
        // SA
        frame.extend_from_slice(&src);
        // BSSID (broadcast)
        frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        
        // SSID IE
        frame.push(0);  // Element ID
        if let Some(s) = ssid {
            frame.push(s.len() as u8);
            frame.extend_from_slice(s);
        } else {
            frame.push(0);  // Wildcard SSID
        }
        
        // Supported Rates IE
        frame.push(1);  // Element ID
        frame.push(8);  // Length
        frame.extend_from_slice(&[0x82, 0x84, 0x8B, 0x96, 0x0C, 0x12, 0x18, 0x24]);
        
        frame
    }
}
