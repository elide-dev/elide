//! TLS Foundation
//!
//! Implements TLS 1.2/1.3 record protocol, handshake messages,
//! and cryptographic primitives for secure connections.

/// TLS content type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum ContentType {
    ChangeCipherSpec = 20,
    Alert = 21,
    Handshake = 22,
    ApplicationData = 23,
}

/// TLS version
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct TlsVersion {
    pub major: u8,
    pub minor: u8,
}

impl TlsVersion {
    pub const TLS_1_0: Self = Self { major: 3, minor: 1 };
    pub const TLS_1_1: Self = Self { major: 3, minor: 2 };
    pub const TLS_1_2: Self = Self { major: 3, minor: 3 };
    pub const TLS_1_3: Self = Self { major: 3, minor: 3 };  // Uses extensions
}

/// TLS record
#[derive(Debug, Clone)]
pub struct TlsRecord {
    pub content_type: ContentType,
    pub version: TlsVersion,
    pub fragment: Vec<u8>,
}

impl TlsRecord {
    pub fn new(content_type: ContentType, version: TlsVersion, fragment: Vec<u8>) -> Self {
        Self { content_type, version, fragment }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut record = Vec::with_capacity(5 + self.fragment.len());
        
        record.push(self.content_type as u8);
        record.push(self.version.major);
        record.push(self.version.minor);
        record.extend_from_slice(&(self.fragment.len() as u16).to_be_bytes());
        record.extend_from_slice(&self.fragment);
        
        record
    }
    
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 5 {
            return None;
        }
        
        let content_type = match data[0] {
            20 => ContentType::ChangeCipherSpec,
            21 => ContentType::Alert,
            22 => ContentType::Handshake,
            23 => ContentType::ApplicationData,
            _ => return None,
        };
        
        let version = TlsVersion {
            major: data[1],
            minor: data[2],
        };
        
        let length = u16::from_be_bytes([data[3], data[4]]) as usize;
        
        if data.len() < 5 + length {
            return None;
        }
        
        let fragment = data[5..5 + length].to_vec();
        
        Some((Self { content_type, version, fragment }, 5 + length))
    }
}

/// Handshake message type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum HandshakeType {
    ClientHello = 1,
    ServerHello = 2,
    NewSessionTicket = 4,
    EndOfEarlyData = 5,
    EncryptedExtensions = 8,
    Certificate = 11,
    ServerKeyExchange = 12,
    CertificateRequest = 13,
    ServerHelloDone = 14,
    CertificateVerify = 15,
    ClientKeyExchange = 16,
    Finished = 20,
    KeyUpdate = 24,
    MessageHash = 254,
}

/// Handshake message
#[derive(Debug, Clone)]
pub struct HandshakeMessage {
    pub msg_type: HandshakeType,
    pub body: Vec<u8>,
}

impl HandshakeMessage {
    pub fn new(msg_type: HandshakeType, body: Vec<u8>) -> Self {
        Self { msg_type, body }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut msg = Vec::with_capacity(4 + self.body.len());
        
        msg.push(self.msg_type as u8);
        let len = self.body.len() as u32;
        msg.push((len >> 16) as u8);
        msg.push((len >> 8) as u8);
        msg.push(len as u8);
        msg.extend_from_slice(&self.body);
        
        msg
    }
    
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 4 {
            return None;
        }
        
        let msg_type = match data[0] {
            1 => HandshakeType::ClientHello,
            2 => HandshakeType::ServerHello,
            4 => HandshakeType::NewSessionTicket,
            5 => HandshakeType::EndOfEarlyData,
            8 => HandshakeType::EncryptedExtensions,
            11 => HandshakeType::Certificate,
            12 => HandshakeType::ServerKeyExchange,
            13 => HandshakeType::CertificateRequest,
            14 => HandshakeType::ServerHelloDone,
            15 => HandshakeType::CertificateVerify,
            16 => HandshakeType::ClientKeyExchange,
            20 => HandshakeType::Finished,
            24 => HandshakeType::KeyUpdate,
            _ => return None,
        };
        
