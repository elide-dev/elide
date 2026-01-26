//! DHCP Client Implementation
//!
//! Implements DHCP (Dynamic Host Configuration Protocol) for
//! automatic IP address configuration on WiFi networks.

use core::net::Ipv4Addr;

/// DHCP state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum DhcpState {
    #[default]
    Init,
    Selecting,
    Requesting,
    Bound,
    Renewing,
    Rebinding,
    InitReboot,
    Rebooting,
}

/// DHCP message type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum DhcpMessageType {
    Discover = 1,
    Offer = 2,
    Request = 3,
    Decline = 4,
    Ack = 5,
    Nak = 6,
    Release = 7,
    Inform = 8,
}

/// DHCP option codes
#[derive(Debug, Clone, Copy)]
#[repr(u8)]
pub enum DhcpOption {
    Pad = 0,
    SubnetMask = 1,
    Router = 3,
    DnsServer = 6,
    HostName = 12,
    DomainName = 15,
    BroadcastAddr = 28,
    RequestedIp = 50,
    LeaseTime = 51,
    MessageType = 53,
    ServerIdentifier = 54,
    ParameterRequestList = 55,
    RenewalTime = 58,
    RebindingTime = 59,
    ClientIdentifier = 61,
    End = 255,
}

/// DHCP lease information
#[derive(Debug, Clone)]
pub struct DhcpLease {
    pub client_ip: Ipv4Addr,
    pub server_ip: Ipv4Addr,
    pub subnet_mask: Ipv4Addr,
    pub gateway: Option<Ipv4Addr>,
    pub dns_servers: Vec<Ipv4Addr>,
    pub domain_name: Option<String>,
    pub lease_time: u32,
    pub renewal_time: u32,
    pub rebinding_time: u32,
    pub obtained_at: u64,
}

impl DhcpLease {
    pub fn is_expired(&self, now: u64) -> bool {
        now - self.obtained_at >= self.lease_time as u64 * 1000
    }
    
    pub fn needs_renewal(&self, now: u64) -> bool {
        now - self.obtained_at >= self.renewal_time as u64 * 1000
    }
    
    pub fn needs_rebinding(&self, now: u64) -> bool {
        now - self.obtained_at >= self.rebinding_time as u64 * 1000
    }
}

/// DHCP packet
#[derive(Debug, Clone)]
pub struct DhcpPacket {
    pub op: u8,
    pub htype: u8,
    pub hlen: u8,
    pub hops: u8,
    pub xid: u32,
    pub secs: u16,
    pub flags: u16,
    pub ciaddr: Ipv4Addr,
    pub yiaddr: Ipv4Addr,
    pub siaddr: Ipv4Addr,
    pub giaddr: Ipv4Addr,
    pub chaddr: [u8; 16],
    pub sname: [u8; 64],
    pub file: [u8; 128],
    pub options: Vec<(u8, Vec<u8>)>,
}

impl DhcpPacket {
    pub const BOOTREQUEST: u8 = 1;
    pub const BOOTREPLY: u8 = 2;
    pub const MAGIC_COOKIE: [u8; 4] = [99, 130, 83, 99];
    
    pub fn new_request(mac: [u8; 6], xid: u32) -> Self {
        let mut chaddr = [0u8; 16];
        chaddr[..6].copy_from_slice(&mac);
        
        Self {
            op: Self::BOOTREQUEST,
            htype: 1,  // Ethernet
            hlen: 6,
            hops: 0,
            xid,
            secs: 0,
            flags: 0x8000,  // Broadcast
            ciaddr: Ipv4Addr::UNSPECIFIED,
            yiaddr: Ipv4Addr::UNSPECIFIED,
            siaddr: Ipv4Addr::UNSPECIFIED,
            giaddr: Ipv4Addr::UNSPECIFIED,
            chaddr,
            sname: [0; 64],
            file: [0; 128],
            options: Vec::new(),
        }
    }
    
    pub fn add_option(&mut self, code: DhcpOption, value: &[u8]) {
        self.options.push((code as u8, value.to_vec()));
    }
    
