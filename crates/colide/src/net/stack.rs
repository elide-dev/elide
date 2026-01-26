//! Integrated Network Stack
//!
//! Ties together all network components into a unified interface
//! for WiFi connectivity with automatic configuration.

use core::net::{Ipv4Addr, SocketAddrV4};

use super::arp::{ArpCache, ArpPacket, ArpAction, ArpResult, EthernetFrame};
use super::dhcp::{DhcpClient, DhcpAction, DhcpLease};
use super::dns::{DnsResolver, DnsAction, DnsRecord};
use super::tcp::{TcpConnection, TcpSegment, TcpAction, TcpEvent, Ipv4Packet};
use super::udp::{UdpSocket, UdpDatagram, IcmpPacket, Pinger};

/// Network stack state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum StackState {
    #[default]
    Down,
    Configuring,
    Up,
    Error,
}

/// Network stack configuration
#[derive(Debug, Clone)]
pub struct StackConfig {
    pub hostname: String,
    pub static_ip: Option<Ipv4Addr>,
    pub static_netmask: Option<Ipv4Addr>,
    pub static_gateway: Option<Ipv4Addr>,
    pub static_dns: Vec<Ipv4Addr>,
    pub mtu: u16,
}

impl Default for StackConfig {
    fn default() -> Self {
        Self {
            hostname: "colide".to_string(),
            static_ip: None,
            static_netmask: None,
            static_gateway: None,
            static_dns: Vec::new(),
            mtu: 1500,
        }
    }
}

/// Integrated network stack
pub struct NetStack {
    state: StackState,
    config: StackConfig,
    mac: [u8; 6],
    
    // Layer 2
    arp: ArpCache,
    
    // Layer 3/4
    local_ip: Ipv4Addr,
    netmask: Ipv4Addr,
    gateway: Option<Ipv4Addr>,
    
    // Protocols
    dhcp: DhcpClient,
    dns: DnsResolver,
    pinger: Pinger,
    
    // Connections
    tcp_connections: Vec<TcpConnection>,
    udp_sockets: Vec<UdpSocket>,
    next_ephemeral: u16,
    
    // Statistics
    stats: StackStats,
}

/// Network statistics
#[derive(Debug, Clone, Default)]
pub struct StackStats {
    pub rx_packets: u64,
    pub tx_packets: u64,
    pub rx_bytes: u64,
    pub tx_bytes: u64,
    pub rx_errors: u64,
    pub tx_errors: u64,
    pub arp_requests: u64,
    pub arp_replies: u64,
}

impl NetStack {
    pub fn new(mac: [u8; 6], config: StackConfig) -> Self {
        Self {
            state: StackState::Down,
            mac,
            arp: ArpCache::new(mac, Ipv4Addr::UNSPECIFIED),
            local_ip: Ipv4Addr::UNSPECIFIED,
            netmask: Ipv4Addr::new(255, 255, 255, 0),
            gateway: None,
            dhcp: DhcpClient::new(mac, &config.hostname),
            dns: DnsResolver::new(),
            pinger: Pinger::new(Ipv4Addr::UNSPECIFIED),
            tcp_connections: Vec::new(),
            udp_sockets: Vec::new(),
            next_ephemeral: 49152,
            stats: StackStats::default(),
            config,
        }
    }
    
    /// Start the network stack (begin DHCP or use static config)
    pub fn start(&mut self, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        if let Some(ip) = self.config.static_ip {
            // Static configuration
            self.local_ip = ip;
            self.netmask = self.config.static_netmask.unwrap_or(Ipv4Addr::new(255, 255, 255, 0));
            self.gateway = self.config.static_gateway;
            
            self.arp.set_ip(ip);
            self.pinger = Pinger::new(ip);
            
            // Add static DNS servers
            for dns in &self.config.static_dns {
                self.dns.add_server(*dns);
            }
            
            self.state = StackState::Up;
            
            // Send gratuitous ARP
            actions.push(StackAction::Transmit(self.arp.announce()));
            actions.push(StackAction::Configured(self.get_config_info()));
        } else {
            // DHCP configuration
            self.state = StackState::Configuring;
            
            if let DhcpAction::SendPacket(packet) = self.dhcp.start(timestamp) {
                let frame = self.build_dhcp_frame(&packet);
                actions.push(StackAction::Transmit(frame));
            }
        }
        
        actions
    }
    
