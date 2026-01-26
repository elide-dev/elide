//! Minimal TCP/IP Stack
//!
//! Implements a minimal TCP stack for network communication,
//! supporting connection establishment, data transfer, and teardown.

use core::net::{Ipv4Addr, SocketAddrV4};

/// TCP state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum TcpState {
    #[default]
    Closed,
    Listen,
    SynSent,
    SynReceived,
    Established,
    FinWait1,
    FinWait2,
    CloseWait,
    Closing,
    LastAck,
    TimeWait,
}

/// TCP flags
#[derive(Debug, Clone, Copy, Default)]
pub struct TcpFlags {
    pub fin: bool,
    pub syn: bool,
    pub rst: bool,
    pub psh: bool,
    pub ack: bool,
    pub urg: bool,
    pub ece: bool,
    pub cwr: bool,
}

impl TcpFlags {
    pub fn syn() -> Self {
        Self { syn: true, ..Default::default() }
    }
    
    pub fn syn_ack() -> Self {
        Self { syn: true, ack: true, ..Default::default() }
    }
    
    pub fn ack() -> Self {
        Self { ack: true, ..Default::default() }
    }
    
    pub fn fin_ack() -> Self {
        Self { fin: true, ack: true, ..Default::default() }
    }
    
    pub fn psh_ack() -> Self {
        Self { psh: true, ack: true, ..Default::default() }
    }
    
    pub fn rst() -> Self {
        Self { rst: true, ..Default::default() }
    }
    
    pub fn from_u8(value: u8) -> Self {
        Self {
            fin: (value & 0x01) != 0,
            syn: (value & 0x02) != 0,
            rst: (value & 0x04) != 0,
            psh: (value & 0x08) != 0,
            ack: (value & 0x10) != 0,
            urg: (value & 0x20) != 0,
            ece: (value & 0x40) != 0,
            cwr: (value & 0x80) != 0,
        }
    }
    
    pub fn to_u8(&self) -> u8 {
        let mut flags = 0u8;
        if self.fin { flags |= 0x01; }
        if self.syn { flags |= 0x02; }
        if self.rst { flags |= 0x04; }
        if self.psh { flags |= 0x08; }
        if self.ack { flags |= 0x10; }
        if self.urg { flags |= 0x20; }
        if self.ece { flags |= 0x40; }
        if self.cwr { flags |= 0x80; }
        flags
    }
}

/// TCP segment
#[derive(Debug, Clone)]
pub struct TcpSegment {
    pub src_port: u16,
    pub dst_port: u16,
    pub seq_num: u32,
    pub ack_num: u32,
    pub data_offset: u8,
    pub flags: TcpFlags,
    pub window: u16,
    pub checksum: u16,
    pub urgent_ptr: u16,
    pub options: Vec<TcpOption>,
    pub payload: Vec<u8>,
}

/// TCP option
#[derive(Debug, Clone)]
pub enum TcpOption {
    EndOfList,
    Nop,
    Mss(u16),
    WindowScale(u8),
    SackPermitted,
    Sack(Vec<(u32, u32)>),
    Timestamp { tsval: u32, tsecr: u32 },
}

impl TcpSegment {
    pub fn new(src_port: u16, dst_port: u16) -> Self {
        Self {
            src_port,
            dst_port,
            seq_num: 0,
            ack_num: 0,
            data_offset: 5,
            flags: TcpFlags::default(),
            window: 65535,
            checksum: 0,
            urgent_ptr: 0,
            options: Vec::new(),
            payload: Vec::new(),
        }
    }
    
