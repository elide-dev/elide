//! 802.1X/EAP Enterprise Authentication
//!
//! Implements EAP (Extensible Authentication Protocol) for
//! WPA2-Enterprise and WPA3-Enterprise networks.

/// EAP state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum EapState {
    #[default]
    Idle,
    Started,
    IdentityRequested,
    IdentitySent,
    MethodNegotiation,
    MethodRunning,
    Success,
    Failure,
}

/// EAP method type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum EapMethod {
    Identity = 1,
    Notification = 2,
    Nak = 3,
    Md5Challenge = 4,
    Otp = 5,
    Gtc = 6,
    Tls = 13,
    Leap = 17,
    Sim = 18,
    Ttls = 21,
    Aka = 23,
    Peap = 25,
    MsChapV2 = 26,
    Pwd = 52,
    Fast = 43,
    AkaPrime = 50,
}

/// EAP code
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum EapCode {
    Request = 1,
    Response = 2,
    Success = 3,
    Failure = 4,
}

/// EAP packet
#[derive(Debug, Clone)]
pub struct EapPacket {
    pub code: EapCode,
    pub identifier: u8,
    pub method: Option<EapMethod>,
    pub data: Vec<u8>,
}

impl EapPacket {
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 4 {
            return None;
        }
        
        let code = match data[0] {
            1 => EapCode::Request,
            2 => EapCode::Response,
            3 => EapCode::Success,
            4 => EapCode::Failure,
            _ => return None,
        };
        
        let identifier = data[1];
        let length = u16::from_be_bytes([data[2], data[3]]) as usize;
        
        if data.len() < length {
            return None;
        }
        
        let (method, data_start) = if length > 4 && (code == EapCode::Request || code == EapCode::Response) {
            let method = match data[4] {
                1 => Some(EapMethod::Identity),
                2 => Some(EapMethod::Notification),
                3 => Some(EapMethod::Nak),
                4 => Some(EapMethod::Md5Challenge),
                6 => Some(EapMethod::Gtc),
                13 => Some(EapMethod::Tls),
                21 => Some(EapMethod::Ttls),
                25 => Some(EapMethod::Peap),
                26 => Some(EapMethod::MsChapV2),
                52 => Some(EapMethod::Pwd),
                _ => None,
            };
            (method, 5)
        } else {
            (None, 4)
        };
        
        Some(Self {
            code,
            identifier,
            method,
            data: data[data_start..length].to_vec(),
        })
    }
    
    pub fn build(&self) -> Vec<u8> {
        let data_len = self.data.len();
        let has_method = self.method.is_some() && 
            (self.code == EapCode::Request || self.code == EapCode::Response);
        let length = 4 + if has_method { 1 + data_len } else { data_len };
        
        let mut packet = Vec::with_capacity(length);
        packet.push(self.code as u8);
        packet.push(self.identifier);
        packet.extend_from_slice(&(length as u16).to_be_bytes());
        
        if let Some(method) = self.method {
            if has_method {
                packet.push(method as u8);
            }
        }
        
        packet.extend_from_slice(&self.data);
        packet
    }
    
    pub fn identity_response(identifier: u8, identity: &str) -> Self {
        Self {
            code: EapCode::Response,
            identifier,
            method: Some(EapMethod::Identity),
            data: identity.as_bytes().to_vec(),
        }
    }
    
    pub fn nak(identifier: u8, preferred_methods: &[EapMethod]) -> Self {
        Self {
            code: EapCode::Response,
            identifier,
            method: Some(EapMethod::Nak),
            data: preferred_methods.iter().map(|m| *m as u8).collect(),
        }
    }
}

/// EAPOL (EAP over LAN) frame type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum EapolType {
    EapPacket = 0,
    Start = 1,
    Logoff = 2,
    Key = 3,
    EncapsulatedAsfAlert = 4,
    Mka = 5,
    AnnouncementGeneric = 6,
    AnnouncementSpecific = 7,
    AnnouncementReq = 8,
}

/// EAPOL frame
#[derive(Debug, Clone)]
pub struct EapolFrame {
    pub version: u8,
    pub packet_type: EapolType,
    pub body: Vec<u8>,
}

impl EapolFrame {
    pub const ETHERTYPE: u16 = 0x888E;
    
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 4 {
            return None;
        }
        
        let version = data[0];
        let packet_type = match data[1] {
            0 => EapolType::EapPacket,
            1 => EapolType::Start,
            2 => EapolType::Logoff,
            3 => EapolType::Key,
            _ => return None,
        };
        
        let body_len = u16::from_be_bytes([data[2], data[3]]) as usize;
        
        if data.len() < 4 + body_len {
            return None;
        }
        
