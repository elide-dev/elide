//! WebSocket Implementation
//!
//! Implements WebSocket protocol (RFC 6455) for real-time
//! bidirectional communication over TCP/TLS.

use std::collections::BTreeMap;

/// WebSocket opcode
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum WsOpcode {
    Continuation = 0x0,
    Text = 0x1,
    Binary = 0x2,
    Close = 0x8,
    Ping = 0x9,
    Pong = 0xA,
}

impl WsOpcode {
    pub fn from_u8(v: u8) -> Option<Self> {
        match v {
            0x0 => Some(Self::Continuation),
            0x1 => Some(Self::Text),
            0x2 => Some(Self::Binary),
            0x8 => Some(Self::Close),
            0x9 => Some(Self::Ping),
            0xA => Some(Self::Pong),
            _ => None,
        }
    }
    
    pub fn is_control(&self) -> bool {
        matches!(self, Self::Close | Self::Ping | Self::Pong)
    }
}

/// WebSocket close code
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum WsCloseCode {
    Normal = 1000,
    GoingAway = 1001,
    ProtocolError = 1002,
    UnsupportedData = 1003,
    NoStatus = 1005,
    Abnormal = 1006,
    InvalidPayload = 1007,
    PolicyViolation = 1008,
    MessageTooBig = 1009,
    MandatoryExtension = 1010,
    InternalError = 1011,
    TlsHandshake = 1015,
}

impl WsCloseCode {
    pub fn from_u16(v: u16) -> Self {
        match v {
            1000 => Self::Normal,
            1001 => Self::GoingAway,
            1002 => Self::ProtocolError,
            1003 => Self::UnsupportedData,
            1005 => Self::NoStatus,
            1006 => Self::Abnormal,
            1007 => Self::InvalidPayload,
            1008 => Self::PolicyViolation,
            1009 => Self::MessageTooBig,
            1010 => Self::MandatoryExtension,
            1011 => Self::InternalError,
            1015 => Self::TlsHandshake,
            _ => Self::ProtocolError,
        }
    }
}

/// WebSocket frame
#[derive(Debug, Clone)]
pub struct WsFrame {
    pub fin: bool,
    pub opcode: WsOpcode,
    pub mask: Option<[u8; 4]>,
    pub payload: Vec<u8>,
}

impl WsFrame {
    pub fn text(data: &str) -> Self {
        Self {
            fin: true,
            opcode: WsOpcode::Text,
            mask: Some(generate_mask()),
            payload: data.as_bytes().to_vec(),
        }
    }
    
    pub fn binary(data: Vec<u8>) -> Self {
        Self {
            fin: true,
            opcode: WsOpcode::Binary,
            mask: Some(generate_mask()),
            payload: data,
        }
    }
    
    pub fn ping(data: Vec<u8>) -> Self {
        Self {
            fin: true,
            opcode: WsOpcode::Ping,
            mask: Some(generate_mask()),
            payload: data,
        }
    }
    
    pub fn pong(data: Vec<u8>) -> Self {
        Self {
            fin: true,
            opcode: WsOpcode::Pong,
            mask: Some(generate_mask()),
            payload: data,
        }
    }
    
