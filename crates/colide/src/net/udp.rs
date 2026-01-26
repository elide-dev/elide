//! UDP (User Datagram Protocol) Implementation
//!
//! Implements connectionless datagram transport for
//! DNS, DHCP, and other UDP-based protocols.

use core::net::{Ipv4Addr, SocketAddrV4};

/// UDP datagram
#[derive(Debug, Clone)]
pub struct UdpDatagram {
    pub src_port: u16,
    pub dst_port: u16,
    pub length: u16,
    pub checksum: u16,
    pub payload: Vec<u8>,
}

impl UdpDatagram {
    pub fn new(src_port: u16, dst_port: u16, payload: Vec<u8>) -> Self {
        Self {
            src_port,
            dst_port,
            length: (8 + payload.len()) as u16,
            checksum: 0,
            payload,
        }
    }
    
    pub fn build(&self, src_ip: Ipv4Addr, dst_ip: Ipv4Addr) -> Vec<u8> {
        let mut datagram = Vec::with_capacity(8 + self.payload.len());
        
        datagram.extend_from_slice(&self.src_port.to_be_bytes());
        datagram.extend_from_slice(&self.dst_port.to_be_bytes());
        datagram.extend_from_slice(&self.length.to_be_bytes());
        datagram.extend_from_slice(&[0, 0]);  // Checksum placeholder
        datagram.extend_from_slice(&self.payload);
        
        // Calculate checksum
        let checksum = Self::calculate_checksum(&datagram, src_ip, dst_ip);
        datagram[6] = (checksum >> 8) as u8;
        datagram[7] = (checksum & 0xFF) as u8;
        
        datagram
    }
    
    fn calculate_checksum(data: &[u8], src_ip: Ipv4Addr, dst_ip: Ipv4Addr) -> u16 {
        let mut sum: u32 = 0;
        
        // Pseudo header
        let src = src_ip.octets();
        let dst = dst_ip.octets();
        sum += u16::from_be_bytes([src[0], src[1]]) as u32;
        sum += u16::from_be_bytes([src[2], src[3]]) as u32;
        sum += u16::from_be_bytes([dst[0], dst[1]]) as u32;
        sum += u16::from_be_bytes([dst[2], dst[3]]) as u32;
        sum += 17u32;  // Protocol: UDP
        sum += data.len() as u32;
        
        // UDP datagram
        for chunk in data.chunks(2) {
            if chunk.len() == 2 {
                sum += u16::from_be_bytes([chunk[0], chunk[1]]) as u32;
            } else {
                sum += (chunk[0] as u32) << 8;
            }
        }
        
        while sum > 0xFFFF {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        
        let result = !(sum as u16);
        if result == 0 { 0xFFFF } else { result }
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 8 {
            return None;
        }
        
        let src_port = u16::from_be_bytes([data[0], data[1]]);
        let dst_port = u16::from_be_bytes([data[2], data[3]]);
        let length = u16::from_be_bytes([data[4], data[5]]);
        let checksum = u16::from_be_bytes([data[6], data[7]]);
        
        if (length as usize) < 8 || data.len() < length as usize {
            return None;
        }
        
        let payload = data[8..length as usize].to_vec();
        
        Some(Self {
            src_port,
            dst_port,
            length,
            checksum,
            payload,
        })
    }
}

/// UDP socket
pub struct UdpSocket {
    local: SocketAddrV4,
    rx_buffer: Vec<(SocketAddrV4, Vec<u8>)>,
    max_buffer_size: usize,
}

impl UdpSocket {
    pub fn new(local: SocketAddrV4) -> Self {
        Self {
            local,
            rx_buffer: Vec::new(),
            max_buffer_size: 64,
        }
    }
    
    pub fn bind(port: u16) -> Self {
        Self::new(SocketAddrV4::new(Ipv4Addr::UNSPECIFIED, port))
    }
    
    /// Send datagram to remote address
    pub fn send_to(&self, data: &[u8], remote: SocketAddrV4, src_ip: Ipv4Addr) -> Vec<u8> {
        let datagram = UdpDatagram::new(self.local.port(), remote.port(), data.to_vec());
        
        // Build IP packet
        let udp_data = datagram.build(src_ip, *remote.ip());
        
        let ip_packet = super::tcp::Ipv4Packet::new(
            src_ip,
            *remote.ip(),
            super::tcp::Ipv4Packet::PROTO_UDP,
            udp_data,
        );
        
        ip_packet.build()
    }
    
