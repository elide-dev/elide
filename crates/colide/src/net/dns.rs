//! DNS Resolver Implementation
//!
//! Implements a minimal DNS resolver for hostname lookup,
//! supporting A, AAAA, CNAME, and PTR records.

use core::net::{Ipv4Addr, Ipv6Addr};

/// DNS query type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum DnsType {
    A = 1,
    Ns = 2,
    Cname = 5,
    Soa = 6,
    Ptr = 12,
    Mx = 15,
    Txt = 16,
    Aaaa = 28,
    Srv = 33,
    Any = 255,
}

/// DNS query class
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum DnsClass {
    In = 1,
    Any = 255,
}

/// DNS response code
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum DnsRcode {
    NoError = 0,
    FormatError = 1,
    ServerFailure = 2,
    NameError = 3,
    NotImplemented = 4,
    Refused = 5,
}

/// DNS header flags
#[derive(Debug, Clone, Copy, Default)]
pub struct DnsFlags {
    pub qr: bool,
    pub opcode: u8,
    pub aa: bool,
    pub tc: bool,
    pub rd: bool,
    pub ra: bool,
    pub rcode: u8,
}

impl DnsFlags {
    pub fn query() -> Self {
        Self {
            qr: false,
            opcode: 0,
            aa: false,
            tc: false,
            rd: true,
            ra: false,
            rcode: 0,
        }
    }
    
    pub fn from_u16(value: u16) -> Self {
        Self {
            qr: (value & 0x8000) != 0,
            opcode: ((value >> 11) & 0x0F) as u8,
            aa: (value & 0x0400) != 0,
            tc: (value & 0x0200) != 0,
            rd: (value & 0x0100) != 0,
            ra: (value & 0x0080) != 0,
            rcode: (value & 0x000F) as u8,
        }
    }
    
    pub fn to_u16(&self) -> u16 {
        let mut flags = 0u16;
        if self.qr { flags |= 0x8000; }
        flags |= ((self.opcode as u16) & 0x0F) << 11;
        if self.aa { flags |= 0x0400; }
        if self.tc { flags |= 0x0200; }
        if self.rd { flags |= 0x0100; }
        if self.ra { flags |= 0x0080; }
        flags |= (self.rcode as u16) & 0x000F;
        flags
    }
}

/// DNS question
#[derive(Debug, Clone)]
pub struct DnsQuestion {
    pub name: String,
    pub qtype: DnsType,
    pub qclass: DnsClass,
}

/// DNS resource record
#[derive(Debug, Clone)]
pub struct DnsRecord {
    pub name: String,
    pub rtype: DnsType,
    pub rclass: DnsClass,
    pub ttl: u32,
    pub rdata: DnsRdata,
}

/// DNS record data
#[derive(Debug, Clone)]
pub enum DnsRdata {
    A(Ipv4Addr),
    Aaaa(Ipv6Addr),
    Cname(String),
    Ptr(String),
    Mx { preference: u16, exchange: String },
    Txt(String),
    Srv { priority: u16, weight: u16, port: u16, target: String },
    Unknown(Vec<u8>),
}

/// DNS message
#[derive(Debug, Clone)]
pub struct DnsMessage {
    pub id: u16,
    pub flags: DnsFlags,
    pub questions: Vec<DnsQuestion>,
    pub answers: Vec<DnsRecord>,
    pub authorities: Vec<DnsRecord>,
    pub additionals: Vec<DnsRecord>,
}

