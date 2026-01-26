//! ARP (Address Resolution Protocol) Implementation
//!
//! Implements ARP for resolving IPv4 addresses to MAC addresses
//! on the local network segment.

use core::net::Ipv4Addr;

/// ARP operation codes
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum ArpOp {
    Request = 1,
    Reply = 2,
}

/// ARP packet
#[derive(Debug, Clone)]
pub struct ArpPacket {
    pub hardware_type: u16,
    pub protocol_type: u16,
    pub hardware_len: u8,
    pub protocol_len: u8,
    pub operation: ArpOp,
    pub sender_mac: [u8; 6],
    pub sender_ip: Ipv4Addr,
    pub target_mac: [u8; 6],
    pub target_ip: Ipv4Addr,
}

impl ArpPacket {
    pub const HARDWARE_ETHERNET: u16 = 1;
    pub const PROTOCOL_IPV4: u16 = 0x0800;
    pub const ETHERTYPE: u16 = 0x0806;
    
    pub fn request(sender_mac: [u8; 6], sender_ip: Ipv4Addr, target_ip: Ipv4Addr) -> Self {
        Self {
            hardware_type: Self::HARDWARE_ETHERNET,
            protocol_type: Self::PROTOCOL_IPV4,
            hardware_len: 6,
            protocol_len: 4,
            operation: ArpOp::Request,
            sender_mac,
            sender_ip,
            target_mac: [0; 6],
            target_ip,
        }
    }
    
    pub fn reply(sender_mac: [u8; 6], sender_ip: Ipv4Addr, 
                 target_mac: [u8; 6], target_ip: Ipv4Addr) -> Self {
        Self {
            hardware_type: Self::HARDWARE_ETHERNET,
            protocol_type: Self::PROTOCOL_IPV4,
            hardware_len: 6,
            protocol_len: 4,
            operation: ArpOp::Reply,
            sender_mac,
            sender_ip,
            target_mac,
            target_ip,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(28);
        
        packet.extend_from_slice(&self.hardware_type.to_be_bytes());
        packet.extend_from_slice(&self.protocol_type.to_be_bytes());
        packet.push(self.hardware_len);
        packet.push(self.protocol_len);
        packet.extend_from_slice(&(self.operation as u16).to_be_bytes());
        packet.extend_from_slice(&self.sender_mac);
        packet.extend_from_slice(&self.sender_ip.octets());
        packet.extend_from_slice(&self.target_mac);
        packet.extend_from_slice(&self.target_ip.octets());
        
        packet
    }
    
    pub fn build_ethernet_frame(&self, src_mac: [u8; 6]) -> Vec<u8> {
        let mut frame = Vec::with_capacity(42);
        
        // Ethernet header
        let dst_mac = if self.operation == ArpOp::Request {
            [0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]  // Broadcast
        } else {
            self.target_mac
        };
        
        frame.extend_from_slice(&dst_mac);
        frame.extend_from_slice(&src_mac);
        frame.extend_from_slice(&Self::ETHERTYPE.to_be_bytes());
        
        // ARP payload
        frame.extend_from_slice(&self.build());
        
        // Pad to minimum Ethernet frame size
        while frame.len() < 60 {
            frame.push(0);
        }
        
        frame
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 28 {
            return None;
        }
        
        let hardware_type = u16::from_be_bytes([data[0], data[1]]);
        let protocol_type = u16::from_be_bytes([data[2], data[3]]);
        let hardware_len = data[4];
        let protocol_len = data[5];
        let operation = match u16::from_be_bytes([data[6], data[7]]) {
            1 => ArpOp::Request,
            2 => ArpOp::Reply,
            _ => return None,
        };
        
        let mut sender_mac = [0u8; 6];
        sender_mac.copy_from_slice(&data[8..14]);
        let sender_ip = Ipv4Addr::new(data[14], data[15], data[16], data[17]);
        
        let mut target_mac = [0u8; 6];
        target_mac.copy_from_slice(&data[18..24]);
        let target_ip = Ipv4Addr::new(data[24], data[25], data[26], data[27]);
        
        Some(Self {
            hardware_type,
            protocol_type,
            hardware_len,
            protocol_len,
            operation,
            sender_mac,
            sender_ip,
            target_mac,
            target_ip,
        })
    }
    
    pub fn is_request(&self) -> bool {
        self.operation == ArpOp::Request
    }
    
    pub fn is_reply(&self) -> bool {
        self.operation == ArpOp::Reply
    }
}

/// ARP cache entry
#[derive(Debug, Clone)]
pub struct ArpEntry {
    pub ip: Ipv4Addr,
    pub mac: [u8; 6],
    pub state: ArpEntryState,
    pub created_at: u64,
    pub last_used: u64,
}

/// ARP entry state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ArpEntryState {
    Incomplete,
    Reachable,
    Stale,
    Permanent,
}