    pub fn close(code: WsCloseCode, reason: &str) -> Self {
        let mut payload = Vec::with_capacity(2 + reason.len());
        payload.extend_from_slice(&(code as u16).to_be_bytes());
        payload.extend_from_slice(reason.as_bytes());
        
        Self {
            fin: true,
            opcode: WsOpcode::Close,
            mask: Some(generate_mask()),
            payload,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut frame = Vec::new();
        
        // First byte: FIN + RSV + Opcode
        let first_byte = (if self.fin { 0x80 } else { 0 }) | (self.opcode as u8);
        frame.push(first_byte);
        
        // Second byte: MASK + Payload length
        let mask_bit = if self.mask.is_some() { 0x80 } else { 0 };
        let len = self.payload.len();
        
        if len < 126 {
            frame.push(mask_bit | (len as u8));
        } else if len < 65536 {
            frame.push(mask_bit | 126);
            frame.extend_from_slice(&(len as u16).to_be_bytes());
        } else {
            frame.push(mask_bit | 127);
            frame.extend_from_slice(&(len as u64).to_be_bytes());
        }
        
        // Masking key
        if let Some(mask) = &self.mask {
            frame.extend_from_slice(mask);
            
            // Masked payload
            for (i, byte) in self.payload.iter().enumerate() {
                frame.push(byte ^ mask[i % 4]);
            }
        } else {
            frame.extend_from_slice(&self.payload);
        }
        
        frame
    }
    
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 2 {
            return None;
        }
        
        let fin = (data[0] & 0x80) != 0;
        let opcode = WsOpcode::from_u8(data[0] & 0x0F)?;
        
        let masked = (data[1] & 0x80) != 0;
        let mut payload_len = (data[1] & 0x7F) as usize;
        let mut offset = 2;
        
        if payload_len == 126 {
            if data.len() < 4 {
                return None;
            }
            payload_len = u16::from_be_bytes([data[2], data[3]]) as usize;
            offset = 4;
        } else if payload_len == 127 {
            if data.len() < 10 {
                return None;
            }
            payload_len = u64::from_be_bytes([
                data[2], data[3], data[4], data[5],
                data[6], data[7], data[8], data[9],
            ]) as usize;
            offset = 10;
        }
        
        let mask = if masked {
            if data.len() < offset + 4 {
                return None;
            }
            let mut m = [0u8; 4];
            m.copy_from_slice(&data[offset..offset + 4]);
            offset += 4;
            Some(m)
        } else {
            None
        };
        
        if data.len() < offset + payload_len {
            return None;
        }
        
        let mut payload = data[offset..offset + payload_len].to_vec();
        
        // Unmask if needed
        if let Some(m) = &mask {
            for (i, byte) in payload.iter_mut().enumerate() {
                *byte ^= m[i % 4];
            }
        }
        
        Some((Self { fin, opcode, mask, payload }, offset + payload_len))
    }
    
    pub fn payload_text(&self) -> Option<String> {
        String::from_utf8(self.payload.clone()).ok()
    }
    
    pub fn close_code(&self) -> Option<WsCloseCode> {
        if self.opcode == WsOpcode::Close && self.payload.len() >= 2 {
            let code = u16::from_be_bytes([self.payload[0], self.payload[1]]);
            Some(WsCloseCode::from_u16(code))
        } else {
            None
        }
    }
    
    pub fn close_reason(&self) -> Option<String> {
        if self.opcode == WsOpcode::Close && self.payload.len() > 2 {
            String::from_utf8(self.payload[2..].to_vec()).ok()
        } else {
            None
        }
    }
}

fn generate_mask() -> [u8; 4] {
    // Simple PRNG for mask (in real impl, use secure random)
    static mut SEED: u32 = 0x12345678;
    unsafe {
        SEED = SEED.wrapping_mul(1103515245).wrapping_add(12345);
        SEED.to_be_bytes()
    }
}

/// WebSocket handshake request
pub struct WsHandshake {
    pub host: String,
    pub path: String,
    pub key: String,
    pub protocols: Vec<String>,
    pub headers: BTreeMap<String, String>,
}

impl WsHandshake {
    pub fn new(host: &str, path: &str) -> Self {
        Self {
            host: host.to_string(),
            path: path.to_string(),
            key: generate_ws_key(),
            protocols: Vec::new(),
            headers: BTreeMap::new(),
        }
    }
    
    pub fn protocol(mut self, protocol: &str) -> Self {
        self.protocols.push(protocol.to_string());
        self
    }
    
    pub fn header(mut self, name: &str, value: &str) -> Self {
        self.headers.insert(name.to_string(), value.to_string());
        self
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut request = format!(
            "GET {} HTTP/1.1\r\n\
             Host: {}\r\n\
             Upgrade: websocket\r\n\
             Connection: Upgrade\r\n\
             Sec-WebSocket-Key: {}\r\n\
             Sec-WebSocket-Version: 13\r\n",
            self.path, self.host, self.key
        );
        
        if !self.protocols.is_empty() {
            request.push_str(&format!(
                "Sec-WebSocket-Protocol: {}\r\n",
                self.protocols.join(", ")
            ));
        }
        
        for (name, value) in &self.headers {
            request.push_str(&format!("{}: {}\r\n", name, value));
        }
        
        request.push_str("\r\n");
        request.into_bytes()
    }
    
    pub fn expected_accept(&self) -> String {
        compute_accept_key(&self.key)
    }
}