        Some(Self {
            version,
            packet_type,
            body: data[4..4 + body_len].to_vec(),
        })
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut frame = Vec::with_capacity(4 + self.body.len());
        frame.push(self.version);
        frame.push(self.packet_type as u8);
        frame.extend_from_slice(&(self.body.len() as u16).to_be_bytes());
        frame.extend_from_slice(&self.body);
        frame
    }
    
    pub fn start() -> Self {
        Self {
            version: 2,
            packet_type: EapolType::Start,
            body: Vec::new(),
        }
    }
    
    pub fn logoff() -> Self {
        Self {
            version: 2,
            packet_type: EapolType::Logoff,
            body: Vec::new(),
        }
    }
    
    pub fn eap(eap: EapPacket) -> Self {
        Self {
            version: 2,
            packet_type: EapolType::EapPacket,
            body: eap.build(),
        }
    }
}

/// EAP-TLS state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum TlsState {
    #[default]
    Idle,
    ClientHelloSent,
    ServerHelloReceived,
    ClientKeyExchange,
    ChangeCipherSpec,
    Finished,
}

/// EAP-TLS session
pub struct EapTlsSession {
    state: TlsState,
    client_random: [u8; 32],
    server_random: [u8; 32],
    session_id: Vec<u8>,
    master_secret: [u8; 48],
}

impl EapTlsSession {
    pub fn new() -> Self {
        let mut client_random = [0u8; 32];
        // In real impl, use secure random
        for (i, b) in client_random.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(17);
        }
        
        Self {
            state: TlsState::Idle,
            client_random,
            server_random: [0; 32],
            session_id: Vec::new(),
            master_secret: [0; 48],
        }
    }
    
    /// Build TLS Client Hello
    pub fn build_client_hello(&mut self) -> Vec<u8> {
        self.state = TlsState::ClientHelloSent;
        
        let mut hello = Vec::with_capacity(128);
        
        // Content type: Handshake (22)
        hello.push(22);
        // Version: TLS 1.0 (for compatibility)
        hello.extend_from_slice(&[0x03, 0x01]);
        // Length placeholder
        let len_pos = hello.len();
        hello.extend_from_slice(&[0x00, 0x00]);
        
        // Handshake type: Client Hello (1)
        hello.push(1);
        // Handshake length placeholder
        let hs_len_pos = hello.len();
        hello.extend_from_slice(&[0x00, 0x00, 0x00]);
        
        // Client version: TLS 1.2
        hello.extend_from_slice(&[0x03, 0x03]);
        // Client random
        hello.extend_from_slice(&self.client_random);
        // Session ID length (0)
        hello.push(0);
        
        // Cipher suites
        hello.extend_from_slice(&[0x00, 0x04]);  // Length
        hello.extend_from_slice(&[0x00, 0x2F]);  // TLS_RSA_WITH_AES_128_CBC_SHA
        hello.extend_from_slice(&[0x00, 0xFF]);  // TLS_EMPTY_RENEGOTIATION_INFO_SCSV
        
        // Compression methods
        hello.push(1);   // Length
        hello.push(0);   // null compression
        
        // Fix lengths
        let record_len = hello.len() - len_pos - 2;
        hello[len_pos] = ((record_len >> 8) & 0xFF) as u8;
        hello[len_pos + 1] = (record_len & 0xFF) as u8;
        
        let hs_len = hello.len() - hs_len_pos - 3;
        hello[hs_len_pos] = ((hs_len >> 16) & 0xFF) as u8;
        hello[hs_len_pos + 1] = ((hs_len >> 8) & 0xFF) as u8;
        hello[hs_len_pos + 2] = (hs_len & 0xFF) as u8;
        
        hello
    }
    
    /// Process TLS Server Hello
    pub fn process_server_hello(&mut self, data: &[u8]) -> Result<(), EapError> {
        if data.len() < 38 {
            return Err(EapError::InvalidMessage);
        }
        
        // Skip to server random (after handshake header)
        self.server_random.copy_from_slice(&data[6..38]);
        
        // Get session ID
        let session_id_len = data[38] as usize;
        if data.len() < 39 + session_id_len {
            return Err(EapError::InvalidMessage);
        }
        self.session_id = data[39..39 + session_id_len].to_vec();
        
        self.state = TlsState::ServerHelloReceived;
        Ok(())
    }
    
    pub fn state(&self) -> TlsState {
        self.state
    }
}

impl Default for EapTlsSession {
    fn default() -> Self {
        Self::new()
    }
}

/// EAP-PEAP state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum PeapState {
    #[default]
    Idle,
    TlsTunnelSetup,
    TlsTunnelEstablished,
    InnerAuthStarted,
    InnerAuthComplete,
    Success,
}