    pub fn build(&self, src_ip: Ipv4Addr, dst_ip: Ipv4Addr) -> Vec<u8> {
        let options_len = self.options_len();
        let header_len = 20 + options_len;
        let total_len = header_len + self.payload.len();
        
        let mut segment = Vec::with_capacity(total_len);
        
        segment.extend_from_slice(&self.src_port.to_be_bytes());
        segment.extend_from_slice(&self.dst_port.to_be_bytes());
        segment.extend_from_slice(&self.seq_num.to_be_bytes());
        segment.extend_from_slice(&self.ack_num.to_be_bytes());
        
        let data_offset = ((header_len / 4) as u8) << 4;
        segment.push(data_offset);
        segment.push(self.flags.to_u8());
        segment.extend_from_slice(&self.window.to_be_bytes());
        segment.extend_from_slice(&[0, 0]);  // Checksum placeholder
        segment.extend_from_slice(&self.urgent_ptr.to_be_bytes());
        
        // Options
        for opt in &self.options {
            match opt {
                TcpOption::EndOfList => segment.push(0),
                TcpOption::Nop => segment.push(1),
                TcpOption::Mss(mss) => {
                    segment.push(2);
                    segment.push(4);
                    segment.extend_from_slice(&mss.to_be_bytes());
                }
                TcpOption::WindowScale(scale) => {
                    segment.push(3);
                    segment.push(3);
                    segment.push(*scale);
                }
                TcpOption::SackPermitted => {
                    segment.push(4);
                    segment.push(2);
                }
                TcpOption::Timestamp { tsval, tsecr } => {
                    segment.push(8);
                    segment.push(10);
                    segment.extend_from_slice(&tsval.to_be_bytes());
                    segment.extend_from_slice(&tsecr.to_be_bytes());
                }
                TcpOption::Sack(blocks) => {
                    segment.push(5);
                    segment.push((2 + blocks.len() * 8) as u8);
                    for (start, end) in blocks {
                        segment.extend_from_slice(&start.to_be_bytes());
                        segment.extend_from_slice(&end.to_be_bytes());
                    }
                }
            }
        }
        
        // Padding
        while segment.len() < header_len {
            segment.push(0);
        }
        
        // Payload
        segment.extend_from_slice(&self.payload);
        
        // Calculate checksum
        let checksum = Self::calculate_checksum(&segment, src_ip, dst_ip);
        segment[16] = (checksum >> 8) as u8;
        segment[17] = (checksum & 0xFF) as u8;
        
        segment
    }
    
    fn options_len(&self) -> usize {
        let mut len = 0;
        for opt in &self.options {
            len += match opt {
                TcpOption::EndOfList | TcpOption::Nop => 1,
                TcpOption::Mss(_) => 4,
                TcpOption::WindowScale(_) => 3,
                TcpOption::SackPermitted => 2,
                TcpOption::Timestamp { .. } => 10,
                TcpOption::Sack(blocks) => 2 + blocks.len() * 8,
            };
        }
        // Pad to 4-byte boundary
        (len + 3) & !3
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
        sum += 6u32;  // Protocol: TCP
        sum += data.len() as u32;
        
        // TCP segment
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
        if data.len() < 20 {
            return None;
        }
        
        let src_port = u16::from_be_bytes([data[0], data[1]]);
        let dst_port = u16::from_be_bytes([data[2], data[3]]);
        let seq_num = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
        let ack_num = u32::from_be_bytes([data[8], data[9], data[10], data[11]]);
        let data_offset = (data[12] >> 4) as usize * 4;
        let flags = TcpFlags::from_u8(data[13]);
        let window = u16::from_be_bytes([data[14], data[15]]);
        let checksum = u16::from_be_bytes([data[16], data[17]]);
        let urgent_ptr = u16::from_be_bytes([data[18], data[19]]);
        
        if data_offset > data.len() {
            return None;
        }
        
        // Parse options
        let mut options = Vec::new();
        let mut offset = 20;
        while offset < data_offset {
            match data[offset] {
                0 => break,
                1 => offset += 1,
                2 if offset + 4 <= data_offset => {
                    let mss = u16::from_be_bytes([data[offset + 2], data[offset + 3]]);
                    options.push(TcpOption::Mss(mss));
                    offset += 4;
                }
                3 if offset + 3 <= data_offset => {
                    options.push(TcpOption::WindowScale(data[offset + 2]));
                    offset += 3;
                }
                4 => {
                    options.push(TcpOption::SackPermitted);
                    offset += 2;
                }
                8 if offset + 10 <= data_offset => {
                    let tsval = u32::from_be_bytes([data[offset + 2], data[offset + 3], data[offset + 4], data[offset + 5]]);
                    let tsecr = u32::from_be_bytes([data[offset + 6], data[offset + 7], data[offset + 8], data[offset + 9]]);
                    options.push(TcpOption::Timestamp { tsval, tsecr });
                    offset += 10;
                }
                _ => {
                    if offset + 1 < data_offset {
                        offset += data[offset + 1] as usize;
                    } else {
                        break;
                    }
                }
            }
        }
        
        let payload = data[data_offset..].to_vec();
        
        Some(Self {
            src_port,
            dst_port,
            seq_num,
            ack_num,
            data_offset: (data_offset / 4) as u8,
            flags,
            window,
            checksum,
            urgent_ptr,
            options,
            payload,
        })
    }
}