    /// Stop the network stack
    pub fn stop(&mut self) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        // Release DHCP lease
        if let Some(DhcpAction::SendPacket(packet)) = self.dhcp.release() {
            let frame = self.build_dhcp_frame(&packet);
            actions.push(StackAction::Transmit(frame));
        }
        
        // Close all TCP connections - collect data first to avoid borrow issues
        let close_data: Vec<_> = self.tcp_connections.iter_mut()
            .filter_map(|conn| {
                if let TcpAction::Send(data) = conn.close() {
                    Some((data, conn.remote()))
                } else {
                    None
                }
            })
            .collect();
        
        for (data, remote) in close_data {
            actions.push(StackAction::Transmit(self.wrap_ip_frame(&data, remote)));
        }
        
        self.tcp_connections.clear();
        self.udp_sockets.clear();
        self.arp.flush();
        
        self.state = StackState::Down;
        self.local_ip = Ipv4Addr::UNSPECIFIED;
        
        actions
    }
    
    /// Process incoming Ethernet frame
    pub fn receive(&mut self, frame: &[u8], timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        self.stats.rx_packets += 1;
        self.stats.rx_bytes += frame.len() as u64;
        
        let eth = match EthernetFrame::parse(frame) {
            Some(f) => f,
            None => {
                self.stats.rx_errors += 1;
                return actions;
            }
        };
        
        // Check if frame is for us
        if eth.dst_mac != self.mac && !eth.is_broadcast() && !eth.is_multicast() {
            return actions;
        }
        
        match eth.ethertype {
            EthernetFrame::TYPE_ARP => {
                if let Some(arp) = ArpPacket::parse(&eth.payload) {
                    actions.extend(self.handle_arp(&arp, timestamp));
                }
            }
            EthernetFrame::TYPE_IPV4 => {
                if let Some(ip) = Ipv4Packet::parse(&eth.payload) {
                    // Learn sender's MAC
                    self.arp.process(&ArpPacket::reply(
                        eth.src_mac, ip.src_addr, self.mac, self.local_ip
                    ), timestamp);
                    
                    actions.extend(self.handle_ip(&ip, timestamp));
                }
            }
            EthernetFrame::TYPE_EAPOL => {
                actions.push(StackAction::EapolReceived(eth.payload));
            }
            _ => {}
        }
        
        actions
    }
    
    fn handle_arp(&mut self, arp: &ArpPacket, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        if arp.is_request() {
            self.stats.arp_requests += 1;
        }
        
        match self.arp.process(arp, timestamp) {
            ArpAction::SendReply(frame) => {
                self.stats.arp_replies += 1;
                actions.push(StackAction::Transmit(frame));
            }
            ArpAction::SendQueuedPackets { mac, packets } => {
                for packet in packets {
                    let frame = EthernetFrame::ipv4(mac, self.mac, packet).build();
                    actions.push(StackAction::Transmit(frame));
                }
            }
            _ => {}
        }
        
        actions
    }
    
    fn handle_ip(&mut self, ip: &Ipv4Packet, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        // Check if packet is for us
        if ip.dst_addr != self.local_ip && ip.dst_addr != Ipv4Addr::BROADCAST {
            return actions;
        }
        
        match ip.protocol {
            Ipv4Packet::PROTO_ICMP => {
                if let Some(icmp) = IcmpPacket::parse(&ip.payload) {
                    actions.extend(self.handle_icmp(&icmp, ip.src_addr, timestamp));
                }
            }
            Ipv4Packet::PROTO_TCP => {
                if let Some(tcp) = TcpSegment::parse(&ip.payload) {
                    actions.extend(self.handle_tcp(&tcp, ip.src_addr, timestamp));
                }
            }
            Ipv4Packet::PROTO_UDP => {
                if let Some(udp) = UdpDatagram::parse(&ip.payload) {
                    actions.extend(self.handle_udp(&udp, ip.src_addr, timestamp));
                }
            }
            _ => {}
        }
        
        actions
    }
    
    fn handle_icmp(&mut self, icmp: &IcmpPacket, src: Ipv4Addr, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        if icmp.is_echo_request() {
            // Reply to ping
            let reply = IcmpPacket::echo_reply(icmp.identifier, icmp.sequence, &icmp.payload);
            let ip = Ipv4Packet::new(self.local_ip, src, Ipv4Packet::PROTO_ICMP, reply.build());
            
            if let Some(mac) = self.arp.lookup(src) {
                let frame = EthernetFrame::ipv4(mac, self.mac, ip.build()).build();
                actions.push(StackAction::Transmit(frame));
            }
        } else if icmp.is_echo_reply() {
            if let Some(result) = self.pinger.on_reply(icmp, src, timestamp) {
                actions.push(StackAction::PingResult(result));
            }
        }
        
        actions
    }
    
    fn handle_tcp(&mut self, tcp: &TcpSegment, src: Ipv4Addr, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        // Find matching connection
        let conn_idx = self.tcp_connections.iter().position(|c| {
            c.local().port() == tcp.dst_port && 
            *c.remote().ip() == src && 
            c.remote().port() == tcp.src_port
        });
        
        if let Some(idx) = conn_idx {
            // Process and extract results before calling wrap_ip_frame
            let (action_result, local_port, remote) = {
                let conn = &mut self.tcp_connections[idx];
                let result = conn.process(tcp, timestamp);
                (result, conn.local().port(), conn.remote())
            };
            
            match action_result {
                TcpAction::Send(data) => {
                    actions.push(StackAction::Transmit(self.wrap_ip_frame(&data, remote)));
                }
                TcpAction::SendAndNotify(data, event) => {
                    actions.push(StackAction::Transmit(self.wrap_ip_frame(&data, remote)));
                    actions.push(StackAction::TcpEvent { port: local_port, event });
                }
                TcpAction::Notify(event) => {
                    actions.push(StackAction::TcpEvent { port: local_port, event });
                }
                _ => {}
            }
        }
        
        actions
    }
    
    fn handle_udp(&mut self, udp: &UdpDatagram, src: Ipv4Addr, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        // DHCP response (port 68)
        if udp.dst_port == 68 && self.state == StackState::Configuring {
            match self.dhcp.process(&udp.payload, timestamp) {
                DhcpAction::SendPacket(packet) => {
                    let frame = self.build_dhcp_frame(&packet);
                    actions.push(StackAction::Transmit(frame));
                }
                DhcpAction::Configured(lease) => {
                    self.apply_dhcp_lease(&lease);
                    actions.push(StackAction::Configured(self.get_config_info()));
                    
                    // Send gratuitous ARP
                    actions.push(StackAction::Transmit(self.arp.announce()));
                }
                _ => {}
            }
            return actions;
        }
        
        // DNS response (from configured DNS servers)
        if udp.src_port == 53 {
            match self.dns.process_response(&udp.payload, timestamp) {
                DnsAction::Resolved(records) => {
                    actions.push(StackAction::DnsResolved(records));
                }
                DnsAction::Failed(err) => {
                    actions.push(StackAction::DnsFailed(err));
                }
                _ => {}
            }
            return actions;
        }
        
        // Find matching socket
        if let Some(socket) = self.udp_sockets.iter_mut().find(|s| s.local().port() == udp.dst_port) {
            socket.on_receive(udp, src);
            actions.push(StackAction::UdpReceived { port: udp.dst_port, from: src });
        }
        
        actions
    }
    
    fn apply_dhcp_lease(&mut self, lease: &DhcpLease) {
        self.local_ip = lease.client_ip;
        self.netmask = lease.subnet_mask;
        self.gateway = lease.gateway;
        
        self.arp.set_ip(lease.client_ip);
        self.pinger = Pinger::new(lease.client_ip);
        
        // Configure DNS
        for dns in &lease.dns_servers {
            self.dns.add_server(*dns);
        }
        
        self.state = StackState::Up;
    }
    
    fn build_dhcp_frame(&self, dhcp_packet: &[u8]) -> Vec<u8> {
        let udp = UdpDatagram::new(68, 67, dhcp_packet.to_vec());
        let udp_data = udp.build(Ipv4Addr::UNSPECIFIED, Ipv4Addr::BROADCAST);
        let ip = Ipv4Packet::new(
            Ipv4Addr::UNSPECIFIED,
            Ipv4Addr::BROADCAST,
            Ipv4Packet::PROTO_UDP,
            udp_data,
        );
        
        EthernetFrame::broadcast(self.mac, EthernetFrame::TYPE_IPV4, ip.build()).build()
    }
    
    fn wrap_ip_frame(&mut self, ip_packet: &[u8], remote: SocketAddrV4) -> Vec<u8> {
        let next_hop = if self.is_local(*remote.ip()) {
            *remote.ip()
        } else {
            self.gateway.unwrap_or(*remote.ip())
        };
        
        if let Some(mac) = self.arp.lookup(next_hop) {
            EthernetFrame::ipv4(mac, self.mac, ip_packet.to_vec()).build()
        } else {
            // Queue for ARP resolution
            // For now, just drop (in real impl, would queue)
            Vec::new()
        }
    }
    
    fn is_local(&self, addr: Ipv4Addr) -> bool {
        let local = u32::from_be_bytes(self.local_ip.octets());
        let mask = u32::from_be_bytes(self.netmask.octets());
        let target = u32::from_be_bytes(addr.octets());
        (local & mask) == (target & mask)
    }
    
    /// Periodic tick for timeouts
    pub fn tick(&mut self, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        // DHCP tick
        if self.state == StackState::Configuring || self.state == StackState::Up {
            match self.dhcp.tick(timestamp) {
                DhcpAction::SendPacket(packet) => {
                    let frame = self.build_dhcp_frame(&packet);
                    actions.push(StackAction::Transmit(frame));
                }
                DhcpAction::Failed => {
                    self.state = StackState::Error;
                    actions.push(StackAction::DhcpFailed);
                }
                _ => {}
            }
        }
        
        // ARP tick
        for action in self.arp.tick(timestamp) {
            match action {
                ArpAction::SendRequest(frame) => {
                    actions.push(StackAction::Transmit(frame));
                }
                ArpAction::ResolutionFailed(ip) => {
                    actions.push(StackAction::ArpFailed(ip));
                }
                _ => {}
            }
        }
        
        // DNS tick
        for action in self.dns.tick(timestamp) {
            match action {
                DnsAction::SendQuery { packet, server } => {
                    let remote = SocketAddrV4::new(server, 53);
                    let udp = UdpDatagram::new(self.ephemeral_port(), 53, packet);
                    let ip = Ipv4Packet::new(
                        self.local_ip,
                        server,
                        Ipv4Packet::PROTO_UDP,
                        udp.build(self.local_ip, server),
                    );
                    actions.push(StackAction::Transmit(self.wrap_ip_frame(&ip.build(), remote)));
                }
                DnsAction::Failed(err) => {
                    actions.push(StackAction::DnsFailed(err));
                }
                _ => {}
            }
        }
        
        // Ping timeout check
        for result in self.pinger.check_timeout(timestamp, 5000) {
            actions.push(StackAction::PingResult(result));
        }
        
        actions
    }
    
    /// Connect to remote TCP endpoint
    pub fn tcp_connect(&mut self, remote: SocketAddrV4, timestamp: u64) -> Option<(u16, Vec<StackAction>)> {
        if self.state != StackState::Up {
            return None;
        }
        
        let port = self.ephemeral_port();
        let local = SocketAddrV4::new(self.local_ip, port);
        
        let mut conn = TcpConnection::new(local);
        let action = conn.connect(remote, timestamp);
        
        let mut actions = Vec::new();
        if let TcpAction::Send(data) = action {
            actions.push(StackAction::Transmit(self.wrap_ip_frame(&data, remote)));
        }
        
        self.tcp_connections.push(conn);
        Some((port, actions))
    }
    
    /// Send data on TCP connection
    pub fn tcp_send(&mut self, port: u16, data: &[u8]) -> Option<Vec<StackAction>> {
        let conn = self.tcp_connections.iter_mut().find(|c| c.local().port() == port)?;
        let remote = conn.remote();
        
        let mut actions = Vec::new();
        if let TcpAction::Send(packet) = conn.send(data) {
            actions.push(StackAction::Transmit(self.wrap_ip_frame(&packet, remote)));
        }
        
        Some(actions)
    }
    
    /// Close TCP connection
    pub fn tcp_close(&mut self, port: u16) -> Option<Vec<StackAction>> {
        let conn = self.tcp_connections.iter_mut().find(|c| c.local().port() == port)?;
        let remote = conn.remote();
        
        let mut actions = Vec::new();
        if let TcpAction::Send(packet) = conn.close() {
            actions.push(StackAction::Transmit(self.wrap_ip_frame(&packet, remote)));
        }
        
        Some(actions)
    }
    
    /// Create UDP socket
    pub fn udp_bind(&mut self, port: u16) -> bool {
        if self.udp_sockets.iter().any(|s| s.local().port() == port) {
            return false;
        }
        self.udp_sockets.push(UdpSocket::bind(port));
        true
    }
    
    /// Send UDP datagram
    pub fn udp_send(&mut self, port: u16, data: &[u8], remote: SocketAddrV4) -> Option<StackAction> {
        let socket = self.udp_sockets.iter().find(|s| s.local().port() == port)?;
        let packet = socket.send_to(data, remote, self.local_ip);
        Some(StackAction::Transmit(self.wrap_ip_frame(&packet, remote)))
    }
    
    /// Resolve hostname
    pub fn dns_resolve(&mut self, hostname: &str, timestamp: u64) -> Vec<StackAction> {
        let mut actions = Vec::new();
        
        match self.dns.resolve(hostname, timestamp) {
            DnsAction::SendQuery { packet, server } => {
                let remote = SocketAddrV4::new(server, 53);
                let udp = UdpDatagram::new(self.ephemeral_port(), 53, packet);
                let ip = Ipv4Packet::new(
                    self.local_ip,
                    server,
                    Ipv4Packet::PROTO_UDP,
                    udp.build(self.local_ip, server),
                );
                actions.push(StackAction::Transmit(self.wrap_ip_frame(&ip.build(), remote)));
            }
            DnsAction::Resolved(records) => {
                actions.push(StackAction::DnsResolved(records));
            }
            _ => {}
        }
        
        actions
    }
    
    /// Send ping
    pub fn ping(&mut self, target: Ipv4Addr, timestamp: u64) -> Vec<StackAction> {
        let packet = self.pinger.ping(target, timestamp);
        let remote = SocketAddrV4::new(target, 0);
        vec![StackAction::Transmit(self.wrap_ip_frame(&packet, remote))]
    }
    
    fn ephemeral_port(&mut self) -> u16 {
        let port = self.next_ephemeral;
        self.next_ephemeral = self.next_ephemeral.wrapping_add(1);
        if self.next_ephemeral < 49152 {
            self.next_ephemeral = 49152;
        }
        port
    }
    
    fn get_config_info(&self) -> ConfigInfo {
        ConfigInfo {
            ip: self.local_ip,
            netmask: self.netmask,
            gateway: self.gateway,
            dns: self.dns.servers().to_vec(),
        }
    }
    
    pub fn state(&self) -> StackState {
        self.state
    }
    
    pub fn local_ip(&self) -> Ipv4Addr {
        self.local_ip
    }
    
    pub fn stats(&self) -> &StackStats {
        &self.stats
    }
}

/// Configuration info
#[derive(Debug, Clone)]
pub struct ConfigInfo {
    pub ip: Ipv4Addr,
    pub netmask: Ipv4Addr,
    pub gateway: Option<Ipv4Addr>,
    pub dns: Vec<Ipv4Addr>,
}

/// Stack action
#[derive(Debug)]
pub enum StackAction {
    Transmit(Vec<u8>),
    Configured(ConfigInfo),
    DhcpFailed,
    ArpFailed(Ipv4Addr),
    DnsResolved(Vec<DnsRecord>),
    DnsFailed(super::dns::DnsError),
    PingResult(super::udp::PingResult),
    TcpEvent { port: u16, event: TcpEvent },
    UdpReceived { port: u16, from: Ipv4Addr },
    EapolReceived(Vec<u8>),
}