impl DnsMessage {
    pub fn new_query(id: u16, name: &str, qtype: DnsType) -> Self {
        Self {
            id,
            flags: DnsFlags::query(),
            questions: vec![DnsQuestion {
                name: name.to_string(),
                qtype,
                qclass: DnsClass::In,
            }],
            answers: Vec::new(),
            authorities: Vec::new(),
            additionals: Vec::new(),
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(512);
        
        // Header
        packet.extend_from_slice(&self.id.to_be_bytes());
        packet.extend_from_slice(&self.flags.to_u16().to_be_bytes());
        packet.extend_from_slice(&(self.questions.len() as u16).to_be_bytes());
        packet.extend_from_slice(&(self.answers.len() as u16).to_be_bytes());
        packet.extend_from_slice(&(self.authorities.len() as u16).to_be_bytes());
        packet.extend_from_slice(&(self.additionals.len() as u16).to_be_bytes());
        
        // Questions
        for q in &self.questions {
            Self::encode_name(&mut packet, &q.name);
            packet.extend_from_slice(&(q.qtype as u16).to_be_bytes());
            packet.extend_from_slice(&(q.qclass as u16).to_be_bytes());
        }
        
        packet
    }
    
    fn encode_name(packet: &mut Vec<u8>, name: &str) {
        for label in name.split('.') {
            let len = label.len().min(63);
            packet.push(len as u8);
            packet.extend_from_slice(&label.as_bytes()[..len]);
        }
        packet.push(0);
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 12 {
            return None;
        }
        
        let id = u16::from_be_bytes([data[0], data[1]]);
        let flags = DnsFlags::from_u16(u16::from_be_bytes([data[2], data[3]]));
        let qdcount = u16::from_be_bytes([data[4], data[5]]) as usize;
        let ancount = u16::from_be_bytes([data[6], data[7]]) as usize;
        let nscount = u16::from_be_bytes([data[8], data[9]]) as usize;
        let arcount = u16::from_be_bytes([data[10], data[11]]) as usize;
        
        let mut offset = 12;
        
        // Parse questions
        let mut questions = Vec::with_capacity(qdcount);
        for _ in 0..qdcount {
            let (name, new_offset) = Self::parse_name(data, offset)?;
            offset = new_offset;
            
            if offset + 4 > data.len() {
                return None;
            }
            
            let qtype = u16::from_be_bytes([data[offset], data[offset + 1]]);
            let qclass = u16::from_be_bytes([data[offset + 2], data[offset + 3]]);
            offset += 4;
            
            questions.push(DnsQuestion {
                name,
                qtype: Self::u16_to_type(qtype),
                qclass: if qclass == 1 { DnsClass::In } else { DnsClass::Any },
            });
        }
        
        // Parse answers
        let mut answers = Vec::with_capacity(ancount);
        for _ in 0..ancount {
            let (record, new_offset) = Self::parse_record(data, offset)?;
            offset = new_offset;
            answers.push(record);
        }
        
        // Parse authorities
        let mut authorities = Vec::with_capacity(nscount);
        for _ in 0..nscount {
            let (record, new_offset) = Self::parse_record(data, offset)?;
            offset = new_offset;
            authorities.push(record);
        }
        
        // Parse additionals
        let mut additionals = Vec::with_capacity(arcount);
        for _ in 0..arcount {
            let (record, new_offset) = Self::parse_record(data, offset)?;
            offset = new_offset;
            additionals.push(record);
        }
        
        Some(Self {
            id,
            flags,
            questions,
            answers,
            authorities,
            additionals,
        })
    }
    
    fn parse_name(data: &[u8], mut offset: usize) -> Option<(String, usize)> {
        let mut name = String::new();
        let mut jumped = false;
        let mut final_offset = offset;
        
        loop {
            if offset >= data.len() {
                return None;
            }
            
            let len = data[offset];
            
            if len == 0 {
                if !jumped {
                    final_offset = offset + 1;
                }
                break;
            }
            
            // Compression pointer
            if (len & 0xC0) == 0xC0 {
                if offset + 1 >= data.len() {
                    return None;
                }
                
                if !jumped {
                    final_offset = offset + 2;
                }
                
                let pointer = (((len & 0x3F) as usize) << 8) | (data[offset + 1] as usize);
                offset = pointer;
                jumped = true;
                continue;
            }
            
            let label_len = len as usize;
            offset += 1;
            
            if offset + label_len > data.len() {
                return None;
            }
            
            if !name.is_empty() {
                name.push('.');
            }
            
            name.push_str(&String::from_utf8_lossy(&data[offset..offset + label_len]));
            offset += label_len;
        }
        
        Some((name, final_offset))
    }
    
    fn parse_record(data: &[u8], offset: usize) -> Option<(DnsRecord, usize)> {
        let (name, mut offset) = Self::parse_name(data, offset)?;
        
        if offset + 10 > data.len() {
            return None;
        }
        
        let rtype = u16::from_be_bytes([data[offset], data[offset + 1]]);
        let rclass = u16::from_be_bytes([data[offset + 2], data[offset + 3]]);
        let ttl = u32::from_be_bytes([data[offset + 4], data[offset + 5], data[offset + 6], data[offset + 7]]);
        let rdlen = u16::from_be_bytes([data[offset + 8], data[offset + 9]]) as usize;
        offset += 10;
        
        if offset + rdlen > data.len() {
            return None;
        }
        
        let rdata = Self::parse_rdata(data, offset, rtype, rdlen)?;
        
        Some((DnsRecord {
            name,
            rtype: Self::u16_to_type(rtype),
            rclass: if rclass == 1 { DnsClass::In } else { DnsClass::Any },
            ttl,
            rdata,
        }, offset + rdlen))
    }
    
    fn parse_rdata(data: &[u8], offset: usize, rtype: u16, rdlen: usize) -> Option<DnsRdata> {
        match rtype {
            1 if rdlen == 4 => {
                Some(DnsRdata::A(Ipv4Addr::new(
                    data[offset], data[offset + 1], data[offset + 2], data[offset + 3]
                )))
            }
            28 if rdlen == 16 => {
                let mut octets = [0u8; 16];
                octets.copy_from_slice(&data[offset..offset + 16]);
                Some(DnsRdata::Aaaa(Ipv6Addr::from(octets)))
            }
            5 | 12 => {
                let (name, _) = Self::parse_name(data, offset)?;
                if rtype == 5 {
                    Some(DnsRdata::Cname(name))
                } else {
                    Some(DnsRdata::Ptr(name))
                }
            }
            15 if rdlen >= 2 => {
                let preference = u16::from_be_bytes([data[offset], data[offset + 1]]);
                let (exchange, _) = Self::parse_name(data, offset + 2)?;
                Some(DnsRdata::Mx { preference, exchange })
            }
            16 => {
                let txt = String::from_utf8_lossy(&data[offset..offset + rdlen]).to_string();
                Some(DnsRdata::Txt(txt))
            }
            33 if rdlen >= 6 => {
                let priority = u16::from_be_bytes([data[offset], data[offset + 1]]);
                let weight = u16::from_be_bytes([data[offset + 2], data[offset + 3]]);
                let port = u16::from_be_bytes([data[offset + 4], data[offset + 5]]);
                let (target, _) = Self::parse_name(data, offset + 6)?;
                Some(DnsRdata::Srv { priority, weight, port, target })
            }
            _ => Some(DnsRdata::Unknown(data[offset..offset + rdlen].to_vec())),
        }
    }
    
    fn u16_to_type(value: u16) -> DnsType {
        match value {
            1 => DnsType::A,
            2 => DnsType::Ns,
            5 => DnsType::Cname,
            6 => DnsType::Soa,
            12 => DnsType::Ptr,
            15 => DnsType::Mx,
            16 => DnsType::Txt,
            28 => DnsType::Aaaa,
            33 => DnsType::Srv,
            255 => DnsType::Any,
            _ => DnsType::A,
        }
    }
    
    pub fn get_a_records(&self) -> Vec<Ipv4Addr> {
        self.answers.iter().filter_map(|r| {
            if let DnsRdata::A(addr) = &r.rdata {
                Some(*addr)
            } else {
                None
            }
        }).collect()
    }
    
    pub fn get_aaaa_records(&self) -> Vec<Ipv6Addr> {
        self.answers.iter().filter_map(|r| {
            if let DnsRdata::Aaaa(addr) = &r.rdata {
                Some(*addr)
            } else {
                None
            }
        }).collect()
    }
}

/// DNS cache entry
#[derive(Debug, Clone)]
pub struct DnsCacheEntry {
    pub records: Vec<DnsRecord>,
    pub expires_at: u64,
}

/// DNS resolver
pub struct DnsResolver {
    servers: Vec<Ipv4Addr>,
    cache: Vec<(String, DnsType, DnsCacheEntry)>,
    next_id: u16,
    pending: Vec<PendingQuery>,
    max_cache_size: usize,
}

struct PendingQuery {
    id: u16,
    name: String,
    qtype: DnsType,
    server_index: usize,
    sent_at: u64,
    retries: u8,
}

impl DnsResolver {
    pub fn new() -> Self {
        Self {
            servers: Vec::new(),
            cache: Vec::new(),
            next_id: 1,
            pending: Vec::new(),
            max_cache_size: 100,
        }
    }
    
