//! IPv6 Foundation
//!
//! Implements IPv6 packet handling, Neighbor Discovery Protocol (NDP),
//! and Stateless Address Autoconfiguration (SLAAC).

use core::net::Ipv6Addr;

/// IPv6 packet
#[derive(Debug, Clone)]
pub struct Ipv6Packet {
    pub version: u8,
    pub traffic_class: u8,
    pub flow_label: u32,
    pub payload_length: u16,
    pub next_header: u8,
    pub hop_limit: u8,
    pub src_addr: Ipv6Addr,
    pub dst_addr: Ipv6Addr,
    pub payload: Vec<u8>,
}

impl Ipv6Packet {
    pub const PROTO_HOPOPT: u8 = 0;
    pub const PROTO_ICMPV6: u8 = 58;
    pub const PROTO_TCP: u8 = 6;
    pub const PROTO_UDP: u8 = 17;
    pub const PROTO_NONE: u8 = 59;
    
    pub fn new(src: Ipv6Addr, dst: Ipv6Addr, next_header: u8, payload: Vec<u8>) -> Self {
        Self {
            version: 6,
            traffic_class: 0,
            flow_label: 0,
            payload_length: payload.len() as u16,
            next_header,
            hop_limit: 64,
            src_addr: src,
            dst_addr: dst,
            payload,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(40 + self.payload.len());
        
        // Version (4) + Traffic Class (8) + Flow Label (20)
        let first_word = ((self.version as u32) << 28) |
                         ((self.traffic_class as u32) << 20) |
                         self.flow_label;
        packet.extend_from_slice(&first_word.to_be_bytes());
        
        packet.extend_from_slice(&self.payload_length.to_be_bytes());
        packet.push(self.next_header);
        packet.push(self.hop_limit);
        packet.extend_from_slice(&self.src_addr.octets());
        packet.extend_from_slice(&self.dst_addr.octets());
        packet.extend_from_slice(&self.payload);
        
        packet
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 40 {
            return None;
        }
        
        let first_word = u32::from_be_bytes([data[0], data[1], data[2], data[3]]);
        let version = ((first_word >> 28) & 0x0F) as u8;
        
        if version != 6 {
            return None;
        }
        
        let traffic_class = ((first_word >> 20) & 0xFF) as u8;
        let flow_label = first_word & 0xFFFFF;
        let payload_length = u16::from_be_bytes([data[4], data[5]]);
        let next_header = data[6];
        let hop_limit = data[7];
        
        let mut src_octets = [0u8; 16];
        src_octets.copy_from_slice(&data[8..24]);
        let src_addr = Ipv6Addr::from(src_octets);
        
        let mut dst_octets = [0u8; 16];
        dst_octets.copy_from_slice(&data[24..40]);
        let dst_addr = Ipv6Addr::from(dst_octets);
        
        let payload = data[40..].to_vec();
        
        Some(Self {
            version,
            traffic_class,
            flow_label,
            payload_length,
            next_header,
            hop_limit,
            src_addr,
            dst_addr,
            payload,
        })
    }
}

/// ICMPv6 packet types
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum Icmpv6Type {
    // Error messages
    DestUnreachable = 1,
    PacketTooBig = 2,
    TimeExceeded = 3,
    ParameterProblem = 4,
    
    // Informational
    EchoRequest = 128,
    EchoReply = 129,
    
    // NDP
    RouterSolicitation = 133,
    RouterAdvertisement = 134,
    NeighborSolicitation = 135,
    NeighborAdvertisement = 136,
    Redirect = 137,
}

/// ICMPv6 packet
#[derive(Debug, Clone)]
pub struct Icmpv6Packet {
    pub icmp_type: u8,
    pub code: u8,
    pub checksum: u16,
    pub body: Vec<u8>,
}

impl Icmpv6Packet {
    pub fn new(icmp_type: Icmpv6Type, code: u8, body: Vec<u8>) -> Self {
        Self {
            icmp_type: icmp_type as u8,
            code,
            checksum: 0,
            body,
        }
    }
    