/// TCP connection
pub struct TcpConnection {
    state: TcpState,
    local: SocketAddrV4,
    remote: SocketAddrV4,
    snd_una: u32,
    snd_nxt: u32,
    snd_wnd: u16,
    rcv_nxt: u32,
    rcv_wnd: u16,
    iss: u32,
    irs: u32,
    mss: u16,
    rx_buffer: Vec<u8>,
    tx_buffer: Vec<u8>,
    rto: u64,
    last_activity: u64,
}

impl TcpConnection {
    pub fn new(local: SocketAddrV4) -> Self {
        // Generate initial sequence number
        let iss = local.port() as u32 * 12345 + 67890;
        
        Self {
            state: TcpState::Closed,
            local,
            remote: SocketAddrV4::new(Ipv4Addr::UNSPECIFIED, 0),
            snd_una: iss,
            snd_nxt: iss,
            snd_wnd: 0,
            rcv_nxt: 0,
            rcv_wnd: 65535,
            iss,
            irs: 0,
            mss: 1460,
            rx_buffer: Vec::new(),
            tx_buffer: Vec::new(),
            rto: 1000,
            last_activity: 0,
        }
    }
    
    /// Initiate connection (active open)
    pub fn connect(&mut self, remote: SocketAddrV4, timestamp: u64) -> TcpAction {
        self.remote = remote;
        self.state = TcpState::SynSent;
        self.last_activity = timestamp;
        
        let mut segment = TcpSegment::new(self.local.port(), self.remote.port());
        segment.seq_num = self.iss;
        segment.flags = TcpFlags::syn();
        segment.window = self.rcv_wnd;
        segment.options.push(TcpOption::Mss(self.mss));
        segment.options.push(TcpOption::WindowScale(7));
        segment.options.push(TcpOption::SackPermitted);
        
        self.snd_nxt = self.iss.wrapping_add(1);
        
        TcpAction::Send(segment.build(*self.local.ip(), *self.remote.ip()))
    }
    
    /// Start listening (passive open)
    pub fn listen(&mut self) {
        self.state = TcpState::Listen;
    }
    