    pub fn add_message_type(&mut self, msg_type: DhcpMessageType) {
        self.add_option(DhcpOption::MessageType, &[msg_type as u8]);
    }
    
    pub fn add_parameter_request(&mut self) {
        self.add_option(DhcpOption::ParameterRequestList, &[
            DhcpOption::SubnetMask as u8,
            DhcpOption::Router as u8,
            DhcpOption::DnsServer as u8,
            DhcpOption::DomainName as u8,
            DhcpOption::LeaseTime as u8,
            DhcpOption::RenewalTime as u8,
            DhcpOption::RebindingTime as u8,
        ]);
    }
    
    pub fn add_requested_ip(&mut self, ip: Ipv4Addr) {
        self.add_option(DhcpOption::RequestedIp, &ip.octets());
    }
    
    pub fn add_server_identifier(&mut self, ip: Ipv4Addr) {
        self.add_option(DhcpOption::ServerIdentifier, &ip.octets());
    }
    
    pub fn add_client_identifier(&mut self, mac: [u8; 6]) {
        let mut value = vec![1u8];  // Hardware type: Ethernet
        value.extend_from_slice(&mac);
        self.add_option(DhcpOption::ClientIdentifier, &value);
    }
    
    pub fn add_hostname(&mut self, hostname: &str) {
        self.add_option(DhcpOption::HostName, hostname.as_bytes());
    }
    
    pub fn get_option(&self, code: DhcpOption) -> Option<&[u8]> {
        self.options.iter()
            .find(|(c, _)| *c == code as u8)
            .map(|(_, v)| v.as_slice())
    }
    
    pub fn get_message_type(&self) -> Option<DhcpMessageType> {
        self.get_option(DhcpOption::MessageType)
            .and_then(|v| v.first())
            .and_then(|&t| match t {
                1 => Some(DhcpMessageType::Discover),
                2 => Some(DhcpMessageType::Offer),
                3 => Some(DhcpMessageType::Request),
                4 => Some(DhcpMessageType::Decline),
                5 => Some(DhcpMessageType::Ack),
                6 => Some(DhcpMessageType::Nak),
                7 => Some(DhcpMessageType::Release),
                8 => Some(DhcpMessageType::Inform),
                _ => None,
            })
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(576);
        
        packet.push(self.op);
        packet.push(self.htype);
        packet.push(self.hlen);
        packet.push(self.hops);
        packet.extend_from_slice(&self.xid.to_be_bytes());
        packet.extend_from_slice(&self.secs.to_be_bytes());
        packet.extend_from_slice(&self.flags.to_be_bytes());
        packet.extend_from_slice(&self.ciaddr.octets());
        packet.extend_from_slice(&self.yiaddr.octets());
        packet.extend_from_slice(&self.siaddr.octets());
        packet.extend_from_slice(&self.giaddr.octets());
        packet.extend_from_slice(&self.chaddr);
        packet.extend_from_slice(&self.sname);
        packet.extend_from_slice(&self.file);
        
        // Magic cookie
        packet.extend_from_slice(&Self::MAGIC_COOKIE);
        
        // Options
        for (code, value) in &self.options {
            packet.push(*code);
            packet.push(value.len() as u8);
            packet.extend_from_slice(value);
        }
        
        // End option
        packet.push(DhcpOption::End as u8);
        
        // Pad to minimum size
        while packet.len() < 300 {
            packet.push(0);
        }
        
        packet
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 240 {
            return None;
        }
        
        let op = data[0];
        let htype = data[1];
        let hlen = data[2];
        let hops = data[3];
        let xid = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
        let secs = u16::from_be_bytes([data[8], data[9]]);
        let flags = u16::from_be_bytes([data[10], data[11]]);
        let ciaddr = Ipv4Addr::new(data[12], data[13], data[14], data[15]);
        let yiaddr = Ipv4Addr::new(data[16], data[17], data[18], data[19]);
        let siaddr = Ipv4Addr::new(data[20], data[21], data[22], data[23]);
        let giaddr = Ipv4Addr::new(data[24], data[25], data[26], data[27]);
        
        let mut chaddr = [0u8; 16];
        chaddr.copy_from_slice(&data[28..44]);
        
        let mut sname = [0u8; 64];
        sname.copy_from_slice(&data[44..108]);
        
        let mut file = [0u8; 128];
        file.copy_from_slice(&data[108..236]);
        
        // Check magic cookie
        if data[236..240] != Self::MAGIC_COOKIE {
            return None;
        }
        
        // Parse options
        let mut options = Vec::new();
        let mut offset = 240;
        
        while offset < data.len() {
            let code = data[offset];
            
            if code == DhcpOption::End as u8 {
                break;
            }
            
            if code == DhcpOption::Pad as u8 {
                offset += 1;
                continue;
            }
            
            if offset + 1 >= data.len() {
                break;
            }
            
            let len = data[offset + 1] as usize;
            
            if offset + 2 + len > data.len() {
                break;
            }
            
            options.push((code, data[offset + 2..offset + 2 + len].to_vec()));
            offset += 2 + len;
        }
        
        Some(Self {
            op,
            htype,
            hlen,
            hops,
            xid,
            secs,
            flags,
            ciaddr,
            yiaddr,
            siaddr,
            giaddr,
            chaddr,
            sname,
            file,
            options,
        })
    }
}