    pub fn echo_request(id: u16, seq: u16, data: &[u8]) -> Self {
        let mut body = Vec::with_capacity(4 + data.len());
        body.extend_from_slice(&id.to_be_bytes());
        body.extend_from_slice(&seq.to_be_bytes());
        body.extend_from_slice(data);
        Self::new(Icmpv6Type::EchoRequest, 0, body)
    }
    
    pub fn echo_reply(id: u16, seq: u16, data: &[u8]) -> Self {
        let mut body = Vec::with_capacity(4 + data.len());
        body.extend_from_slice(&id.to_be_bytes());
        body.extend_from_slice(&seq.to_be_bytes());
        body.extend_from_slice(data);
        Self::new(Icmpv6Type::EchoReply, 0, body)
    }
    
    pub fn build(&self, src: Ipv6Addr, dst: Ipv6Addr) -> Vec<u8> {
        let mut packet = Vec::with_capacity(4 + self.body.len());
        
        packet.push(self.icmp_type);
        packet.push(self.code);
        packet.push(0); // Checksum placeholder
        packet.push(0);
        packet.extend_from_slice(&self.body);
        
        // Calculate checksum with pseudo-header
        let checksum = Self::calculate_checksum(&packet, src, dst);
        packet[2] = (checksum >> 8) as u8;
        packet[3] = (checksum & 0xFF) as u8;
        
        packet
    }
    
    fn calculate_checksum(data: &[u8], src: Ipv6Addr, dst: Ipv6Addr) -> u16 {
        let mut sum: u32 = 0;
        
        // Pseudo-header
        for chunk in src.octets().chunks(2) {
            sum += u16::from_be_bytes([chunk[0], chunk[1]]) as u32;
        }
        for chunk in dst.octets().chunks(2) {
            sum += u16::from_be_bytes([chunk[0], chunk[1]]) as u32;
        }
        sum += data.len() as u32;
        sum += Ipv6Packet::PROTO_ICMPV6 as u32;
        
        // ICMPv6 data
        let mut i = 0;
        while i + 1 < data.len() {
            if i != 2 {  // Skip checksum field
                sum += u16::from_be_bytes([data[i], data[i + 1]]) as u32;
            }
            i += 2;
        }
        if i < data.len() {
            sum += (data[i] as u32) << 8;
        }
        
        while sum > 0xFFFF {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        
        !sum as u16
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 4 {
            return None;
        }
        
        Some(Self {
            icmp_type: data[0],
            code: data[1],
            checksum: u16::from_be_bytes([data[2], data[3]]),
            body: data[4..].to_vec(),
        })
    }
    
    pub fn is_ndp(&self) -> bool {
        self.icmp_type >= 133 && self.icmp_type <= 137
    }
}

/// NDP option types
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum NdpOptionType {
    SourceLinkLayerAddr = 1,
    TargetLinkLayerAddr = 2,
    PrefixInfo = 3,
    RedirectedHeader = 4,
    Mtu = 5,
}

/// NDP option
#[derive(Debug, Clone)]
pub enum NdpOption {
    SourceLinkLayerAddr([u8; 6]),
    TargetLinkLayerAddr([u8; 6]),
    PrefixInfo {
        prefix_length: u8,
        on_link: bool,
        autonomous: bool,
        valid_lifetime: u32,
        preferred_lifetime: u32,
        prefix: Ipv6Addr,
    },
    Mtu(u32),
}

impl NdpOption {
    pub fn build(&self) -> Vec<u8> {
        match self {
            NdpOption::SourceLinkLayerAddr(mac) => {
                let mut opt = vec![1, 1];  // Type, Length (in 8-byte units)
                opt.extend_from_slice(mac);
                opt
            }
            NdpOption::TargetLinkLayerAddr(mac) => {
                let mut opt = vec![2, 1];
                opt.extend_from_slice(mac);
                opt
            }
            NdpOption::PrefixInfo { prefix_length, on_link, autonomous, 
                                    valid_lifetime, preferred_lifetime, prefix } => {
                let mut opt = vec![3, 4];  // Type, Length = 4 * 8 = 32 bytes
                opt.push(*prefix_length);
                let flags = (if *on_link { 0x80 } else { 0 }) |
                           (if *autonomous { 0x40 } else { 0 });
                opt.push(flags);
                opt.extend_from_slice(&valid_lifetime.to_be_bytes());
                opt.extend_from_slice(&preferred_lifetime.to_be_bytes());
                opt.extend_from_slice(&[0; 4]);  // Reserved
                opt.extend_from_slice(&prefix.octets());
                opt
            }
            NdpOption::Mtu(mtu) => {
                let mut opt = vec![5, 1, 0, 0];  // Type, Length, Reserved
                opt.extend_from_slice(&mtu.to_be_bytes());
                opt
            }
        }
    }
    
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 2 {
            return None;
        }
        