    /// Process incoming segment
    pub fn process(&mut self, segment: &TcpSegment, timestamp: u64) -> TcpAction {
        self.last_activity = timestamp;
        
        match self.state {
            TcpState::Closed => TcpAction::None,
            
            TcpState::Listen => {
                if segment.flags.syn && !segment.flags.ack {
                    self.remote = SocketAddrV4::new(Ipv4Addr::UNSPECIFIED, segment.src_port);
                    self.irs = segment.seq_num;
                    self.rcv_nxt = segment.seq_num.wrapping_add(1);
                    self.snd_wnd = segment.window;
                    self.state = TcpState::SynReceived;
                    
                    // Extract MSS from options
                    for opt in &segment.options {
                        if let TcpOption::Mss(mss) = opt {
                            self.mss = (*mss).min(1460);
                        }
                    }
                    
                    let mut reply = TcpSegment::new(self.local.port(), segment.src_port);
                    reply.seq_num = self.iss;
                    reply.ack_num = self.rcv_nxt;
                    reply.flags = TcpFlags::syn_ack();
                    reply.window = self.rcv_wnd;
                    reply.options.push(TcpOption::Mss(self.mss));
                    
                    self.snd_nxt = self.iss.wrapping_add(1);
                    
                    return TcpAction::Send(reply.build(*self.local.ip(), *self.remote.ip()));
                }
                TcpAction::None
            }
            
            TcpState::SynSent => {
                if segment.flags.syn && segment.flags.ack {
                    if segment.ack_num != self.snd_nxt {
                        return TcpAction::Reset;
                    }
                    
                    self.irs = segment.seq_num;
                    self.rcv_nxt = segment.seq_num.wrapping_add(1);
                    self.snd_una = segment.ack_num;
                    self.snd_wnd = segment.window;
                    self.state = TcpState::Established;
                    
                    let mut reply = TcpSegment::new(self.local.port(), self.remote.port());
                    reply.seq_num = self.snd_nxt;
                    reply.ack_num = self.rcv_nxt;
                    reply.flags = TcpFlags::ack();
                    reply.window = self.rcv_wnd;
                    
                    return TcpAction::SendAndNotify(
                        reply.build(*self.local.ip(), *self.remote.ip()),
                        TcpEvent::Connected,
                    );
                }
                TcpAction::None
            }
            
            TcpState::SynReceived => {
                if segment.flags.ack && segment.ack_num == self.snd_nxt {
                    self.snd_una = segment.ack_num;
                    self.state = TcpState::Established;
                    return TcpAction::Notify(TcpEvent::Connected);
                }
                TcpAction::None
            }
            
            TcpState::Established => {
                // Handle RST
                if segment.flags.rst {
                    self.state = TcpState::Closed;
                    return TcpAction::Notify(TcpEvent::Reset);
                }
                
                // Handle FIN
                if segment.flags.fin {
                    self.rcv_nxt = segment.seq_num.wrapping_add(1);
                    self.state = TcpState::CloseWait;
                    
                    let mut reply = TcpSegment::new(self.local.port(), self.remote.port());
                    reply.seq_num = self.snd_nxt;
                    reply.ack_num = self.rcv_nxt;
                    reply.flags = TcpFlags::ack();
                    reply.window = self.rcv_wnd;
                    
                    return TcpAction::SendAndNotify(
                        reply.build(*self.local.ip(), *self.remote.ip()),
                        TcpEvent::RemoteClosed,
                    );
                }
                
                // Handle data
                if !segment.payload.is_empty() {
                    if segment.seq_num == self.rcv_nxt {
                        self.rx_buffer.extend_from_slice(&segment.payload);
                        self.rcv_nxt = self.rcv_nxt.wrapping_add(segment.payload.len() as u32);
                        
                        let mut reply = TcpSegment::new(self.local.port(), self.remote.port());
                        reply.seq_num = self.snd_nxt;
                        reply.ack_num = self.rcv_nxt;
                        reply.flags = TcpFlags::ack();
                        reply.window = self.rcv_wnd;
                        
                        return TcpAction::SendAndNotify(
                            reply.build(*self.local.ip(), *self.remote.ip()),
                            TcpEvent::DataReceived(segment.payload.len()),
                        );
                    }
                }
                
                // Handle ACK
                if segment.flags.ack {
                    self.snd_una = segment.ack_num;
                    self.snd_wnd = segment.window;
                    
                    // Remove acknowledged data from tx buffer
                    let acked = segment.ack_num.wrapping_sub(self.iss) as usize;
                    if acked <= self.tx_buffer.len() {
                        self.tx_buffer.drain(..acked);
                    }
                }
                
                TcpAction::None
            }
            
            TcpState::FinWait1 => {
                if segment.flags.ack && segment.ack_num == self.snd_nxt {
                    self.snd_una = segment.ack_num;
                    
                    if segment.flags.fin {
                        self.rcv_nxt = segment.seq_num.wrapping_add(1);
                        self.state = TcpState::TimeWait;
                        
                        let mut reply = TcpSegment::new(self.local.port(), self.remote.port());
                        reply.seq_num = self.snd_nxt;
                        reply.ack_num = self.rcv_nxt;
                        reply.flags = TcpFlags::ack();
                        reply.window = self.rcv_wnd;
                        
                        return TcpAction::Send(reply.build(*self.local.ip(), *self.remote.ip()));
                    }
                    
                    self.state = TcpState::FinWait2;
                }
                TcpAction::None
            }
            
            TcpState::FinWait2 => {
                if segment.flags.fin {
                    self.rcv_nxt = segment.seq_num.wrapping_add(1);
                    self.state = TcpState::TimeWait;
                    
                    let mut reply = TcpSegment::new(self.local.port(), self.remote.port());
                    reply.seq_num = self.snd_nxt;
                    reply.ack_num = self.rcv_nxt;
                    reply.flags = TcpFlags::ack();
                    reply.window = self.rcv_wnd;
                    
                    return TcpAction::Send(reply.build(*self.local.ip(), *self.remote.ip()));
                }
                TcpAction::None
            }
            
            TcpState::CloseWait => TcpAction::None,
            
            TcpState::LastAck => {
                if segment.flags.ack {
                    self.state = TcpState::Closed;
                    return TcpAction::Notify(TcpEvent::Closed);
                }
                TcpAction::None
            }
            
            TcpState::TimeWait => TcpAction::None,
            TcpState::Closing => TcpAction::None,
        }
    }
    
