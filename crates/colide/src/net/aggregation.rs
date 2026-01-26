//! Frame Aggregation for WiFi (A-MPDU and A-MSDU)
//!
//! Implements IEEE 802.11n/ac/ax frame aggregation for improved throughput:
//! - A-MSDU (Aggregate MAC Service Data Unit) - multiple MSDUs in one MPDU
//! - A-MPDU (Aggregate MAC Protocol Data Unit) - multiple MPDUs in one PPDU
//!
//! Based on IEEE 802.11-2020 aggregation specifications.

/// Maximum A-MSDU sizes
pub mod AmsduSize {
    pub const MAX_3839: usize = 3839;   // HT basic
    pub const MAX_7935: usize = 7935;   // HT extended
    pub const MAX_11454: usize = 11454; // VHT/HE
}

/// Maximum A-MPDU length exponent values
pub mod AmpduExponent {
    pub const HT_MAX_8K: u8 = 0;     // 8KB (2^13)
    pub const HT_MAX_16K: u8 = 1;    // 16KB
    pub const HT_MAX_32K: u8 = 2;    // 32KB
    pub const HT_MAX_64K: u8 = 3;    // 64KB (2^16)
    pub const VHT_MAX_256K: u8 = 4;  // 256KB
    pub const VHT_MAX_512K: u8 = 5;  // 512KB
    pub const VHT_MAX_1M: u8 = 6;    // 1MB (2^20)
    pub const HE_MAX_2M: u8 = 7;     // 2MB
}

/// A-MSDU subframe header (14 bytes)
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct AmsduSubframeHeader {
    pub da: [u8; 6],        // Destination Address
    pub sa: [u8; 6],        // Source Address
    pub length: [u8; 2],    // MSDU length (big endian)
}

impl AmsduSubframeHeader {
    pub fn new(da: [u8; 6], sa: [u8; 6], length: u16) -> Self {
        Self {
            da,
            sa,
            length: length.to_be_bytes(),
        }
    }
    
    pub fn msdu_length(&self) -> u16 {
        u16::from_be_bytes(self.length)
    }
}

/// A-MPDU delimiter (4 bytes)
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct AmpduDelimiter {
    /// Reserved (4 bits) + MPDU length (12 bits)
    pub length_info: [u8; 2],
    /// CRC-8
    pub crc: u8,
    /// Delimiter signature (0x4E)
    pub signature: u8,
}

impl AmpduDelimiter {
    pub const SIGNATURE: u8 = 0x4E;
    
    pub fn new(mpdu_length: u16) -> Self {
        let length_info = (mpdu_length & 0x0FFF).to_le_bytes();
        let crc = Self::calc_crc(&length_info);
        Self {
            length_info,
            crc,
            signature: Self::SIGNATURE,
        }
    }
    
    pub fn mpdu_length(&self) -> u16 {
        u16::from_le_bytes(self.length_info) & 0x0FFF
    }
    
    pub fn is_valid(&self) -> bool {
        self.signature == Self::SIGNATURE && 
        self.crc == Self::calc_crc(&self.length_info)
    }
    
    fn calc_crc(data: &[u8; 2]) -> u8 {
        // CRC-8 with polynomial 0x07
        let mut crc = 0u8;
        for byte in data {
            crc ^= byte;
            for _ in 0..8 {
                if crc & 0x80 != 0 {
                    crc = (crc << 1) ^ 0x07;
                } else {
                    crc <<= 1;
                }
            }
        }
        crc
    }
}

/// Block Ack control field
#[derive(Debug, Clone, Copy)]
pub struct BlockAckControl {
    /// BA policy (0=immediate, 1=delayed)
    pub policy: bool,
    /// Multi-TID
    pub multi_tid: bool,
    /// Compressed bitmap
    pub compressed: bool,
    /// GCR (Group Cast with Retries)
    pub gcr: bool,
    /// TID
    pub tid: u8,
}

impl BlockAckControl {
    pub fn to_u16(&self) -> u16 {
        let mut ctrl = 0u16;
        if self.policy { ctrl |= 1 << 0; }
        if self.multi_tid { ctrl |= 1 << 1; }
        if self.compressed { ctrl |= 1 << 2; }
        if self.gcr { ctrl |= 1 << 3; }
        ctrl |= ((self.tid as u16) & 0x0F) << 12;
        ctrl
    }
    