fn generate_ws_key() -> String {
    // Generate 16 random bytes and base64 encode
    let mut bytes = [0u8; 16];
    static mut SEED: u64 = 0xDEADBEEF12345678;
    unsafe {
        for byte in &mut bytes {
            SEED = SEED.wrapping_mul(6364136223846793005).wrapping_add(1);
            *byte = (SEED >> 56) as u8;
        }
    }
    base64_encode(&bytes)
}

fn compute_accept_key(key: &str) -> String {
    // Sec-WebSocket-Accept = base64(SHA-1(key + GUID))
    let magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    let combined = format!("{}{}", key, magic);
    let hash = sha1(combined.as_bytes());
    base64_encode(&hash)
}

fn sha1(data: &[u8]) -> [u8; 20] {
    // Minimal SHA-1 implementation
    let mut h: [u32; 5] = [
        0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0
    ];
    
    // Pad message
    let mut msg = data.to_vec();
    let orig_len = msg.len();
    msg.push(0x80);
    while (msg.len() % 64) != 56 {
        msg.push(0);
    }
    msg.extend_from_slice(&((orig_len as u64 * 8).to_be_bytes()));
    
    // Process blocks
    for chunk in msg.chunks(64) {
        let mut w = [0u32; 80];
        for (i, bytes) in chunk.chunks(4).enumerate() {
            w[i] = u32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
        }
        for i in 16..80 {
            w[i] = (w[i-3] ^ w[i-8] ^ w[i-14] ^ w[i-16]).rotate_left(1);
        }
        
        let (mut a, mut b, mut c, mut d, mut e) = (h[0], h[1], h[2], h[3], h[4]);
        
        for i in 0..80 {
            let (f, k) = match i {
                0..=19 => ((b & c) | ((!b) & d), 0x5A827999u32),
                20..=39 => (b ^ c ^ d, 0x6ED9EBA1u32),
                40..=59 => ((b & c) | (b & d) | (c & d), 0x8F1BBCDCu32),
                _ => (b ^ c ^ d, 0xCA62C1D6u32),
            };
            
            let temp = a.rotate_left(5)
                .wrapping_add(f)
                .wrapping_add(e)
                .wrapping_add(k)
                .wrapping_add(w[i]);
            e = d;
            d = c;
            c = b.rotate_left(30);
            b = a;
            a = temp;
        }
        
        h[0] = h[0].wrapping_add(a);
        h[1] = h[1].wrapping_add(b);
        h[2] = h[2].wrapping_add(c);
        h[3] = h[3].wrapping_add(d);
        h[4] = h[4].wrapping_add(e);
    }
    
    let mut result = [0u8; 20];
    for (i, &val) in h.iter().enumerate() {
        result[i*4..i*4+4].copy_from_slice(&val.to_be_bytes());
    }
    result
}

fn base64_encode(data: &[u8]) -> String {
    const CHARS: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut result = String::new();
    
    for chunk in data.chunks(3) {
        let n = match chunk.len() {
            1 => ((chunk[0] as u32) << 16, 2),
            2 => (((chunk[0] as u32) << 16) | ((chunk[1] as u32) << 8), 3),
            _ => (((chunk[0] as u32) << 16) | ((chunk[1] as u32) << 8) | (chunk[2] as u32), 4),
        };
        
        for i in 0..n.1 {
            let idx = ((n.0 >> (18 - i * 6)) & 0x3F) as usize;
            result.push(CHARS[idx] as char);
        }
        
        for _ in n.1..4 {
            result.push('=');
        }
    }
    
    result
}

/// WebSocket connection state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum WsState {
    #[default]
    Connecting,
    Handshaking,
    Open,
    Closing,
    Closed,
    Error,
}

/// WebSocket message (possibly fragmented)
#[derive(Debug, Clone)]
pub struct WsMessage {
    pub opcode: WsOpcode,
    pub data: Vec<u8>,
}

impl WsMessage {
    pub fn text(&self) -> Option<String> {
        if self.opcode == WsOpcode::Text {
            String::from_utf8(self.data.clone()).ok()
        } else {
            None
        }
    }
    
    pub fn is_text(&self) -> bool {
        self.opcode == WsOpcode::Text
    }
    