        let length = ((data[1] as usize) << 16) | ((data[2] as usize) << 8) | (data[3] as usize);
        
        if data.len() < 4 + length {
            return None;
        }
        
        let body = data[4..4 + length].to_vec();
        
        Some((Self { msg_type, body }, 4 + length))
    }
}

/// TLS cipher suite
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum CipherSuite {
    // TLS 1.2
    TlsRsaWithAes128GcmSha256 = 0x009C,
    TlsRsaWithAes256GcmSha384 = 0x009D,
    TlsEcdheRsaWithAes128GcmSha256 = 0xC02F,
    TlsEcdheRsaWithAes256GcmSha384 = 0xC030,
    TlsEcdheEcdsaWithAes128GcmSha256 = 0xC02B,
    TlsEcdheEcdsaWithAes256GcmSha384 = 0xC02C,
    
    // TLS 1.3
    Tls13Aes128GcmSha256 = 0x1301,
    Tls13Aes256GcmSha384 = 0x1302,
    Tls13Chacha20Poly1305Sha256 = 0x1303,
}

impl CipherSuite {
    pub fn from_u16(v: u16) -> Option<Self> {
        match v {
            0x009C => Some(Self::TlsRsaWithAes128GcmSha256),
            0x009D => Some(Self::TlsRsaWithAes256GcmSha384),
            0xC02F => Some(Self::TlsEcdheRsaWithAes128GcmSha256),
            0xC030 => Some(Self::TlsEcdheRsaWithAes256GcmSha384),
            0xC02B => Some(Self::TlsEcdheEcdsaWithAes128GcmSha256),
            0xC02C => Some(Self::TlsEcdheEcdsaWithAes256GcmSha384),
            0x1301 => Some(Self::Tls13Aes128GcmSha256),
            0x1302 => Some(Self::Tls13Aes256GcmSha384),
            0x1303 => Some(Self::Tls13Chacha20Poly1305Sha256),
            _ => None,
        }
    }
    
    pub fn is_tls13(&self) -> bool {
        matches!(self, 
            Self::Tls13Aes128GcmSha256 | 
            Self::Tls13Aes256GcmSha384 | 
            Self::Tls13Chacha20Poly1305Sha256)
    }
}

/// TLS extension type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u16)]
pub enum ExtensionType {
    ServerName = 0,
    MaxFragmentLength = 1,
    StatusRequest = 5,
    SupportedGroups = 10,
    SignatureAlgorithms = 13,
    UseSrtp = 14,
    Heartbeat = 15,
    ApplicationLayerProtocolNegotiation = 16,
    SignedCertificateTimestamp = 18,
    ClientCertificateType = 19,
    ServerCertificateType = 20,
    Padding = 21,
    PreSharedKey = 41,
    EarlyData = 42,
    SupportedVersions = 43,
    Cookie = 44,
    PskKeyExchangeModes = 45,
    CertificateAuthorities = 47,
    OidFilters = 48,
    PostHandshakeAuth = 49,
    SignatureAlgorithmsCert = 50,
    KeyShare = 51,
}

/// TLS extension
#[derive(Debug, Clone)]
pub struct Extension {
    pub ext_type: u16,
    pub data: Vec<u8>,
}

impl Extension {
    pub fn server_name(hostname: &str) -> Self {
        let name_bytes = hostname.as_bytes();
        let mut data = Vec::with_capacity(5 + name_bytes.len());
        
        // Server Name List length
        let list_len = 3 + name_bytes.len();
        data.extend_from_slice(&(list_len as u16).to_be_bytes());
        
        // Name type (host_name = 0)
        data.push(0);
        
        // Name length
        data.extend_from_slice(&(name_bytes.len() as u16).to_be_bytes());
        data.extend_from_slice(name_bytes);
        
        Self {
            ext_type: ExtensionType::ServerName as u16,
            data,
        }
    }
    
    pub fn supported_versions_client() -> Self {
        let mut data = Vec::new();
        
        // Length of versions list
        data.push(4);  // 2 versions * 2 bytes each
        
        // TLS 1.3
        data.push(0x03);
        data.push(0x04);
        
        // TLS 1.2
        data.push(0x03);
        data.push(0x03);
        
        Self {
            ext_type: ExtensionType::SupportedVersions as u16,
            data,
        }
    }
    