    pub fn from_u16(val: u16) -> Self {
        Self {
            policy: (val & (1 << 0)) != 0,
            multi_tid: (val & (1 << 1)) != 0,
            compressed: (val & (1 << 2)) != 0,
            gcr: (val & (1 << 3)) != 0,
            tid: ((val >> 12) & 0x0F) as u8,
        }
    }
}

/// Block Ack Request frame
#[derive(Debug, Clone, Copy)]
pub struct BlockAckRequest {
    pub control: BlockAckControl,
    pub starting_seq: u16,
}

impl BlockAckRequest {
    pub fn build_frame(&self, ra: [u8; 6], ta: [u8; 6]) -> [u8; 20] {
        let mut frame = [0u8; 20];
        // Frame Control: Block Ack Request (subtype 8)
        frame[0] = 0x84;
        frame[1] = 0x00;
        // Duration
        frame[2] = 0;
        frame[3] = 0;
        // RA
        frame[4..10].copy_from_slice(&ra);
        // TA
        frame[10..16].copy_from_slice(&ta);
        // BA Control
        let ctrl = self.control.to_u16();
        frame[16] = ctrl as u8;
        frame[17] = (ctrl >> 8) as u8;
        // Starting Sequence Control
        frame[18] = self.starting_seq as u8;
        frame[19] = (self.starting_seq >> 8) as u8;
        frame
    }
}

/// Block Ack frame (compressed bitmap)
#[derive(Debug, Clone)]
pub struct BlockAck {
    pub control: BlockAckControl,
    pub starting_seq: u16,
    pub bitmap: [u8; 8],  // Compressed: 64 MPDUs
}

impl BlockAck {
    pub fn new(tid: u8, starting_seq: u16) -> Self {
        Self {
            control: BlockAckControl {
                policy: false,
                multi_tid: false,
                compressed: true,
                gcr: false,
                tid,
            },
            starting_seq,
            bitmap: [0; 8],
        }
    }
    
    /// Mark MPDU as received
    pub fn ack_mpdu(&mut self, seq: u16) {
        let offset = seq.wrapping_sub(self.starting_seq) as usize;
        if offset < 64 {
            self.bitmap[offset / 8] |= 1 << (offset % 8);
        }
    }
    
    /// Check if MPDU is acknowledged
    pub fn is_acked(&self, seq: u16) -> bool {
        let offset = seq.wrapping_sub(self.starting_seq) as usize;
        if offset < 64 {
            (self.bitmap[offset / 8] & (1 << (offset % 8))) != 0
        } else {
            false
        }
    }
    
    /// Count acknowledged MPDUs
    pub fn ack_count(&self) -> u32 {
        self.bitmap.iter().map(|b| b.count_ones()).sum()
    }
    
    pub fn build_frame(&self, ra: [u8; 6], ta: [u8; 6]) -> [u8; 28] {
        let mut frame = [0u8; 28];
        // Frame Control: Block Ack (subtype 9)
        frame[0] = 0x94;
        frame[1] = 0x00;
        // Duration
        frame[2] = 0;
        frame[3] = 0;
        // RA
        frame[4..10].copy_from_slice(&ra);
        // TA
        frame[10..16].copy_from_slice(&ta);
        // BA Control
        let ctrl = self.control.to_u16();
        frame[16] = ctrl as u8;
        frame[17] = (ctrl >> 8) as u8;
        // Starting Sequence Control
        frame[18] = self.starting_seq as u8;
        frame[19] = (self.starting_seq >> 8) as u8;
        // Bitmap
        frame[20..28].copy_from_slice(&self.bitmap);
        frame
    }
}

/// A-MPDU TX aggregation state
pub struct AmpduTxState {
    /// TID for this aggregation
    pub tid: u8,
    /// Starting sequence number
    pub ssn: u16,
    /// Current sequence number
    pub seq: u16,
    /// Maximum A-MPDU length (bytes)
    pub max_length: usize,
    /// Minimum MPDU start spacing (µs)
    pub mpdu_spacing: u8,
    /// Pending MPDUs for aggregation
    pub pending: Vec<Vec<u8>>,
    /// Awaiting Block Ack
    pub awaiting_ba: bool,
    /// BA bitmap for retransmissions
    pub ba_bitmap: [u8; 8],
}