impl ArpEntry {
    pub fn new(ip: Ipv4Addr, mac: [u8; 6], timestamp: u64) -> Self {
        Self {
            ip,
            mac,
            state: ArpEntryState::Reachable,
            created_at: timestamp,
            last_used: timestamp,
        }
    }
    
    pub fn incomplete(ip: Ipv4Addr, timestamp: u64) -> Self {
        Self {
            ip,
            mac: [0; 6],
            state: ArpEntryState::Incomplete,
            created_at: timestamp,
            last_used: timestamp,
        }
    }
    
    pub fn permanent(ip: Ipv4Addr, mac: [u8; 6]) -> Self {
        Self {
            ip,
            mac,
            state: ArpEntryState::Permanent,
            created_at: 0,
            last_used: 0,
        }
    }
    
    pub fn is_valid(&self) -> bool {
        matches!(self.state, ArpEntryState::Reachable | ArpEntryState::Stale | ArpEntryState::Permanent)
    }
}

/// Pending ARP request
struct PendingArp {
    ip: Ipv4Addr,
    sent_at: u64,
    retries: u8,
    queued_packets: Vec<Vec<u8>>,
}

/// ARP cache
pub struct ArpCache {
    our_mac: [u8; 6],
    our_ip: Ipv4Addr,
    entries: Vec<ArpEntry>,
    pending: Vec<PendingArp>,
    max_entries: usize,
    reachable_time: u64,
    stale_time: u64,
    request_timeout: u64,
    max_retries: u8,
}

impl ArpCache {
    pub fn new(mac: [u8; 6], ip: Ipv4Addr) -> Self {
        Self {
            our_mac: mac,
            our_ip: ip,
            entries: Vec::new(),
            pending: Vec::new(),
            max_entries: 256,
            reachable_time: 30_000,
            stale_time: 60_000,
            request_timeout: 1_000,
            max_retries: 3,
        }
    }
    
    /// Set our IP address (e.g., after DHCP)
    pub fn set_ip(&mut self, ip: Ipv4Addr) {
        self.our_ip = ip;
    }
    
    /// Look up MAC address for IP
    pub fn lookup(&self, ip: Ipv4Addr) -> Option<[u8; 6]> {
        self.entries.iter()
            .find(|e| e.ip == ip && e.is_valid())
            .map(|e| e.mac)
    }
    
    /// Resolve IP to MAC, sending ARP request if needed
    pub fn resolve(&mut self, ip: Ipv4Addr, timestamp: u64) -> ArpResult {
        // Check cache first
        if let Some(entry) = self.entries.iter_mut().find(|e| e.ip == ip) {
            if entry.is_valid() {
                entry.last_used = timestamp;
                return ArpResult::Resolved(entry.mac);
            }
        }
        
        // Check if already pending
        if self.pending.iter().any(|p| p.ip == ip) {
            return ArpResult::Pending;
        }
        
        // Send ARP request
        let request = ArpPacket::request(self.our_mac, self.our_ip, ip);
        let frame = request.build_ethernet_frame(self.our_mac);
        
        self.pending.push(PendingArp {
            ip,
            sent_at: timestamp,
            retries: 0,
            queued_packets: Vec::new(),
        });
        
        // Add incomplete entry
        self.entries.push(ArpEntry::incomplete(ip, timestamp));
        
        ArpResult::SendRequest(frame)
    }
    