    /// Process incoming datagram
    pub fn on_receive(&mut self, datagram: &UdpDatagram, src_ip: Ipv4Addr) {
        let remote = SocketAddrV4::new(src_ip, datagram.src_port);
        
        // Add to buffer
        if self.rx_buffer.len() < self.max_buffer_size {
            self.rx_buffer.push((remote, datagram.payload.clone()));
        }
    }
    
    /// Receive datagram from buffer
    pub fn recv_from(&mut self) -> Option<(SocketAddrV4, Vec<u8>)> {
        if self.rx_buffer.is_empty() {
            None
        } else {
            Some(self.rx_buffer.remove(0))
        }
    }
    
    /// Peek at next datagram without removing
    pub fn peek(&self) -> Option<(&SocketAddrV4, &[u8])> {
        self.rx_buffer.first().map(|(addr, data)| (addr, data.as_slice()))
    }
    
    pub fn local(&self) -> SocketAddrV4 {
        self.local
    }
    
    pub fn available(&self) -> usize {
        self.rx_buffer.len()
    }
    
    pub fn set_max_buffer(&mut self, size: usize) {
        self.max_buffer_size = size;
    }
}

/// Simple ICMP support for ping
pub struct IcmpPacket {
    pub icmp_type: u8,
    pub code: u8,
    pub checksum: u16,
    pub identifier: u16,
    pub sequence: u16,
    pub payload: Vec<u8>,
}

impl IcmpPacket {
    pub const ECHO_REQUEST: u8 = 8;
    pub const ECHO_REPLY: u8 = 0;
    pub const DEST_UNREACHABLE: u8 = 3;
    pub const TIME_EXCEEDED: u8 = 11;
    
    pub fn echo_request(identifier: u16, sequence: u16, payload: &[u8]) -> Self {
        Self {
            icmp_type: Self::ECHO_REQUEST,
            code: 0,
            checksum: 0,
            identifier,
            sequence,
            payload: payload.to_vec(),
        }
    }
    