/// DHCP client
pub struct DhcpClient {
    state: DhcpState,
    mac: [u8; 6],
    xid: u32,
    hostname: String,
    lease: Option<DhcpLease>,
    offered_ip: Option<Ipv4Addr>,
    server_ip: Option<Ipv4Addr>,
    retry_count: u8,
    last_send: u64,
    timeout_ms: u64,
}

impl DhcpClient {
    pub fn new(mac: [u8; 6], hostname: &str) -> Self {
        // Generate XID from MAC
        let xid = u32::from_be_bytes([mac[2], mac[3], mac[4], mac[5]]);
        
        Self {
            state: DhcpState::Init,
            mac,
            xid,
            hostname: hostname.to_string(),
            lease: None,
            offered_ip: None,
            server_ip: None,
            retry_count: 0,
            last_send: 0,
            timeout_ms: 4000,
        }
    }
    
    /// Start DHCP discovery
    pub fn start(&mut self, timestamp: u64) -> DhcpAction {
        self.state = DhcpState::Selecting;
        self.retry_count = 0;
        self.xid = self.xid.wrapping_add(1);
        self.last_send = timestamp;
        
        let packet = self.build_discover();
        DhcpAction::SendPacket(packet.build())
    }
    
    /// Check for timeout and retry
    pub fn tick(&mut self, timestamp: u64) -> DhcpAction {
        // Check lease renewal
        if let Some(lease) = &self.lease {
            if self.state == DhcpState::Bound {
                if lease.is_expired(timestamp) {
                    self.state = DhcpState::Init;
                    return self.start(timestamp);
                }
                
                if lease.needs_rebinding(timestamp) {
                    self.state = DhcpState::Rebinding;
                    return self.send_request(timestamp, true);
                }
                
                if lease.needs_renewal(timestamp) {
                    self.state = DhcpState::Renewing;
                    return self.send_request(timestamp, false);
                }
            }
        }
        
        // Check for timeout
        if timestamp - self.last_send >= self.timeout_ms {
            self.retry_count += 1;
            
            if self.retry_count >= 4 {
                self.state = DhcpState::Init;
                return DhcpAction::Failed;
            }
            
            // Exponential backoff
            self.timeout_ms = (self.timeout_ms * 2).min(64000);
            self.last_send = timestamp;
            
            match self.state {
                DhcpState::Selecting => {
                    let packet = self.build_discover();
                    return DhcpAction::SendPacket(packet.build());
                }
                DhcpState::Requesting | DhcpState::Renewing | DhcpState::Rebinding => {
                    return self.send_request(timestamp, self.state == DhcpState::Rebinding);
                }
                _ => {}
            }
        }
        
        DhcpAction::None
    }
    