    /// Send data
    pub fn send(&mut self, data: &[u8]) -> TcpAction {
        if self.state != TcpState::Established {
            return TcpAction::None;
        }
        
        self.tx_buffer.extend_from_slice(data);
        
        let send_len = data.len().min(self.mss as usize).min(self.snd_wnd as usize);
        
        let mut segment = TcpSegment::new(self.local.port(), self.remote.port());
        segment.seq_num = self.snd_nxt;
        segment.ack_num = self.rcv_nxt;
        segment.flags = TcpFlags::psh_ack();
        segment.window = self.rcv_wnd;
        segment.payload = data[..send_len].to_vec();
        
        self.snd_nxt = self.snd_nxt.wrapping_add(send_len as u32);
        
        TcpAction::Send(segment.build(*self.local.ip(), *self.remote.ip()))
    }
    
    /// Close connection
    pub fn close(&mut self) -> TcpAction {
        match self.state {
            TcpState::Established => {
                self.state = TcpState::FinWait1;
                
                let mut segment = TcpSegment::new(self.local.port(), self.remote.port());
                segment.seq_num = self.snd_nxt;
                segment.ack_num = self.rcv_nxt;
                segment.flags = TcpFlags::fin_ack();
                segment.window = self.rcv_wnd;
                
                self.snd_nxt = self.snd_nxt.wrapping_add(1);
                
                TcpAction::Send(segment.build(*self.local.ip(), *self.remote.ip()))
            }
            TcpState::CloseWait => {
                self.state = TcpState::LastAck;
                
                let mut segment = TcpSegment::new(self.local.port(), self.remote.port());
                segment.seq_num = self.snd_nxt;
                segment.ack_num = self.rcv_nxt;
                segment.flags = TcpFlags::fin_ack();
                segment.window = self.rcv_wnd;
                
                self.snd_nxt = self.snd_nxt.wrapping_add(1);
                
                TcpAction::Send(segment.build(*self.local.ip(), *self.remote.ip()))
            }
            _ => TcpAction::None,
        }
    }
    
    /// Read received data
    pub fn read(&mut self, buf: &mut [u8]) -> usize {
        let len = buf.len().min(self.rx_buffer.len());
        buf[..len].copy_from_slice(&self.rx_buffer[..len]);
        self.rx_buffer.drain(..len);
        len
    }
    
    pub fn state(&self) -> TcpState {
        self.state
    }
    
    pub fn local(&self) -> SocketAddrV4 {
        self.local
    }
    