    pub fn echo_reply(identifier: u16, sequence: u16, payload: &[u8]) -> Self {
        Self {
            icmp_type: Self::ECHO_REPLY,
            code: 0,
            checksum: 0,
            identifier,
            sequence,
            payload: payload.to_vec(),
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(8 + self.payload.len());
        
        packet.push(self.icmp_type);
        packet.push(self.code);
        packet.extend_from_slice(&[0, 0]);  // Checksum placeholder
        packet.extend_from_slice(&self.identifier.to_be_bytes());
        packet.extend_from_slice(&self.sequence.to_be_bytes());
        packet.extend_from_slice(&self.payload);
        
        // Calculate checksum
        let checksum = Self::calculate_checksum(&packet);
        packet[2] = (checksum >> 8) as u8;
        packet[3] = (checksum & 0xFF) as u8;
        
        packet
    }
    
    fn calculate_checksum(data: &[u8]) -> u16 {
        let mut sum: u32 = 0;
        
        for chunk in data.chunks(2) {
            if chunk.len() == 2 {
                sum += u16::from_be_bytes([chunk[0], chunk[1]]) as u32;
            } else {
                sum += (chunk[0] as u32) << 8;
            }
        }
        
        while sum > 0xFFFF {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        
        !(sum as u16)
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 8 {
            return None;
        }
        
        Some(Self {
            icmp_type: data[0],
            code: data[1],
            checksum: u16::from_be_bytes([data[2], data[3]]),
            identifier: u16::from_be_bytes([data[4], data[5]]),
            sequence: u16::from_be_bytes([data[6], data[7]]),
            payload: data[8..].to_vec(),
        })
    }
    
    pub fn is_echo_request(&self) -> bool {
        self.icmp_type == Self::ECHO_REQUEST
    }
    
    pub fn is_echo_reply(&self) -> bool {
        self.icmp_type == Self::ECHO_REPLY
    }
}

/// Ping utility
pub struct Pinger {
    src_ip: Ipv4Addr,
    identifier: u16,
    sequence: u16,
    pending: Vec<PendingPing>,
}

struct PendingPing {
    dst_ip: Ipv4Addr,
    sequence: u16,
    sent_at: u64,
}

impl Pinger {
    pub fn new(src_ip: Ipv4Addr) -> Self {
        Self {
            src_ip,
            identifier: 1234,
            sequence: 0,
            pending: Vec::new(),
        }
    }
    
    /// Send ping to target
    pub fn ping(&mut self, dst_ip: Ipv4Addr, timestamp: u64) -> Vec<u8> {
        self.sequence = self.sequence.wrapping_add(1);
        
        // Timestamp as payload
        let payload = timestamp.to_be_bytes().to_vec();
        
        let icmp = IcmpPacket::echo_request(self.identifier, self.sequence, &payload);
        let icmp_data = icmp.build();
        
        let ip_packet = super::tcp::Ipv4Packet::new(
            self.src_ip,
            dst_ip,
            super::tcp::Ipv4Packet::PROTO_ICMP,
            icmp_data,
        );
        
        self.pending.push(PendingPing {
            dst_ip,
            sequence: self.sequence,
            sent_at: timestamp,
        });
        
        ip_packet.build()
    }
    
    /// Process ICMP reply
    pub fn on_reply(&mut self, icmp: &IcmpPacket, src_ip: Ipv4Addr, timestamp: u64) -> Option<PingResult> {
        if !icmp.is_echo_reply() || icmp.identifier != self.identifier {
            return None;
        }
        
        let idx = self.pending.iter().position(|p| 
            p.dst_ip == src_ip && p.sequence == icmp.sequence
        )?;
        
        let pending = self.pending.remove(idx);
        let rtt = timestamp - pending.sent_at;
        
        Some(PingResult {
            addr: src_ip,
            sequence: icmp.sequence,
            rtt_ms: rtt,
            ttl: 0,  // Would need to extract from IP header
        })
    }
    
    /// Check for timeouts
    pub fn check_timeout(&mut self, timestamp: u64, timeout_ms: u64) -> Vec<PingResult> {
        let mut timed_out = Vec::new();
        
        self.pending.retain(|p| {
            if timestamp - p.sent_at > timeout_ms {
                timed_out.push(PingResult {
                    addr: p.dst_ip,
                    sequence: p.sequence,
                    rtt_ms: 0,
                    ttl: 0,
                });
                false
            } else {
                true
            }
        });
        
        timed_out
    }
}

/// Ping result
#[derive(Debug, Clone)]
pub struct PingResult {
    pub addr: Ipv4Addr,
    pub sequence: u16,
    pub rtt_ms: u64,
    pub ttl: u8,
}

impl PingResult {
    pub fn is_timeout(&self) -> bool {
        self.rtt_ms == 0
    }
}

/// Network stack combining all protocols
pub struct NetworkStack {
    pub local_ip: Ipv4Addr,
    pub subnet_mask: Ipv4Addr,
    pub gateway: Option<Ipv4Addr>,
    pub mac: [u8; 6],
    udp_sockets: Vec<UdpSocket>,
    next_ephemeral_port: u16,
}

impl NetworkStack {
    pub fn new(mac: [u8; 6]) -> Self {
        Self {
            local_ip: Ipv4Addr::UNSPECIFIED,
            subnet_mask: Ipv4Addr::new(255, 255, 255, 0),
            gateway: None,
            mac,
            udp_sockets: Vec::new(),
            next_ephemeral_port: 49152,
        }
    }
    
    /// Configure from DHCP lease
    pub fn configure(&mut self, lease: &super::dhcp::DhcpLease) {
        self.local_ip = lease.client_ip;
        self.subnet_mask = lease.subnet_mask;
        self.gateway = lease.gateway;
    }
    
    /// Get next ephemeral port
    pub fn ephemeral_port(&mut self) -> u16 {
        let port = self.next_ephemeral_port;
        self.next_ephemeral_port = self.next_ephemeral_port.wrapping_add(1);
        if self.next_ephemeral_port < 49152 {
            self.next_ephemeral_port = 49152;
        }
        port
    }
    
    /// Check if destination is on local network
    pub fn is_local(&self, dst: Ipv4Addr) -> bool {
        let local = u32::from_be_bytes(self.local_ip.octets());
        let mask = u32::from_be_bytes(self.subnet_mask.octets());
        let target = u32::from_be_bytes(dst.octets());
        
        (local & mask) == (target & mask)
    }
    
    /// Get next hop for destination
    pub fn next_hop(&self, dst: Ipv4Addr) -> Option<Ipv4Addr> {
        if self.is_local(dst) {
            Some(dst)
        } else {
            self.gateway
        }
    }
    
    /// Create UDP socket
    pub fn udp_socket(&mut self, port: u16) -> &mut UdpSocket {
        let socket = UdpSocket::bind(port);
        self.udp_sockets.push(socket);
        self.udp_sockets.last_mut().unwrap()
    }
    
    pub fn is_configured(&self) -> bool {
        self.local_ip != Ipv4Addr::UNSPECIFIED
    }
}