        let opt_type = data[0];
        let length = (data[1] as usize) * 8;
        
        if data.len() < length || length == 0 {
            return None;
        }
        
        let opt = match opt_type {
            1 if length >= 8 => {
                let mut mac = [0u8; 6];
                mac.copy_from_slice(&data[2..8]);
                NdpOption::SourceLinkLayerAddr(mac)
            }
            2 if length >= 8 => {
                let mut mac = [0u8; 6];
                mac.copy_from_slice(&data[2..8]);
                NdpOption::TargetLinkLayerAddr(mac)
            }
            3 if length >= 32 => {
                let prefix_length = data[2];
                let on_link = (data[3] & 0x80) != 0;
                let autonomous = (data[3] & 0x40) != 0;
                let valid_lifetime = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
                let preferred_lifetime = u32::from_be_bytes([data[8], data[9], data[10], data[11]]);
                let mut prefix_octets = [0u8; 16];
                prefix_octets.copy_from_slice(&data[16..32]);
                NdpOption::PrefixInfo {
                    prefix_length,
                    on_link,
                    autonomous,
                    valid_lifetime,
                    preferred_lifetime,
                    prefix: Ipv6Addr::from(prefix_octets),
                }
            }
            5 if length >= 8 => {
                let mtu = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
                NdpOption::Mtu(mtu)
            }
            _ => return None,
        };
        
        Some((opt, length))
    }
}

/// Router Solicitation message
#[derive(Debug, Clone)]
pub struct RouterSolicitation {
    pub options: Vec<NdpOption>,
}

impl RouterSolicitation {
    pub fn new(src_mac: [u8; 6]) -> Self {
        Self {
            options: vec![NdpOption::SourceLinkLayerAddr(src_mac)],
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut body = vec![0; 4];  // Reserved
        for opt in &self.options {
            body.extend_from_slice(&opt.build());
        }
        body
    }
}

/// Router Advertisement message
#[derive(Debug, Clone)]
pub struct RouterAdvertisement {
    pub cur_hop_limit: u8,
    pub managed: bool,
    pub other: bool,
    pub router_lifetime: u16,
    pub reachable_time: u32,
    pub retrans_timer: u32,
    pub options: Vec<NdpOption>,
}

impl RouterAdvertisement {
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 12 {
            return None;
        }
        
        let cur_hop_limit = data[0];
        let managed = (data[1] & 0x80) != 0;
        let other = (data[1] & 0x40) != 0;
        let router_lifetime = u16::from_be_bytes([data[2], data[3]]);
        let reachable_time = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
        let retrans_timer = u32::from_be_bytes([data[8], data[9], data[10], data[11]]);
        
        let mut options = Vec::new();
        let mut offset = 12;
        while offset < data.len() {
            if let Some((opt, len)) = NdpOption::parse(&data[offset..]) {
                options.push(opt);
                offset += len;
            } else {
                break;
            }
        }
        