    pub fn supported_groups() -> Self {
        let mut data = Vec::new();
        
        // Groups list length
        data.extend_from_slice(&6u16.to_be_bytes());
        
        // x25519
        data.extend_from_slice(&0x001Du16.to_be_bytes());
        // secp256r1
        data.extend_from_slice(&0x0017u16.to_be_bytes());
        // secp384r1
        data.extend_from_slice(&0x0018u16.to_be_bytes());
        
        Self {
            ext_type: ExtensionType::SupportedGroups as u16,
            data,
        }
    }
    
    pub fn signature_algorithms() -> Self {
        let mut data = Vec::new();
        
        // Algorithms list length
        data.extend_from_slice(&8u16.to_be_bytes());
        
        // ecdsa_secp256r1_sha256
        data.extend_from_slice(&0x0403u16.to_be_bytes());
        // rsa_pss_rsae_sha256
        data.extend_from_slice(&0x0804u16.to_be_bytes());
        // rsa_pkcs1_sha256
        data.extend_from_slice(&0x0401u16.to_be_bytes());
        // ecdsa_secp384r1_sha384
        data.extend_from_slice(&0x0503u16.to_be_bytes());
        
        Self {
            ext_type: ExtensionType::SignatureAlgorithms as u16,
            data,
        }
    }
    
    pub fn key_share_client(public_key: &[u8]) -> Self {
        let mut data = Vec::new();
        
        // Client key share list length
        let entry_len = 4 + public_key.len();
        data.extend_from_slice(&(entry_len as u16).to_be_bytes());
        
        // x25519
        data.extend_from_slice(&0x001Du16.to_be_bytes());
        data.extend_from_slice(&(public_key.len() as u16).to_be_bytes());
        data.extend_from_slice(public_key);
        
        Self {
            ext_type: ExtensionType::KeyShare as u16,
            data,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut ext = Vec::with_capacity(4 + self.data.len());
        
        ext.extend_from_slice(&self.ext_type.to_be_bytes());
        ext.extend_from_slice(&(self.data.len() as u16).to_be_bytes());
        ext.extend_from_slice(&self.data);
        
        ext
    }
    
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 4 {
            return None;
        }
        
        let ext_type = u16::from_be_bytes([data[0], data[1]]);
        let length = u16::from_be_bytes([data[2], data[3]]) as usize;
        
        if data.len() < 4 + length {
            return None;
        }
        
        Some((Self {
            ext_type,
            data: data[4..4 + length].to_vec(),
        }, 4 + length))
    }
}

/// Client Hello message
#[derive(Debug, Clone)]
pub struct ClientHello {
    pub version: TlsVersion,
    pub random: [u8; 32],
    pub session_id: Vec<u8>,
    pub cipher_suites: Vec<u16>,
    pub compression_methods: Vec<u8>,
    pub extensions: Vec<Extension>,
}