    /// Queue packet for transmission after ARP resolves
    pub fn queue_packet(&mut self, ip: Ipv4Addr, packet: Vec<u8>) {
        if let Some(pending) = self.pending.iter_mut().find(|p| p.ip == ip) {
            if pending.queued_packets.len() < 8 {
                pending.queued_packets.push(packet);
            }
        }
    }
    
    /// Process incoming ARP packet
    pub fn process(&mut self, packet: &ArpPacket, timestamp: u64) -> ArpAction {
        // Learn from any ARP packet we receive
        self.update_entry(packet.sender_ip, packet.sender_mac, timestamp);
        
        match packet.operation {
            ArpOp::Request => {
                // Reply if it's for us
                if packet.target_ip == self.our_ip {
                    let reply = ArpPacket::reply(
                        self.our_mac,
                        self.our_ip,
                        packet.sender_mac,
                        packet.sender_ip,
                    );
                    return ArpAction::SendReply(reply.build_ethernet_frame(self.our_mac));
                }
            }
            ArpOp::Reply => {
                // Check if we have pending request for this
                if let Some(idx) = self.pending.iter().position(|p| p.ip == packet.sender_ip) {
                    let pending = self.pending.remove(idx);
                    
                    // Update entry to reachable
                    if let Some(entry) = self.entries.iter_mut().find(|e| e.ip == packet.sender_ip) {
                        entry.mac = packet.sender_mac;
                        entry.state = ArpEntryState::Reachable;
                        entry.last_used = timestamp;
                    }
                    
                    // Return queued packets
                    if !pending.queued_packets.is_empty() {
                        return ArpAction::SendQueuedPackets {
                            mac: packet.sender_mac,
                            packets: pending.queued_packets,
                        };
                    }
                }
            }
        }
        
        ArpAction::None
    }
    
    fn update_entry(&mut self, ip: Ipv4Addr, mac: [u8; 6], timestamp: u64) {
        if let Some(entry) = self.entries.iter_mut().find(|e| e.ip == ip) {
            if entry.state != ArpEntryState::Permanent {
                entry.mac = mac;
                entry.state = ArpEntryState::Reachable;
                entry.last_used = timestamp;
            }
        } else if self.entries.len() < self.max_entries {
            self.entries.push(ArpEntry::new(ip, mac, timestamp));
        }
    }
    
    /// Check for timeouts and stale entries
    pub fn tick(&mut self, timestamp: u64) -> Vec<ArpAction> {
        let mut actions = Vec::new();
        
        // Check pending requests for timeout
        let mut timed_out = Vec::new();
        for (idx, pending) in self.pending.iter_mut().enumerate() {
            if timestamp - pending.sent_at >= self.request_timeout {
                if pending.retries < self.max_retries {
                    pending.retries += 1;
                    pending.sent_at = timestamp;
                    
                    let request = ArpPacket::request(self.our_mac, self.our_ip, pending.ip);
                    actions.push(ArpAction::SendRequest(request.build_ethernet_frame(self.our_mac)));
                } else {
                    timed_out.push(idx);
                }
            }
        }
        
        // Remove timed out requests
        for idx in timed_out.into_iter().rev() {
            let pending = self.pending.remove(idx);
            // Remove incomplete entry
            self.entries.retain(|e| e.ip != pending.ip || e.state != ArpEntryState::Incomplete);
            actions.push(ArpAction::ResolutionFailed(pending.ip));
        }
        
        // Age entries
        for entry in &mut self.entries {
            if entry.state == ArpEntryState::Permanent {
                continue;
            }
            
            let age = timestamp - entry.last_used;
            
            if age >= self.stale_time {
                entry.state = ArpEntryState::Stale;
            } else if age >= self.reachable_time && entry.state == ArpEntryState::Reachable {
                entry.state = ArpEntryState::Stale;
            }
        }
        
        // Remove very old stale entries
        self.entries.retain(|e| {
            e.state == ArpEntryState::Permanent || 
            timestamp - e.last_used < self.stale_time * 2
        });
        
        actions
    }
    