        Some(Self {
            cur_hop_limit,
            managed,
            other,
            router_lifetime,
            reachable_time,
            retrans_timer,
            options,
        })
    }
}

/// Neighbor Solicitation message
#[derive(Debug, Clone)]
pub struct NeighborSolicitation {
    pub target: Ipv6Addr,
    pub options: Vec<NdpOption>,
}

impl NeighborSolicitation {
    pub fn new(target: Ipv6Addr, src_mac: [u8; 6]) -> Self {
        Self {
            target,
            options: vec![NdpOption::SourceLinkLayerAddr(src_mac)],
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut body = vec![0; 4];  // Reserved
        body.extend_from_slice(&self.target.octets());
        for opt in &self.options {
            body.extend_from_slice(&opt.build());
        }
        body
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 20 {
            return None;
        }
        
        let mut target_octets = [0u8; 16];
        target_octets.copy_from_slice(&data[4..20]);
        
        let mut options = Vec::new();
        let mut offset = 20;
        while offset < data.len() {
            if let Some((opt, len)) = NdpOption::parse(&data[offset..]) {
                options.push(opt);
                offset += len;
            } else {
                break;
            }
        }
        
        Some(Self {
            target: Ipv6Addr::from(target_octets),
            options,
        })
    }
}

/// Neighbor Advertisement message
#[derive(Debug, Clone)]
pub struct NeighborAdvertisement {
    pub router: bool,
    pub solicited: bool,
    pub override_flag: bool,
    pub target: Ipv6Addr,
    pub options: Vec<NdpOption>,
}

impl NeighborAdvertisement {
    pub fn new(target: Ipv6Addr, target_mac: [u8; 6], solicited: bool) -> Self {
        Self {
            router: false,
            solicited,
            override_flag: true,
            target,
            options: vec![NdpOption::TargetLinkLayerAddr(target_mac)],
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let flags = (if self.router { 0x80 } else { 0 }) |
                    (if self.solicited { 0x40 } else { 0 }) |
                    (if self.override_flag { 0x20 } else { 0 });
        
        let mut body = vec![flags, 0, 0, 0];  // Flags + Reserved
        body.extend_from_slice(&self.target.octets());
        for opt in &self.options {
            body.extend_from_slice(&opt.build());
        }
        body
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 20 {
            return None;
        }
        
        let router = (data[0] & 0x80) != 0;
        let solicited = (data[0] & 0x40) != 0;
        let override_flag = (data[0] & 0x20) != 0;
        
        let mut target_octets = [0u8; 16];
        target_octets.copy_from_slice(&data[4..20]);
        
        let mut options = Vec::new();
        let mut offset = 20;
        while offset < data.len() {
            if let Some((opt, len)) = NdpOption::parse(&data[offset..]) {
                options.push(opt);
                offset += len;
            } else {
                break;
            }
        }
        
        Some(Self {
            router,
            solicited,
            override_flag,
            target: Ipv6Addr::from(target_octets),
            options,
        })
    }
}

/// Neighbor cache entry
#[derive(Debug, Clone)]
pub struct NeighborEntry {
    pub addr: Ipv6Addr,
    pub mac: [u8; 6],
    pub state: NeighborState,
    pub is_router: bool,
    pub created_at: u64,
    pub last_used: u64,
}

/// Neighbor cache state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum NeighborState {
    Incomplete,
    Reachable,
    Stale,
    Delay,
    Probe,
}

/// IPv6 address configuration
#[derive(Debug, Clone)]
pub struct Ipv6AddrConfig {
    pub addr: Ipv6Addr,
    pub prefix_len: u8,
    pub state: AddrState,
    pub valid_until: u64,
    pub preferred_until: u64,
}

/// Address state (DAD)
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AddrState {
    Tentative,
    Preferred,
    Deprecated,
}

/// SLAAC manager
pub struct SlaacManager {
    mac: [u8; 6],
    link_local: Option<Ipv6Addr>,
    addresses: Vec<Ipv6AddrConfig>,
    neighbors: Vec<NeighborEntry>,
    default_router: Option<Ipv6Addr>,
    hop_limit: u8,
    mtu: u32,
    dad_transmits: u8,
    pending_dad: Vec<(Ipv6Addr, u8, u64)>,
}

impl SlaacManager {
    pub fn new(mac: [u8; 6]) -> Self {
        Self {
            mac,
            link_local: None,
            addresses: Vec::new(),
            neighbors: Vec::new(),
            default_router: None,
            hop_limit: 64,
            mtu: 1500,
            dad_transmits: 1,
            pending_dad: Vec::new(),
        }
    }
    