impl ClientHello {
    pub fn new(random: [u8; 32], hostname: Option<&str>) -> Self {
        let mut extensions = vec![
            Extension::supported_versions_client(),
            Extension::supported_groups(),
            Extension::signature_algorithms(),
        ];
        
        if let Some(name) = hostname {
            extensions.insert(0, Extension::server_name(name));
        }
        
        Self {
            version: TlsVersion::TLS_1_2,  // Legacy version
            random,
            session_id: Vec::new(),
            cipher_suites: vec![
                CipherSuite::Tls13Aes128GcmSha256 as u16,
                CipherSuite::Tls13Aes256GcmSha384 as u16,
                CipherSuite::TlsEcdheRsaWithAes128GcmSha256 as u16,
                CipherSuite::TlsEcdheRsaWithAes256GcmSha384 as u16,
            ],
            compression_methods: vec![0],  // No compression
            extensions,
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut body = Vec::new();
        
        // Version
        body.push(self.version.major);
        body.push(self.version.minor);
        
        // Random
        body.extend_from_slice(&self.random);
        
        // Session ID
        body.push(self.session_id.len() as u8);
        body.extend_from_slice(&self.session_id);
        
        // Cipher suites
        body.extend_from_slice(&((self.cipher_suites.len() * 2) as u16).to_be_bytes());
        for suite in &self.cipher_suites {
            body.extend_from_slice(&suite.to_be_bytes());
        }
        
        // Compression methods
        body.push(self.compression_methods.len() as u8);
        body.extend_from_slice(&self.compression_methods);
        
        // Extensions
        let mut ext_data = Vec::new();
        for ext in &self.extensions {
            ext_data.extend_from_slice(&ext.build());
        }
        body.extend_from_slice(&(ext_data.len() as u16).to_be_bytes());
        body.extend_from_slice(&ext_data);
        
        body
    }
}

/// Server Hello message
#[derive(Debug, Clone)]
pub struct ServerHello {
    pub version: TlsVersion,
    pub random: [u8; 32],
    pub session_id: Vec<u8>,
    pub cipher_suite: u16,
    pub compression_method: u8,
    pub extensions: Vec<Extension>,
}

impl ServerHello {
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 38 {
            return None;
        }
        
        let version = TlsVersion {
            major: data[0],
            minor: data[1],
        };
        
        let mut random = [0u8; 32];
        random.copy_from_slice(&data[2..34]);
        
        let session_id_len = data[34] as usize;
        if data.len() < 35 + session_id_len + 3 {
            return None;
        }
        
        let session_id = data[35..35 + session_id_len].to_vec();
        let offset = 35 + session_id_len;
        
        let cipher_suite = u16::from_be_bytes([data[offset], data[offset + 1]]);
        let compression_method = data[offset + 2];
        
        let mut extensions = Vec::new();
        if data.len() > offset + 3 {
            let ext_len = u16::from_be_bytes([data[offset + 3], data[offset + 4]]) as usize;
            let mut ext_offset = offset + 5;
            while ext_offset < offset + 5 + ext_len {
                if let Some((ext, len)) = Extension::parse(&data[ext_offset..]) {
                    extensions.push(ext);
                    ext_offset += len;
                } else {
                    break;
                }
            }
        }
        
        Some(Self {
            version,
            random,
            session_id,
            cipher_suite,
            compression_method,
            extensions,
        })
    }
}

/// TLS alert level
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum AlertLevel {
    Warning = 1,
    Fatal = 2,
}

/// TLS alert description
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum AlertDescription {
    CloseNotify = 0,
    UnexpectedMessage = 10,
    BadRecordMac = 20,
    RecordOverflow = 22,
    HandshakeFailure = 40,
    BadCertificate = 42,
    UnsupportedCertificate = 43,
    CertificateRevoked = 44,
    CertificateExpired = 45,
    CertificateUnknown = 46,
    IllegalParameter = 47,
    UnknownCa = 48,
    AccessDenied = 49,
    DecodeError = 50,
    DecryptError = 51,
    ProtocolVersion = 70,
    InsufficientSecurity = 71,
    InternalError = 80,
    InappropriateFallback = 86,
    UserCanceled = 90,
    MissingExtension = 109,
    UnsupportedExtension = 110,
    UnrecognizedName = 112,
    BadCertificateStatusResponse = 113,
    UnknownPskIdentity = 115,
    CertificateRequired = 116,
    NoApplicationProtocol = 120,
}

/// TLS alert
#[derive(Debug, Clone)]
pub struct TlsAlert {
    pub level: AlertLevel,
    pub description: AlertDescription,
}

impl TlsAlert {
    pub fn build(&self) -> Vec<u8> {
        vec![self.level as u8, self.description as u8]
    }
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 2 {
            return None;
        }
        
        let level = match data[0] {
            1 => AlertLevel::Warning,
            2 => AlertLevel::Fatal,
            _ => return None,
        };
        