    pub fn remote(&self) -> SocketAddrV4 {
        self.remote
    }
    
    pub fn rx_available(&self) -> usize {
        self.rx_buffer.len()
    }
}

/// TCP action
#[derive(Debug, Clone)]
pub enum TcpAction {
    None,
    Send(Vec<u8>),
    SendAndNotify(Vec<u8>, TcpEvent),
    Notify(TcpEvent),
    Reset,
}

/// TCP event
#[derive(Debug, Clone, Copy)]
pub enum TcpEvent {
    Connected,
    DataReceived(usize),
    RemoteClosed,
    Closed,
    Reset,
}

/// IPv4 packet builder
pub struct Ipv4Packet {
    pub version: u8,
    pub ihl: u8,
    pub dscp: u8,
    pub ecn: u8,
    pub total_length: u16,
    pub identification: u16,
    pub flags: u8,
    pub fragment_offset: u16,
    pub ttl: u8,
    pub protocol: u8,
    pub checksum: u16,
    pub src_addr: Ipv4Addr,
    pub dst_addr: Ipv4Addr,
    pub payload: Vec<u8>,
}

impl Ipv4Packet {
    pub const PROTO_ICMP: u8 = 1;
    pub const PROTO_TCP: u8 = 6;
    pub const PROTO_UDP: u8 = 17;
    
    pub fn new(src: Ipv4Addr, dst: Ipv4Addr, protocol: u8, payload: Vec<u8>) -> Self {
        Self {
            version: 4,
            ihl: 5,
            dscp: 0,
            ecn: 0,
            total_length: (20 + payload.len()) as u16,
            identification: 0,
            flags: 0x40,  // Don't fragment
            fragment_offset: 0,
            ttl: 64,
            protocol,
            checksum: 0,
            src_addr: src,
            dst_addr: dst,
            payload,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut packet = Vec::with_capacity(20 + self.payload.len());
        
        packet.push((self.version << 4) | self.ihl);
        packet.push((self.dscp << 2) | self.ecn);
        packet.extend_from_slice(&self.total_length.to_be_bytes());
        packet.extend_from_slice(&self.identification.to_be_bytes());
        let flags_frag = ((self.flags as u16) << 13) | self.fragment_offset;
        packet.extend_from_slice(&flags_frag.to_be_bytes());
        packet.push(self.ttl);
        packet.push(self.protocol);
        packet.extend_from_slice(&[0, 0]);  // Checksum placeholder
        packet.extend_from_slice(&self.src_addr.octets());
        packet.extend_from_slice(&self.dst_addr.octets());
        
        // Calculate checksum
        let checksum = Self::calculate_checksum(&packet[..20]);
        packet[10] = (checksum >> 8) as u8;
        packet[11] = (checksum & 0xFF) as u8;
        
        packet.extend_from_slice(&self.payload);
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
        if data.len() < 20 {
            return None;
        }
        
        let version = data[0] >> 4;
        let ihl = data[0] & 0x0F;
        
        if version != 4 || ihl < 5 {
            return None;
        }
        
        let header_len = (ihl as usize) * 4;
        let total_length = u16::from_be_bytes([data[2], data[3]]) as usize;
        
        if data.len() < header_len || data.len() < total_length {
            return None;
        }
        
        Some(Self {
            version,
            ihl,
            dscp: data[1] >> 2,
            ecn: data[1] & 0x03,
            total_length: total_length as u16,
            identification: u16::from_be_bytes([data[4], data[5]]),
            flags: (data[6] >> 5) & 0x07,
            fragment_offset: u16::from_be_bytes([data[6] & 0x1F, data[7]]),
            ttl: data[8],
            protocol: data[9],
            checksum: u16::from_be_bytes([data[10], data[11]]),
            src_addr: Ipv4Addr::new(data[12], data[13], data[14], data[15]),
            dst_addr: Ipv4Addr::new(data[16], data[17], data[18], data[19]),
            payload: data[header_len..total_length].to_vec(),
        })
    }
}