    /// Start SLAAC by generating link-local address
    pub fn start(&mut self, timestamp: u64) -> SlaacAction {
        let link_local = self.generate_link_local();
        self.pending_dad.push((link_local, 0, timestamp));
        
        // Start DAD for link-local
        SlaacAction::SendDad(link_local)
    }
    
    fn generate_link_local(&self) -> Ipv6Addr {
        // EUI-64 from MAC
        let mut octets = [0u8; 16];
        octets[0] = 0xFE;
        octets[1] = 0x80;
        // octets[2..8] = 0
        octets[8] = self.mac[0] ^ 0x02;  // Flip U/L bit
        octets[9] = self.mac[1];
        octets[10] = self.mac[2];
        octets[11] = 0xFF;
        octets[12] = 0xFE;
        octets[13] = self.mac[3];
        octets[14] = self.mac[4];
        octets[15] = self.mac[5];
        
        Ipv6Addr::from(octets)
    }
    
    fn generate_global(&self, prefix: Ipv6Addr, prefix_len: u8) -> Ipv6Addr {
        let mut octets = prefix.octets();
        
        // Add EUI-64 interface identifier
        octets[8] = self.mac[0] ^ 0x02;
        octets[9] = self.mac[1];
        octets[10] = self.mac[2];
        octets[11] = 0xFF;
        octets[12] = 0xFE;
        octets[13] = self.mac[3];
        octets[14] = self.mac[4];
        octets[15] = self.mac[5];
        
        Ipv6Addr::from(octets)
    }
    
    /// Process incoming NDP message
    pub fn process_ndp(&mut self, icmp: &Icmpv6Packet, src: Ipv6Addr, timestamp: u64) -> Vec<SlaacAction> {
        let mut actions = Vec::new();
        
        match icmp.icmp_type {
            133 => {
                // Router Solicitation - we're a host, ignore
            }
            134 => {
                // Router Advertisement
                if let Some(ra) = RouterAdvertisement::parse(&icmp.body) {
                    actions.extend(self.process_ra(&ra, src, timestamp));
                }
            }
            135 => {
                // Neighbor Solicitation
                if let Some(ns) = NeighborSolicitation::parse(&icmp.body) {
                    actions.extend(self.process_ns(&ns, src, timestamp));
                }
            }
            136 => {
                // Neighbor Advertisement
                if let Some(na) = NeighborAdvertisement::parse(&icmp.body) {
                    self.process_na(&na, src, timestamp);
                }
            }
            _ => {}
        }
        
        actions
    }
    
    fn process_ra(&mut self, ra: &RouterAdvertisement, src: Ipv6Addr, timestamp: u64) -> Vec<SlaacAction> {
        let mut actions = Vec::new();
        
        // Update default router
        if ra.router_lifetime > 0 {
            self.default_router = Some(src);
            self.hop_limit = ra.cur_hop_limit;
        }
        
        // Process options
        for opt in &ra.options {
            match opt {
                NdpOption::PrefixInfo { prefix_length, autonomous, 
                                        valid_lifetime, preferred_lifetime, prefix, .. } => {
                    if *autonomous && *valid_lifetime > 0 {
                        let global = self.generate_global(*prefix, *prefix_length);
                        
                        // Check if we already have this address
                        if !self.addresses.iter().any(|a| a.addr == global) {
                            // Start DAD
                            self.pending_dad.push((global, 0, timestamp));
                            actions.push(SlaacAction::SendDad(global));
                            
                            // Add tentative address
                            self.addresses.push(Ipv6AddrConfig {
                                addr: global,
                                prefix_len: *prefix_length,
                                state: AddrState::Tentative,
                                valid_until: timestamp + (*valid_lifetime as u64 * 1000),
                                preferred_until: timestamp + (*preferred_lifetime as u64 * 1000),
                            });
                        }
                    }
                }
                NdpOption::Mtu(mtu) => {
                    self.mtu = *mtu;
                }
                NdpOption::SourceLinkLayerAddr(mac) => {
                    self.update_neighbor(src, *mac, true, timestamp);
                }
                _ => {}
            }
        }
        
        actions
    }
    