    pub fn add_server(&mut self, server: Ipv4Addr) {
        if !self.servers.contains(&server) {
            self.servers.push(server);
        }
    }
    
    pub fn set_servers(&mut self, servers: Vec<Ipv4Addr>) {
        self.servers = servers;
    }
    
    /// Resolve hostname to IPv4 address
    pub fn resolve(&mut self, name: &str, timestamp: u64) -> DnsAction {
        self.resolve_type(name, DnsType::A, timestamp)
    }
    
    /// Resolve hostname to IPv6 address
    pub fn resolve_ipv6(&mut self, name: &str, timestamp: u64) -> DnsAction {
        self.resolve_type(name, DnsType::Aaaa, timestamp)
    }
    
    fn resolve_type(&mut self, name: &str, qtype: DnsType, timestamp: u64) -> DnsAction {
        // Check cache
        if let Some(entry) = self.lookup_cache(name, qtype, timestamp) {
            return DnsAction::Resolved(entry.records.clone());
        }
        
        // No servers configured
        if self.servers.is_empty() {
            return DnsAction::Failed(DnsError::NoServers);
        }
        
        // Check if already pending
        if self.pending.iter().any(|p| p.name == name && p.qtype == qtype) {
            return DnsAction::Pending;
        }
        
        // Create query
        let id = self.next_id;
        self.next_id = self.next_id.wrapping_add(1);
        
        let query = DnsMessage::new_query(id, name, qtype);
        let packet = query.build();
        
        self.pending.push(PendingQuery {
            id,
            name: name.to_string(),
            qtype,
            server_index: 0,
            sent_at: timestamp,
            retries: 0,
        });
        
        DnsAction::SendQuery {
            packet,
            server: self.servers[0],
        }
    }
    