        let description = match data[1] {
            0 => AlertDescription::CloseNotify,
            10 => AlertDescription::UnexpectedMessage,
            20 => AlertDescription::BadRecordMac,
            40 => AlertDescription::HandshakeFailure,
            42 => AlertDescription::BadCertificate,
            80 => AlertDescription::InternalError,
            _ => AlertDescription::InternalError,
        };
        
        Some(Self { level, description })
    }
}

/// TLS connection state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum TlsState {
    #[default]
    Initial,
    ClientHelloSent,
    ServerHelloReceived,
    CertificateReceived,
    KeyExchangeReceived,
    ServerHelloDoneReceived,
    ClientKeyExchangeSent,
    ChangeCipherSpecSent,
    FinishedSent,
    Connected,
    Closing,
    Closed,
    Error,
}

/// TLS session (minimal)
pub struct TlsSession {
    state: TlsState,
    is_client: bool,
    hostname: Option<String>,
    client_random: [u8; 32],
    server_random: [u8; 32],
    cipher_suite: Option<CipherSuite>,
    master_secret: [u8; 48],
    handshake_hash: Vec<u8>,
    is_tls13: bool,
}

impl TlsSession {
    pub fn new_client(hostname: Option<&str>) -> Self {
        let mut client_random = [0u8; 32];
        // In real impl, use secure random
        for (i, b) in client_random.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(17);
        }
        