    fn process_ns(&mut self, ns: &NeighborSolicitation, src: Ipv6Addr, timestamp: u64) -> Vec<SlaacAction> {
        let mut actions = Vec::new();
        
        // Check if solicitation is for one of our addresses
        let is_our_addr = self.link_local == Some(ns.target) ||
                          self.addresses.iter().any(|a| a.addr == ns.target && a.state == AddrState::Preferred);
        
        if is_our_addr {
            // Send Neighbor Advertisement
            actions.push(SlaacAction::SendNa(ns.target, src));
            
            // Learn source MAC
            for opt in &ns.options {
                if let NdpOption::SourceLinkLayerAddr(mac) = opt {
                    self.update_neighbor(src, *mac, false, timestamp);
                }
            }
        }
        
        // Check for DAD conflict
        if src == Ipv6Addr::UNSPECIFIED {
            if let Some(idx) = self.pending_dad.iter().position(|(a, _, _)| *a == ns.target) {
                // DAD failed - address conflict
                self.pending_dad.remove(idx);
                self.addresses.retain(|a| a.addr != ns.target);
                actions.push(SlaacAction::DadFailed(ns.target));
            }
        }
        
        actions
    }
    
    fn process_na(&mut self, na: &NeighborAdvertisement, _src: Ipv6Addr, timestamp: u64) {
        // Check for DAD failure
        if let Some(idx) = self.pending_dad.iter().position(|(a, _, _)| *a == na.target) {
            // DAD failed
            self.pending_dad.remove(idx);
            self.addresses.retain(|a| a.addr != na.target);
        }
        
        // Update neighbor cache
        for opt in &na.options {
            if let NdpOption::TargetLinkLayerAddr(mac) = opt {
                self.update_neighbor(na.target, *mac, na.router, timestamp);
            }
        }
    }
    
    fn update_neighbor(&mut self, addr: Ipv6Addr, mac: [u8; 6], is_router: bool, timestamp: u64) {
        if let Some(entry) = self.neighbors.iter_mut().find(|n| n.addr == addr) {
            entry.mac = mac;
            entry.state = NeighborState::Reachable;
            entry.is_router = is_router;
            entry.last_used = timestamp;
        } else {
            self.neighbors.push(NeighborEntry {
                addr,
                mac,
                state: NeighborState::Reachable,
                is_router,
                created_at: timestamp,
                last_used: timestamp,
            });
        }
    }
    
    /// Periodic tick for DAD and timeouts
    pub fn tick(&mut self, timestamp: u64) -> Vec<SlaacAction> {
        let mut actions = Vec::new();
        
        // Process pending DAD
        let mut completed = Vec::new();
        for (idx, (addr, count, sent_at)) in self.pending_dad.iter_mut().enumerate() {
            if timestamp - *sent_at >= 1000 {  // 1 second DAD timeout
                if *count >= self.dad_transmits {
                    completed.push((idx, *addr));
                } else {
                    *count += 1;
                    *sent_at = timestamp;
                    actions.push(SlaacAction::SendDad(*addr));
                }
            }
        }
        
        // Complete DAD
        for (idx, addr) in completed.into_iter().rev() {
            self.pending_dad.remove(idx);
            
            if self.link_local.is_none() && addr.segments()[0] == 0xFE80 {
                self.link_local = Some(addr);
                actions.push(SlaacAction::LinkLocalReady(addr));
                actions.push(SlaacAction::SendRs);
            } else if let Some(a) = self.addresses.iter_mut().find(|a| a.addr == addr) {
                a.state = AddrState::Preferred;
                actions.push(SlaacAction::GlobalReady(addr));
            }
        }
        
        // Age addresses
        for addr in &mut self.addresses {
            if addr.state == AddrState::Preferred && timestamp >= addr.preferred_until {
                addr.state = AddrState::Deprecated;
            }
        }
        self.addresses.retain(|a| timestamp < a.valid_until);
        
        // Age neighbors
        for neighbor in &mut self.neighbors {
            if timestamp - neighbor.last_used > 30_000 {
                neighbor.state = NeighborState::Stale;
            }
        }
        
        actions
    }
    