    /// Add permanent entry (e.g., for gateway)
    pub fn add_permanent(&mut self, ip: Ipv4Addr, mac: [u8; 6]) {
        self.entries.retain(|e| e.ip != ip);
        self.entries.push(ArpEntry::permanent(ip, mac));
    }
    
    /// Send gratuitous ARP (announce our IP)
    pub fn announce(&self) -> Vec<u8> {
        let request = ArpPacket::request(self.our_mac, self.our_ip, self.our_ip);
        request.build_ethernet_frame(self.our_mac)
    }
    
    /// Flush cache (except permanent entries)
    pub fn flush(&mut self) {
        self.entries.retain(|e| e.state == ArpEntryState::Permanent);
        self.pending.clear();
    }
    
    pub fn entries(&self) -> &[ArpEntry] {
        &self.entries
    }
}

/// ARP resolution result
#[derive(Debug)]
pub enum ArpResult {
    Resolved([u8; 6]),
    Pending,
    SendRequest(Vec<u8>),
}

/// ARP action
#[derive(Debug)]
pub enum ArpAction {
    None,
    SendRequest(Vec<u8>),
    SendReply(Vec<u8>),
    SendQueuedPackets { mac: [u8; 6], packets: Vec<Vec<u8>> },
    ResolutionFailed(Ipv4Addr),
}

/// Ethernet frame builder
pub struct EthernetFrame {
    pub dst_mac: [u8; 6],
    pub src_mac: [u8; 6],
    pub ethertype: u16,
    pub payload: Vec<u8>,
}

impl EthernetFrame {
    pub const TYPE_IPV4: u16 = 0x0800;
    pub const TYPE_ARP: u16 = 0x0806;
    pub const TYPE_IPV6: u16 = 0x86DD;
    pub const TYPE_EAPOL: u16 = 0x888E;
    
    pub fn new(dst: [u8; 6], src: [u8; 6], ethertype: u16, payload: Vec<u8>) -> Self {
        Self {
            dst_mac: dst,
            src_mac: src,
            ethertype,
            payload,
        }
    }
    
    pub fn ipv4(dst: [u8; 6], src: [u8; 6], payload: Vec<u8>) -> Self {
        Self::new(dst, src, Self::TYPE_IPV4, payload)
    }
    
    pub fn broadcast(src: [u8; 6], ethertype: u16, payload: Vec<u8>) -> Self {
        Self::new([0xFF; 6], src, ethertype, payload)
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut frame = Vec::with_capacity(14 + self.payload.len());
        
        frame.extend_from_slice(&self.dst_mac);
        frame.extend_from_slice(&self.src_mac);
        frame.extend_from_slice(&self.ethertype.to_be_bytes());
        frame.extend_from_slice(&self.payload);
        
        // Pad to minimum frame size
        while frame.len() < 60 {
            frame.push(0);
        }
        
        frame
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 14 {
            return None;
        }
        
        let mut dst_mac = [0u8; 6];
        dst_mac.copy_from_slice(&data[0..6]);
        
        let mut src_mac = [0u8; 6];
        src_mac.copy_from_slice(&data[6..12]);
        
        let ethertype = u16::from_be_bytes([data[12], data[13]]);
        let payload = data[14..].to_vec();
        
        Some(Self {
            dst_mac,
            src_mac,
            ethertype,
            payload,
        })
    }
    
    pub fn is_broadcast(&self) -> bool {
        self.dst_mac == [0xFF; 6]
    }
    
    pub fn is_multicast(&self) -> bool {
        (self.dst_mac[0] & 0x01) != 0
    }
}