        Self {
            state: TlsState::Initial,
            is_client: true,
            hostname: hostname.map(|s| s.to_string()),
            client_random,
            server_random: [0; 32],
            cipher_suite: None,
            master_secret: [0; 48],
            handshake_hash: Vec::new(),
            is_tls13: false,
        }
    }
    
    /// Start handshake
    pub fn start(&mut self) -> TlsAction {
        if !self.is_client {
            return TlsAction::None;
        }
        
        let client_hello = ClientHello::new(
            self.client_random,
            self.hostname.as_deref(),
        );
        
        let handshake = HandshakeMessage::new(
            HandshakeType::ClientHello,
            client_hello.build(),
        );
        
        // Add to handshake hash
        let hs_bytes = handshake.build();
        self.handshake_hash.extend_from_slice(&hs_bytes);
        
        let record = TlsRecord::new(
            ContentType::Handshake,
            TlsVersion::TLS_1_0,  // Use 1.0 for ClientHello compatibility
            hs_bytes,
        );
        
        self.state = TlsState::ClientHelloSent;
        TlsAction::Send(record.build())
    }
    
    /// Process incoming TLS data
    pub fn process(&mut self, data: &[u8]) -> Vec<TlsAction> {
        let mut actions = Vec::new();
        let mut offset = 0;
        
        while offset < data.len() {
            let (record, consumed) = match TlsRecord::parse(&data[offset..]) {
                Some(r) => r,
                None => break,
            };
            offset += consumed;
            
            match record.content_type {
                ContentType::Handshake => {
                    actions.extend(self.process_handshake(&record.fragment));
                }
                ContentType::ChangeCipherSpec => {
                    // Server is changing to encrypted
                }
                ContentType::Alert => {
                    if let Some(alert) = TlsAlert::parse(&record.fragment) {
                        if alert.level == AlertLevel::Fatal {
                            self.state = TlsState::Error;
                            actions.push(TlsAction::Error(alert.description));
                        }
                    }
                }
                ContentType::ApplicationData => {
                    if self.state == TlsState::Connected {
                        // Decrypt and return data
                        actions.push(TlsAction::Data(record.fragment));
                    }
                }
            }
        }
        
        actions
    }
    
    fn process_handshake(&mut self, data: &[u8]) -> Vec<TlsAction> {
        let mut actions = Vec::new();
        let mut offset = 0;
        
        while offset < data.len() {
            let (msg, consumed) = match HandshakeMessage::parse(&data[offset..]) {
                Some(m) => m,
                None => break,
            };
            offset += consumed;
            
            // Add to handshake hash
            self.handshake_hash.extend_from_slice(&data[offset - consumed..offset]);
            
            match msg.msg_type {
                HandshakeType::ServerHello => {
                    if let Some(server_hello) = ServerHello::parse(&msg.body) {
                        self.server_random = server_hello.random;
                        self.cipher_suite = CipherSuite::from_u16(server_hello.cipher_suite);
                        
                        // Check for TLS 1.3 via supported_versions extension
                        for ext in &server_hello.extensions {
                            if ext.ext_type == ExtensionType::SupportedVersions as u16 {
                                if ext.data.len() >= 2 && ext.data[0] == 0x03 && ext.data[1] == 0x04 {
                                    self.is_tls13 = true;
                                }
                            }
                        }
                        
                        self.state = TlsState::ServerHelloReceived;
                    }
                }
                HandshakeType::Certificate => {
                    self.state = TlsState::CertificateReceived;
                    // In real impl, validate certificate chain
                }
                HandshakeType::ServerKeyExchange => {
                    self.state = TlsState::KeyExchangeReceived;
                    // Process ECDHE parameters
                }
                HandshakeType::ServerHelloDone => {
                    self.state = TlsState::ServerHelloDoneReceived;
                    
                    // Send ClientKeyExchange, ChangeCipherSpec, Finished
                    actions.extend(self.send_client_finish());
                }
                HandshakeType::Finished => {
                    if self.state == TlsState::FinishedSent {
                        self.state = TlsState::Connected;
                        actions.push(TlsAction::Connected);
                    }
                }
                _ => {}
            }
        }
        
        actions
    }
    
    fn send_client_finish(&mut self) -> Vec<TlsAction> {
        let mut actions = Vec::new();
        
        // ClientKeyExchange (placeholder - would contain ECDHE public key)
        let key_exchange = HandshakeMessage::new(
            HandshakeType::ClientKeyExchange,
            vec![0; 33],  // Placeholder
        );
        let hs_bytes = key_exchange.build();
        self.handshake_hash.extend_from_slice(&hs_bytes);
        
        let record = TlsRecord::new(
            ContentType::Handshake,
            TlsVersion::TLS_1_2,
            hs_bytes,
        );
        actions.push(TlsAction::Send(record.build()));
        self.state = TlsState::ClientKeyExchangeSent;
        
        // ChangeCipherSpec
        let ccs = TlsRecord::new(
            ContentType::ChangeCipherSpec,
            TlsVersion::TLS_1_2,
            vec![1],
        );
        actions.push(TlsAction::Send(ccs.build()));
        self.state = TlsState::ChangeCipherSpecSent;
        
        // Finished (placeholder - would be encrypted verify_data)
        let finished = HandshakeMessage::new(
            HandshakeType::Finished,
            vec![0; 12],  // Placeholder verify_data
        );
        let record = TlsRecord::new(
            ContentType::Handshake,
            TlsVersion::TLS_1_2,
            finished.build(),
        );
        actions.push(TlsAction::Send(record.build()));
        self.state = TlsState::FinishedSent;
        
        actions
    }
    
    /// Encrypt and send application data
    pub fn send(&mut self, data: &[u8]) -> Option<Vec<u8>> {
        if self.state != TlsState::Connected {
            return None;
        }
        
        // In real impl, encrypt data with negotiated cipher
        let record = TlsRecord::new(
            ContentType::ApplicationData,
            TlsVersion::TLS_1_2,
            data.to_vec(),  // Placeholder - would be encrypted
        );
        
        Some(record.build())
    }
    
    /// Send close_notify alert
    pub fn close(&mut self) -> Vec<u8> {
        self.state = TlsState::Closing;
        
        let alert = TlsAlert {
            level: AlertLevel::Warning,
            description: AlertDescription::CloseNotify,
        };
        
        let record = TlsRecord::new(
            ContentType::Alert,
            TlsVersion::TLS_1_2,
            alert.build(),
        );
        
        record.build()
    }
    
    pub fn state(&self) -> TlsState {
        self.state
    }
    
    pub fn is_connected(&self) -> bool {
        self.state == TlsState::Connected
    }
}

/// TLS action
#[derive(Debug)]
pub enum TlsAction {
    None,
    Send(Vec<u8>),
    Data(Vec<u8>),
    Connected,
    Error(AlertDescription),
}