    /// Build Neighbor Solicitation for DAD
    pub fn build_dad_ns(&self, target: Ipv6Addr) -> (Ipv6Packet, Vec<u8>) {
        let ns = NeighborSolicitation {
            target,
            options: Vec::new(),  // No source option for DAD
        };
        
        let icmp = Icmpv6Packet::new(Icmpv6Type::NeighborSolicitation, 0, ns.build());
        let dst = solicited_node_multicast(target);
        let src = Ipv6Addr::UNSPECIFIED;
        
        let payload = icmp.build(src, dst);
        let ip = Ipv6Packet::new(src, dst, Ipv6Packet::PROTO_ICMPV6, payload.clone());
        
        (ip, payload)
    }
    
    /// Build Router Solicitation
    pub fn build_rs(&self) -> (Ipv6Packet, Vec<u8>) {
        let src = self.link_local.unwrap_or(Ipv6Addr::UNSPECIFIED);
        let rs = RouterSolicitation::new(self.mac);
        
        let icmp = Icmpv6Packet::new(Icmpv6Type::RouterSolicitation, 0, rs.build());
        let dst = Ipv6Addr::new(0xFF02, 0, 0, 0, 0, 0, 0, 2);  // All routers
        
        let payload = icmp.build(src, dst);
        let ip = Ipv6Packet::new(src, dst, Ipv6Packet::PROTO_ICMPV6, payload.clone());
        
        (ip, payload)
    }
    
    /// Build Neighbor Advertisement
    pub fn build_na(&self, target: Ipv6Addr, dst: Ipv6Addr) -> (Ipv6Packet, Vec<u8>) {
        let src = self.link_local.unwrap_or(target);
        let na = NeighborAdvertisement::new(target, self.mac, true);
        
        let icmp = Icmpv6Packet::new(Icmpv6Type::NeighborAdvertisement, 0, na.build());
        let payload = icmp.build(src, dst);
        let ip = Ipv6Packet::new(src, dst, Ipv6Packet::PROTO_ICMPV6, payload.clone());
        
        (ip, payload)
    }
    
    /// Lookup neighbor MAC
    pub fn lookup_neighbor(&self, addr: Ipv6Addr) -> Option<[u8; 6]> {
        self.neighbors.iter()
            .find(|n| n.addr == addr && n.state == NeighborState::Reachable)
            .map(|n| n.mac)
    }
    
    pub fn link_local(&self) -> Option<Ipv6Addr> {
        self.link_local
    }
    
    pub fn global_addresses(&self) -> &[Ipv6AddrConfig] {
        &self.addresses
    }
    
    pub fn default_router(&self) -> Option<Ipv6Addr> {
        self.default_router
    }
}

/// Compute solicited-node multicast address
pub fn solicited_node_multicast(addr: Ipv6Addr) -> Ipv6Addr {
    let octets = addr.octets();
    Ipv6Addr::new(
        0xFF02, 0, 0, 0, 0, 1,
        0xFF00 | (octets[13] as u16),
        ((octets[14] as u16) << 8) | (octets[15] as u16),
    )
}

/// SLAAC action
#[derive(Debug)]
pub enum SlaacAction {
    SendDad(Ipv6Addr),
    SendRs,
    SendNa(Ipv6Addr, Ipv6Addr),
    LinkLocalReady(Ipv6Addr),
    GlobalReady(Ipv6Addr),
    DadFailed(Ipv6Addr),
}