    pub fn is_binary(&self) -> bool {
        self.opcode == WsOpcode::Binary
    }
}

/// WebSocket client
pub struct WebSocket {
    state: WsState,
    host: String,
    path: String,
    handshake: Option<WsHandshake>,
    fragment_buffer: Vec<u8>,
    fragment_opcode: Option<WsOpcode>,
    receive_buffer: Vec<u8>,
    max_message_size: usize,
}

impl WebSocket {
    pub fn new(host: &str, path: &str) -> Self {
        Self {
            state: WsState::Connecting,
            host: host.to_string(),
            path: path.to_string(),
            handshake: None,
            fragment_buffer: Vec::new(),
            fragment_opcode: None,
            receive_buffer: Vec::new(),
            max_message_size: 16 * 1024 * 1024, // 16 MB
        }
    }
    
    pub fn max_message_size(mut self, size: usize) -> Self {
        self.max_message_size = size;
        self
    }
    
    /// Start connection (returns data to send for handshake)
    pub fn connect(&mut self) -> WsAction {
        let handshake = WsHandshake::new(&self.host, &self.path);
        let data = handshake.build();
        self.handshake = Some(handshake);
        self.state = WsState::Handshaking;
        
        WsAction::Send(data)
    }
    
    /// Process received data
    pub fn on_data(&mut self, data: &[u8]) -> Vec<WsAction> {
        self.receive_buffer.extend_from_slice(data);
        let mut actions = Vec::new();
        
        match self.state {
            WsState::Handshaking => {
                if let Some(action) = self.process_handshake_response() {
                    actions.push(action);
                }
            }
            WsState::Open | WsState::Closing => {
                loop {
                    let action = self.process_frame();
                    if matches!(action, WsAction::None) {
                        break;
                    }
                    actions.push(action);
                }
            }
            _ => {}
        }
        
        actions
    }
    
    fn process_handshake_response(&mut self) -> Option<WsAction> {
        let text = core::str::from_utf8(&self.receive_buffer).ok()?;
        let end = text.find("\r\n\r\n")?;
        
        // Parse response
        let response = &text[..end];
        let mut lines = response.lines();
        
        // Status line
        let status = lines.next()?;
        if !status.contains("101") {
            self.state = WsState::Error;
            return Some(WsAction::Error(WsError::HandshakeFailed));
        }
        
        // Parse headers
        let mut accept_key = None;
        for line in lines {
            if let Some(colon) = line.find(':') {
                let name = line[..colon].trim().to_lowercase();
                let value = line[colon + 1..].trim();
                
                if name == "sec-websocket-accept" {
                    accept_key = Some(value.to_string());
                }
            }
        }
        
        // Verify accept key
        if let (Some(handshake), Some(accept)) = (&self.handshake, accept_key) {
            if accept != handshake.expected_accept() {
                self.state = WsState::Error;
                return Some(WsAction::Error(WsError::InvalidAcceptKey));
            }
        } else {
            self.state = WsState::Error;
            return Some(WsAction::Error(WsError::MissingAcceptKey));
        }
        
        // Clear handshake data from buffer
        self.receive_buffer.drain(..end + 4);
        self.state = WsState::Open;
        
        Some(WsAction::Connected)
    }
    