/// EAP-PEAP session
pub struct EapPeapSession {
    state: PeapState,
    tls_session: EapTlsSession,
    inner_identity: String,
    inner_method: EapMethod,
}

impl EapPeapSession {
    pub fn new(identity: &str) -> Self {
        Self {
            state: PeapState::Idle,
            tls_session: EapTlsSession::new(),
            inner_identity: identity.to_string(),
            inner_method: EapMethod::MsChapV2,
        }
    }
    
    pub fn state(&self) -> PeapState {
        self.state
    }
    
    pub fn start_tunnel(&mut self) -> Vec<u8> {
        self.state = PeapState::TlsTunnelSetup;
        self.tls_session.build_client_hello()
    }
}

/// EAP-TTLS state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum TtlsState {
    #[default]
    Idle,
    TlsTunnelSetup,
    TlsTunnelEstablished,
    InnerAuthPap,
    InnerAuthChap,
    InnerAuthMsChapV2,
    Success,
}

/// EAP-TTLS session
pub struct EapTtlsSession {
    state: TtlsState,
    tls_session: EapTlsSession,
    inner_identity: String,
    inner_password: Vec<u8>,
}

impl EapTtlsSession {
    pub fn new(identity: &str, password: &[u8]) -> Self {
        Self {
            state: TtlsState::Idle,
            tls_session: EapTlsSession::new(),
            inner_identity: identity.to_string(),
            inner_password: password.to_vec(),
        }
    }
    
    pub fn state(&self) -> TtlsState {
        self.state
    }
}

/// Enterprise credentials
#[derive(Debug, Clone)]
pub enum EnterpriseCredentials {
    /// Username/password (for PEAP, TTLS, etc.)
    UsernamePassword {
        identity: String,
        anonymous_identity: Option<String>,
        password: Vec<u8>,
    },
    /// Certificate-based (for EAP-TLS)
    Certificate {
        identity: String,
        client_cert: Vec<u8>,
        client_key: Vec<u8>,
        ca_cert: Option<Vec<u8>>,
    },
    /// SIM-based (for EAP-SIM/AKA)
    Sim {
        imsi: String,
    },
}

/// EAP supplicant
pub struct EapSupplicant {
    state: EapState,
    credentials: EnterpriseCredentials,
    supported_methods: Vec<EapMethod>,
    current_method: Option<EapMethod>,
    identifier: u8,
    tls_session: Option<EapTlsSession>,
    peap_session: Option<EapPeapSession>,
    ttls_session: Option<EapTtlsSession>,
    msk: Option<[u8; 64]>,
}

impl EapSupplicant {
    pub fn new(credentials: EnterpriseCredentials) -> Self {
        let supported_methods = match &credentials {
            EnterpriseCredentials::UsernamePassword { .. } => {
                vec![EapMethod::Peap, EapMethod::Ttls, EapMethod::MsChapV2]
            }
            EnterpriseCredentials::Certificate { .. } => {
                vec![EapMethod::Tls]
            }
            EnterpriseCredentials::Sim { .. } => {
                vec![EapMethod::Sim, EapMethod::Aka]
            }
        };
        
        Self {
            state: EapState::Idle,
            credentials,
            supported_methods,
            current_method: None,
            identifier: 0,
            tls_session: None,
            peap_session: None,
            ttls_session: None,
            msk: None,
        }
    }
    
    /// Start EAP authentication
    pub fn start(&mut self) -> EapolFrame {
        self.state = EapState::Started;
        EapolFrame::start()
    }
    
    /// Process incoming EAP packet
    pub fn process(&mut self, eapol: &EapolFrame) -> Option<EapolFrame> {
        if eapol.packet_type != EapolType::EapPacket {
            return None;
        }
        
        let eap = EapPacket::parse(&eapol.body)?;
        self.identifier = eap.identifier;
        
        match eap.code {
            EapCode::Request => self.handle_request(eap),
            EapCode::Success => {
                self.state = EapState::Success;
                None
            }
            EapCode::Failure => {
                self.state = EapState::Failure;
                None
            }
            _ => None,
        }
    }
    
    fn handle_request(&mut self, eap: EapPacket) -> Option<EapolFrame> {
        match eap.method {
            Some(EapMethod::Identity) => {
                self.state = EapState::IdentitySent;
                let identity = self.get_identity();
                let response = EapPacket::identity_response(eap.identifier, &identity);
                Some(EapolFrame::eap(response))
            }
            Some(method) if self.supported_methods.contains(&method) => {
                self.current_method = Some(method);
                self.state = EapState::MethodRunning;
                self.handle_method_request(method, eap)
            }
            Some(_) => {
                // NAK with preferred methods
                let response = EapPacket::nak(eap.identifier, &self.supported_methods);
                Some(EapolFrame::eap(response))
            }
            None => None,
        }
    }
    