    fn lookup_cache(&self, name: &str, qtype: DnsType, timestamp: u64) -> Option<&DnsCacheEntry> {
        self.cache.iter()
            .find(|(n, t, entry)| n == name && *t == qtype && entry.expires_at > timestamp)
            .map(|(_, _, entry)| entry)
    }
    
    /// Process DNS response
    pub fn process_response(&mut self, data: &[u8], timestamp: u64) -> DnsAction {
        let message = match DnsMessage::parse(data) {
            Some(m) => m,
            None => return DnsAction::None,
        };
        
        // Find pending query
        let pending_idx = self.pending.iter().position(|p| p.id == message.id);
        let pending = match pending_idx {
            Some(idx) => self.pending.remove(idx),
            None => return DnsAction::None,
        };
        
        // Check response code
        if message.flags.rcode != 0 {
            return DnsAction::Failed(DnsError::ResponseError(message.flags.rcode));
        }
        
        // Cache results
        if !message.answers.is_empty() {
            let min_ttl = message.answers.iter()
                .map(|r| r.ttl)
                .min()
                .unwrap_or(300);
            
            let entry = DnsCacheEntry {
                records: message.answers.clone(),
                expires_at: timestamp + (min_ttl as u64 * 1000),
            };
            
            // Remove old entry if exists
            self.cache.retain(|(n, t, _)| n != &pending.name || *t != pending.qtype);
            
            // Add new entry
            self.cache.push((pending.name, pending.qtype, entry));
            
            // Limit cache size
            while self.cache.len() > self.max_cache_size {
                self.cache.remove(0);
            }
        }
        
        DnsAction::Resolved(message.answers)
    }
    
    /// Check for timeouts and retry
    pub fn tick(&mut self, timestamp: u64) -> Vec<DnsAction> {
        let mut actions = Vec::new();
        let mut timed_out = Vec::new();
        
        for (idx, pending) in self.pending.iter_mut().enumerate() {
            if timestamp - pending.sent_at >= 3000 {
                pending.retries += 1;
                
                if pending.retries >= 3 {
                    timed_out.push(idx);
                } else {
                    // Try next server
                    pending.server_index = (pending.server_index + 1) % self.servers.len();
                    pending.sent_at = timestamp;
                    
                    let query = DnsMessage::new_query(pending.id, &pending.name, pending.qtype);
                    actions.push(DnsAction::SendQuery {
                        packet: query.build(),
                        server: self.servers[pending.server_index],
                    });
                }
            }
        }
        
        // Remove timed out queries
        for idx in timed_out.into_iter().rev() {
            self.pending.remove(idx);
            actions.push(DnsAction::Failed(DnsError::Timeout));
        }
        
        actions
    }
    
    /// Flush cache
    pub fn flush_cache(&mut self) {
        self.cache.clear();
    }
    
    /// Get configured servers
    pub fn servers(&self) -> &[Ipv4Addr] {
        &self.servers
    }
    
    /// Reverse DNS lookup
    pub fn reverse_lookup(&mut self, addr: Ipv4Addr, timestamp: u64) -> DnsAction {
        let octets = addr.octets();
        let name = format!("{}.{}.{}.{}.in-addr.arpa",
            octets[3], octets[2], octets[1], octets[0]);
        self.resolve_type(&name, DnsType::Ptr, timestamp)
    }
}

impl Default for DnsResolver {
    fn default() -> Self {
        Self::new()
    }
}

/// DNS action
#[derive(Debug, Clone)]
pub enum DnsAction {
    None,
    Pending,
    SendQuery { packet: Vec<u8>, server: Ipv4Addr },
    Resolved(Vec<DnsRecord>),
    Failed(DnsError),
}

/// DNS error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DnsError {
    NoServers,
    Timeout,
    ResponseError(u8),
    InvalidResponse,
}