    fn process_frame(&mut self) -> WsAction {
        let (frame, consumed) = match WsFrame::parse(&self.receive_buffer) {
            Some(f) => f,
            None => return WsAction::None,
        };
        
        self.receive_buffer.drain(..consumed);
        
        match frame.opcode {
            WsOpcode::Text | WsOpcode::Binary => {
                if frame.fin {
                    if self.fragment_buffer.is_empty() {
                        // Complete single-frame message
                        WsAction::Message(WsMessage {
                            opcode: frame.opcode,
                            data: frame.payload,
                        })
                    } else {
                        // Final fragment
                        self.fragment_buffer.extend_from_slice(&frame.payload);
                        let opcode = self.fragment_opcode.take().unwrap_or(frame.opcode);
                        let data = core::mem::take(&mut self.fragment_buffer);
                        
                        WsAction::Message(WsMessage { opcode, data })
                    }
                } else {
                    // Start fragmented message
                    self.fragment_opcode = Some(frame.opcode);
                    self.fragment_buffer = frame.payload;
                    WsAction::None
                }
            }
            WsOpcode::Continuation => {
                if self.fragment_buffer.len() + frame.payload.len() > self.max_message_size {
                    self.state = WsState::Error;
                    return WsAction::Error(WsError::MessageTooLarge);
                }
                
                self.fragment_buffer.extend_from_slice(&frame.payload);
                
                if frame.fin {
                    let opcode = self.fragment_opcode.take().unwrap_or(WsOpcode::Binary);
                    let data = core::mem::take(&mut self.fragment_buffer);
                    WsAction::Message(WsMessage { opcode, data })
                } else {
                    WsAction::None
                }
            }
            WsOpcode::Ping => {
                // Respond with pong
                WsAction::Send(WsFrame::pong(frame.payload).build())
            }
            WsOpcode::Pong => {
                WsAction::Pong(frame.payload)
            }
            WsOpcode::Close => {
                let code = frame.close_code().unwrap_or(WsCloseCode::Normal);
                let reason = frame.close_reason().unwrap_or_default();
                
                if self.state == WsState::Closing {
                    // We initiated close, now complete
                    self.state = WsState::Closed;
                } else {
                    // Server initiated close, respond and close
                    self.state = WsState::Closed;
                }
                
                WsAction::Closed { code, reason }
            }
        }
    }
    
    /// Send text message
    pub fn send_text(&self, text: &str) -> Option<Vec<u8>> {
        if self.state != WsState::Open {
            return None;
        }
        Some(WsFrame::text(text).build())
    }
    
    /// Send binary message
    pub fn send_binary(&self, data: Vec<u8>) -> Option<Vec<u8>> {
        if self.state != WsState::Open {
            return None;
        }
        Some(WsFrame::binary(data).build())
    }
    
    /// Send ping
    pub fn ping(&self, data: Vec<u8>) -> Option<Vec<u8>> {
        if self.state != WsState::Open {
            return None;
        }
        Some(WsFrame::ping(data).build())
    }
    
    /// Close connection
    pub fn close(&mut self, code: WsCloseCode, reason: &str) -> Option<Vec<u8>> {
        if self.state != WsState::Open {
            return None;
        }
        self.state = WsState::Closing;
        Some(WsFrame::close(code, reason).build())
    }
    
    pub fn state(&self) -> WsState {
        self.state
    }
    
    pub fn is_open(&self) -> bool {
        self.state == WsState::Open
    }
}

/// WebSocket action
#[derive(Debug)]
pub enum WsAction {
    None,
    Send(Vec<u8>),
    Connected,
    Message(WsMessage),
    Pong(Vec<u8>),
    Closed { code: WsCloseCode, reason: String },
    Error(WsError),
}