    fn handle_method_request(&mut self, method: EapMethod, eap: EapPacket) -> Option<EapolFrame> {
        match method {
            EapMethod::Peap => self.handle_peap(eap),
            EapMethod::Ttls => self.handle_ttls(eap),
            EapMethod::Tls => self.handle_tls(eap),
            EapMethod::MsChapV2 => self.handle_mschapv2(eap),
            _ => None,
        }
    }
    
    fn handle_peap(&mut self, eap: EapPacket) -> Option<EapolFrame> {
        if self.peap_session.is_none() {
            let identity = self.get_identity();
            self.peap_session = Some(EapPeapSession::new(&identity));
        }
        
        let session = self.peap_session.as_mut()?;
        
        // Check PEAP flags
        let flags = if !eap.data.is_empty() { eap.data[0] } else { 0 };
        let _start = (flags & 0x20) != 0;
        
        match session.state() {
            PeapState::Idle => {
                let tls_data = session.start_tunnel();
                let mut response_data = vec![0x20];  // PEAP flags (length included)
                response_data.extend_from_slice(&(tls_data.len() as u32).to_be_bytes());
                response_data.extend_from_slice(&tls_data);
                
                let response = EapPacket {
                    code: EapCode::Response,
                    identifier: eap.identifier,
                    method: Some(EapMethod::Peap),
                    data: response_data,
                };
                Some(EapolFrame::eap(response))
            }
            _ => {
                // Continue TLS handshake / inner auth
                let response = EapPacket {
                    code: EapCode::Response,
                    identifier: eap.identifier,
                    method: Some(EapMethod::Peap),
                    data: vec![0x00],  // Empty acknowledgment
                };
                Some(EapolFrame::eap(response))
            }
        }
    }
    
    fn handle_ttls(&mut self, eap: EapPacket) -> Option<EapolFrame> {
        if self.ttls_session.is_none() {
            if let EnterpriseCredentials::UsernamePassword { identity, password, .. } = &self.credentials {
                self.ttls_session = Some(EapTtlsSession::new(identity, password));
            }
        }
        
        // Similar to PEAP handling
        let response = EapPacket {
            code: EapCode::Response,
            identifier: eap.identifier,
            method: Some(EapMethod::Ttls),
            data: vec![0x00],
        };
        Some(EapolFrame::eap(response))
    }
    
    fn handle_tls(&mut self, eap: EapPacket) -> Option<EapolFrame> {
        if self.tls_session.is_none() {
            self.tls_session = Some(EapTlsSession::new());
        }
        
        let session = self.tls_session.as_mut()?;
        
        match session.state() {
            TlsState::Idle => {
                let tls_data = session.build_client_hello();
                let mut response_data = vec![0x20];  // TLS flags
                response_data.extend_from_slice(&(tls_data.len() as u32).to_be_bytes());
                response_data.extend_from_slice(&tls_data);
                
                let response = EapPacket {
                    code: EapCode::Response,
                    identifier: eap.identifier,
                    method: Some(EapMethod::Tls),
                    data: response_data,
                };
                Some(EapolFrame::eap(response))
            }
            _ => {
                let response = EapPacket {
                    code: EapCode::Response,
                    identifier: eap.identifier,
                    method: Some(EapMethod::Tls),
                    data: vec![0x00],
                };
                Some(EapolFrame::eap(response))
            }
        }
    }
    
    fn handle_mschapv2(&mut self, eap: EapPacket) -> Option<EapolFrame> {
        // MS-CHAPv2 challenge/response
        let response = EapPacket {
            code: EapCode::Response,
            identifier: eap.identifier,
            method: Some(EapMethod::MsChapV2),
            data: vec![0x02],  // Success response
        };
        Some(EapolFrame::eap(response))
    }
    
    fn get_identity(&self) -> String {
        match &self.credentials {
            EnterpriseCredentials::UsernamePassword { identity, anonymous_identity, .. } => {
                anonymous_identity.clone().unwrap_or_else(|| identity.clone())
            }
            EnterpriseCredentials::Certificate { identity, .. } => identity.clone(),
            EnterpriseCredentials::Sim { imsi } => imsi.clone(),
        }
    }
    
    pub fn state(&self) -> EapState {
        self.state
    }
    
    pub fn is_authenticated(&self) -> bool {
        self.state == EapState::Success
    }
    
    pub fn msk(&self) -> Option<&[u8; 64]> {
        self.msk.as_ref()
    }
}

/// EAP error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum EapError {
    InvalidMessage,
    UnsupportedMethod,
    AuthenticationFailed,
    TlsError,
    Timeout,
}