    /// Process incoming DHCP packet
    pub fn process(&mut self, data: &[u8], timestamp: u64) -> DhcpAction {
        let packet = match DhcpPacket::parse(data) {
            Some(p) => p,
            None => return DhcpAction::None,
        };
        
        // Verify XID
        if packet.xid != self.xid {
            return DhcpAction::None;
        }
        
        // Verify it's a reply
        if packet.op != DhcpPacket::BOOTREPLY {
            return DhcpAction::None;
        }
        
        match packet.get_message_type() {
            Some(DhcpMessageType::Offer) if self.state == DhcpState::Selecting => {
                self.handle_offer(&packet, timestamp)
            }
            Some(DhcpMessageType::Ack) => {
                self.handle_ack(&packet, timestamp)
            }
            Some(DhcpMessageType::Nak) => {
                self.state = DhcpState::Init;
                DhcpAction::Failed
            }
            _ => DhcpAction::None,
        }
    }
    
    fn handle_offer(&mut self, packet: &DhcpPacket, timestamp: u64) -> DhcpAction {
        self.offered_ip = Some(packet.yiaddr);
        
        // Get server identifier
        self.server_ip = packet.get_option(DhcpOption::ServerIdentifier)
            .filter(|v| v.len() == 4)
            .map(|v| Ipv4Addr::new(v[0], v[1], v[2], v[3]));
        
        self.state = DhcpState::Requesting;
        self.retry_count = 0;
        self.timeout_ms = 4000;
        
        self.send_request(timestamp, false)
    }
    
    fn handle_ack(&mut self, packet: &DhcpPacket, timestamp: u64) -> DhcpAction {
        // Parse lease information
        let client_ip = packet.yiaddr;
        
        let server_ip = self.server_ip.unwrap_or(packet.siaddr);
        
        let subnet_mask = packet.get_option(DhcpOption::SubnetMask)
            .filter(|v| v.len() == 4)
            .map(|v| Ipv4Addr::new(v[0], v[1], v[2], v[3]))
            .unwrap_or(Ipv4Addr::new(255, 255, 255, 0));
        
        let gateway = packet.get_option(DhcpOption::Router)
            .filter(|v| v.len() >= 4)
            .map(|v| Ipv4Addr::new(v[0], v[1], v[2], v[3]));
        
        let mut dns_servers = Vec::new();
        if let Some(dns_data) = packet.get_option(DhcpOption::DnsServer) {
            for chunk in dns_data.chunks_exact(4) {
                dns_servers.push(Ipv4Addr::new(chunk[0], chunk[1], chunk[2], chunk[3]));
            }
        }
        
        let domain_name = packet.get_option(DhcpOption::DomainName)
            .and_then(|v| String::from_utf8(v.to_vec()).ok());
        
        let lease_time = packet.get_option(DhcpOption::LeaseTime)
            .filter(|v| v.len() == 4)
            .map(|v| u32::from_be_bytes([v[0], v[1], v[2], v[3]]))
            .unwrap_or(86400);
        
        let renewal_time = packet.get_option(DhcpOption::RenewalTime)
            .filter(|v| v.len() == 4)
            .map(|v| u32::from_be_bytes([v[0], v[1], v[2], v[3]]))
            .unwrap_or(lease_time / 2);
        
        let rebinding_time = packet.get_option(DhcpOption::RebindingTime)
            .filter(|v| v.len() == 4)
            .map(|v| u32::from_be_bytes([v[0], v[1], v[2], v[3]]))
            .unwrap_or(lease_time * 7 / 8);
        
        let lease = DhcpLease {
            client_ip,
            server_ip,
            subnet_mask,
            gateway,
            dns_servers,
            domain_name,
            lease_time,
            renewal_time,
            rebinding_time,
            obtained_at: timestamp,
        };
        
        self.lease = Some(lease.clone());
        self.state = DhcpState::Bound;
        
        DhcpAction::Configured(lease)
    }
    