/// WebSocket error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WsError {
    HandshakeFailed,
    InvalidAcceptKey,
    MissingAcceptKey,
    MessageTooLarge,
    InvalidFrame,
    ConnectionClosed,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ws_frame_text_build_parse() {
        let frame = WsFrame::text("Hello, WebSocket!");
        let built = frame.build();
        
        // Verify frame structure
        assert!(built.len() > 2);
        assert_eq!(built[0] & 0x0F, WsOpcode::Text as u8); // Opcode
        assert!(built[0] & 0x80 != 0); // FIN bit set
        
        // Parse and verify roundtrip (without mask for server frames)
        let mut unmasked = WsFrame {
            fin: true,
            opcode: WsOpcode::Text,
            mask: None,
            payload: "Hello, WebSocket!".as_bytes().to_vec(),
        };
        let server_frame = unmasked.build();
        let (parsed, _) = WsFrame::parse(&server_frame).unwrap();
        assert_eq!(parsed.payload_text().unwrap(), "Hello, WebSocket!");
    }

    #[test]
    fn test_ws_frame_binary() {
        let data = vec![0x01, 0x02, 0x03, 0xFF, 0xFE];
        let frame = WsFrame::binary(data.clone());
        assert_eq!(frame.opcode, WsOpcode::Binary);
        assert_eq!(frame.payload, data);
        assert!(frame.fin);
    }

    #[test]
    fn test_ws_frame_close_code() {
        let frame = WsFrame::close(WsCloseCode::Normal, "goodbye");
        assert_eq!(frame.opcode, WsOpcode::Close);
        assert_eq!(frame.close_code(), Some(WsCloseCode::Normal));
        assert_eq!(frame.close_reason(), Some("goodbye".to_string()));
    }

    #[test]
    fn test_ws_frame_ping_pong() {
        let ping = WsFrame::ping(vec![1, 2, 3]);
        assert_eq!(ping.opcode, WsOpcode::Ping);
        
        let pong = WsFrame::pong(vec![1, 2, 3]);
        assert_eq!(pong.opcode, WsOpcode::Pong);
    }

    #[test]
    fn test_ws_opcode_control() {
        assert!(WsOpcode::Close.is_control());
        assert!(WsOpcode::Ping.is_control());
        assert!(WsOpcode::Pong.is_control());
        assert!(!WsOpcode::Text.is_control());
        assert!(!WsOpcode::Binary.is_control());
    }

    #[test]
    fn test_sha1_known_vector() {
        // Test vector: SHA-1("abc") = a9993e36...
        let hash = sha1(b"abc");
        assert_eq!(hash[0], 0xa9);
        assert_eq!(hash[1], 0x99);
        assert_eq!(hash[2], 0x3e);
        assert_eq!(hash[3], 0x36);
    }

    #[test]
    fn test_base64_encode() {
        assert_eq!(base64_encode(b""), "");
        assert_eq!(base64_encode(b"f"), "Zg==");
        assert_eq!(base64_encode(b"fo"), "Zm8=");
        assert_eq!(base64_encode(b"foo"), "Zm9v");
        assert_eq!(base64_encode(b"foob"), "Zm9vYg==");
        assert_eq!(base64_encode(b"fooba"), "Zm9vYmE=");
        assert_eq!(base64_encode(b"foobar"), "Zm9vYmFy");
    }

    #[test]
    fn test_ws_accept_key() {
        // RFC 6455 example
        let key = "dGhlIHNhbXBsZSBub25jZQ==";
        let accept = compute_accept_key(key);
        assert_eq!(accept, "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=");
    }

    #[test]
    fn test_ws_handshake_build() {
        let handshake = WsHandshake::new("example.com", "/chat");
        let request = handshake.build();
        let text = String::from_utf8(request).unwrap();
        
        assert!(text.starts_with("GET /chat HTTP/1.1\r\n"));
        assert!(text.contains("Host: example.com\r\n"));
        assert!(text.contains("Upgrade: websocket\r\n"));
        assert!(text.contains("Connection: Upgrade\r\n"));
        assert!(text.contains("Sec-WebSocket-Key:"));
        assert!(text.contains("Sec-WebSocket-Version: 13\r\n"));
        assert!(text.ends_with("\r\n\r\n"));
    }

    #[test]
    fn test_websocket_state_machine() {
        let mut ws = WebSocket::new("example.com", "/ws");
        assert_eq!(ws.state(), WsState::Connecting);
        
        let action = ws.connect();
        assert!(matches!(action, WsAction::Send(_)));
        assert_eq!(ws.state(), WsState::Handshaking);
    }

    #[test]
    fn test_ws_frame_length_encoding() {
        // Small payload (< 126 bytes)
        let small = WsFrame { fin: true, opcode: WsOpcode::Text, mask: None, payload: vec![0; 10] };
        let built = small.build();
        assert_eq!(built[1] & 0x7F, 10);
        
        // Medium payload (126-65535 bytes)
        let medium = WsFrame { fin: true, opcode: WsOpcode::Binary, mask: None, payload: vec![0; 1000] };
        let built = medium.build();
        assert_eq!(built[1] & 0x7F, 126);
        let len = u16::from_be_bytes([built[2], built[3]]);
        assert_eq!(len, 1000);
    }

    #[test]
    fn test_ws_masking() {
        let mask = [0x12, 0x34, 0x56, 0x78];
        let payload = vec![0x48, 0x65, 0x6c, 0x6c, 0x6f]; // "Hello"
        
        let frame = WsFrame {
            fin: true,
            opcode: WsOpcode::Text,
            mask: Some(mask),
            payload: payload.clone(),
        };
        
        let built = frame.build();
        
        // Verify mask is in frame
        assert!(built[1] & 0x80 != 0); // Mask bit set
        
        // Parse should unmask correctly
        let (parsed, _) = WsFrame::parse(&built).unwrap();
        assert_eq!(parsed.payload, payload);
    }
}