impl AmpduTxState {
    pub fn new(tid: u8, max_exponent: u8, mpdu_spacing: u8) -> Self {
        let max_length = match max_exponent {
            0 => 8191,
            1 => 16383,
            2 => 32767,
            3 => 65535,
            _ => 65535,
        };
        Self {
            tid,
            ssn: 0,
            seq: 0,
            max_length,
            mpdu_spacing,
            pending: Vec::new(),
            awaiting_ba: false,
            ba_bitmap: [0; 8],
        }
    }
    
    /// Add MPDU to pending aggregation
    pub fn add_mpdu(&mut self, mpdu: Vec<u8>) -> bool {
        let total_len: usize = self.pending.iter().map(|m| m.len() + 4).sum();
        if total_len + mpdu.len() + 4 > self.max_length {
            return false;
        }
        self.pending.push(mpdu);
        true
    }
    
    /// Build aggregated A-MPDU
    pub fn build_ampdu(&mut self) -> Vec<u8> {
        let mut ampdu = Vec::new();
        
        for mpdu in &self.pending {
            // Add delimiter
            let delimiter = AmpduDelimiter::new(mpdu.len() as u16);
            ampdu.extend_from_slice(&delimiter.length_info);
            ampdu.push(delimiter.crc);
            ampdu.push(delimiter.signature);
            
            // Add MPDU
            ampdu.extend_from_slice(mpdu);
            
            // Pad to 4-byte boundary
            while ampdu.len() % 4 != 0 {
                ampdu.push(0);
            }
        }
        
        self.ssn = self.seq;
        self.awaiting_ba = true;
        ampdu
    }
    
    /// Process Block Ack response
    pub fn process_ba(&mut self, ba: &BlockAck) {
        self.ba_bitmap = ba.bitmap;
        self.awaiting_ba = false;
        
        // Remove acknowledged MPDUs, keep unacked for retransmission
        let mut new_pending = Vec::new();
        for (i, mpdu) in self.pending.drain(..).enumerate() {
            if !ba.is_acked(self.ssn.wrapping_add(i as u16)) {
                new_pending.push(mpdu);
            }
        }
        self.pending = new_pending;
    }
    
    /// Get next sequence number
    pub fn next_seq(&mut self) -> u16 {
        let seq = self.seq;
        self.seq = self.seq.wrapping_add(1) & 0x0FFF;
        seq
    }
}

/// A-MPDU RX reorder buffer
pub struct AmpduRxState {
    /// TID
    pub tid: u8,
    /// Expected sequence number
    pub expected_seq: u16,
    /// Reorder buffer (sequence -> MPDU)
    pub buffer: [Option<Vec<u8>>; 64],
    /// Head index
    pub head: usize,
    /// Timeout for reorder buffer (ms)
    pub timeout_ms: u64,
    /// Last receive timestamp
    pub last_rx: u64,
}

impl AmpduRxState {
    pub fn new(tid: u8) -> Self {
        Self {
            tid,
            expected_seq: 0,
            buffer: core::array::from_fn(|_| None),
            head: 0,
            timeout_ms: 100,
            last_rx: 0,
        }
    }
    
    /// Process received MPDU, returns frames to deliver
    pub fn rx_mpdu(&mut self, seq: u16, mpdu: Vec<u8>, timestamp: u64) -> Vec<Vec<u8>> {
        self.last_rx = timestamp;
        let mut delivered = Vec::new();
        
        let offset = seq.wrapping_sub(self.expected_seq) as usize;
        
        if offset < 64 {
            // In window - buffer it
            let idx = (self.head + offset) % 64;
            self.buffer[idx] = Some(mpdu);
            
            // Deliver in-order frames
            while self.buffer[self.head].is_some() {
                if let Some(frame) = self.buffer[self.head].take() {
                    delivered.push(frame);
                }
                self.head = (self.head + 1) % 64;
                self.expected_seq = self.expected_seq.wrapping_add(1) & 0x0FFF;
            }
        }
        // Out of window - drop or handle BAR
        
        delivered
    }
    
    /// Build Block Ack for received frames
    pub fn build_ba(&self) -> BlockAck {
        let mut ba = BlockAck::new(self.tid, self.expected_seq);
        for i in 0..64 {
            let idx = (self.head + i) % 64;
            if self.buffer[idx].is_some() {
                ba.bitmap[i / 8] |= 1 << (i % 8);
            }
        }
        ba
    }
}