    fn build_discover(&self) -> DhcpPacket {
        let mut packet = DhcpPacket::new_request(self.mac, self.xid);
        packet.add_message_type(DhcpMessageType::Discover);
        packet.add_client_identifier(self.mac);
        packet.add_hostname(&self.hostname);
        packet.add_parameter_request();
        packet
    }
    
    fn send_request(&mut self, timestamp: u64, broadcast: bool) -> DhcpAction {
        self.last_send = timestamp;
        
        let mut packet = DhcpPacket::new_request(self.mac, self.xid);
        packet.add_message_type(DhcpMessageType::Request);
        packet.add_client_identifier(self.mac);
        packet.add_hostname(&self.hostname);
        packet.add_parameter_request();
        
        match self.state {
            DhcpState::Requesting => {
                // SELECTING state - include server ID and requested IP
                if let Some(ip) = self.offered_ip {
                    packet.add_requested_ip(ip);
                }
                if let Some(server) = self.server_ip {
                    packet.add_server_identifier(server);
                }
            }
            DhcpState::Renewing => {
                // Unicast to server, use current IP as ciaddr
                if let Some(lease) = &self.lease {
                    packet.ciaddr = lease.client_ip;
                    packet.flags = 0;  // Unicast
                }
            }
            DhcpState::Rebinding => {
                // Broadcast, use current IP as ciaddr
                if let Some(lease) = &self.lease {
                    packet.ciaddr = lease.client_ip;
                }
            }
            _ => {}
        }
        
        if broadcast {
            packet.flags = 0x8000;
        }
        
        DhcpAction::SendPacket(packet.build())
    }
    
    /// Release the current lease
    pub fn release(&mut self) -> Option<DhcpAction> {
        if let Some(lease) = &self.lease {
            let mut packet = DhcpPacket::new_request(self.mac, self.xid);
            packet.add_message_type(DhcpMessageType::Release);
            packet.add_client_identifier(self.mac);
            packet.ciaddr = lease.client_ip;
            packet.add_server_identifier(lease.server_ip);
            
            self.lease = None;
            self.state = DhcpState::Init;
            
            Some(DhcpAction::SendPacket(packet.build()))
        } else {
            None
        }
    }
    
    pub fn state(&self) -> DhcpState {
        self.state
    }
    
    pub fn lease(&self) -> Option<&DhcpLease> {
        self.lease.as_ref()
    }
    
    pub fn is_configured(&self) -> bool {
        self.state == DhcpState::Bound && self.lease.is_some()
    }
}

/// DHCP action
#[derive(Debug, Clone)]
pub enum DhcpAction {
    None,
    SendPacket(Vec<u8>),
    Configured(DhcpLease),
    Failed,
}

/// Simple ARP for IP conflict detection
pub struct ArpProbe {
    pub sender_mac: [u8; 6],
    pub sender_ip: Ipv4Addr,
    pub target_ip: Ipv4Addr,
}

impl ArpProbe {
    pub fn new(mac: [u8; 6], target_ip: Ipv4Addr) -> Self {
        Self {
            sender_mac: mac,
            sender_ip: Ipv4Addr::UNSPECIFIED,
            target_ip,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(42);
        
        // Ethernet header
        packet.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);  // Dest: broadcast
        packet.extend_from_slice(&self.sender_mac);
        packet.extend_from_slice(&[0x08, 0x06]);  // EtherType: ARP
        
        // ARP
        packet.extend_from_slice(&[0x00, 0x01]);  // Hardware type: Ethernet
        packet.extend_from_slice(&[0x08, 0x00]);  // Protocol type: IPv4
        packet.push(6);  // Hardware size
        packet.push(4);  // Protocol size
        packet.extend_from_slice(&[0x00, 0x01]);  // Opcode: Request
        packet.extend_from_slice(&self.sender_mac);
        packet.extend_from_slice(&self.sender_ip.octets());
        packet.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);  // Target MAC
        packet.extend_from_slice(&self.target_ip.octets());
        
        packet
    }
}
