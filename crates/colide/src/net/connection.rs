//! WiFi Connection State Machine
//!
//! Unified connection management for WiFi including authentication,
//! association, key exchange, and data path setup.

use crate::net::scan::{ScanResult, SecurityType};

/// Connection state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ConnState {
    #[default]
    Disconnected,
    Scanning,
    Authenticating,
    Associating,
    FourWayHandshake,
    GroupKeyHandshake,
    Connected,
    Disconnecting,
    Roaming,
}

/// Connection event
#[derive(Debug, Clone)]
pub enum ConnEvent {
    /// User requested connection
    Connect(ConnectRequest),
    /// User requested disconnect
    Disconnect,
    /// Scan completed
    ScanComplete(Vec<ScanResult>),
    /// Authentication response received
    AuthResponse(AuthResult),
    /// Association response received
    AssocResponse(AssocResult),
    /// EAPOL frame received
    EapolRx(Vec<u8>),
    /// 4-way handshake complete
    PtkInstalled,
    /// Group key handshake complete
    GtkInstalled,
    /// Deauth/disassoc received
    Deauth(u16),
    /// Beacon lost
    BeaconLoss,
    /// Signal quality changed
    SignalChange(i8),
    /// Roam trigger
    RoamTrigger(ScanResult),
    /// Timeout
    Timeout,
}

/// Connection request
#[derive(Debug, Clone)]
pub struct ConnectRequest {
    pub ssid: Vec<u8>,
    pub bssid: Option<[u8; 6]>,
    pub passphrase: Option<Vec<u8>>,
    pub security: SecurityType,
    pub channel: Option<u8>,
}

impl ConnectRequest {
    pub fn open(ssid: &[u8]) -> Self {
        Self {
            ssid: ssid.to_vec(),
            bssid: None,
            passphrase: None,
            security: SecurityType::Open,
            channel: None,
        }
    }
    
    pub fn wpa2(ssid: &[u8], passphrase: &[u8]) -> Self {
        Self {
            ssid: ssid.to_vec(),
            bssid: None,
            passphrase: Some(passphrase.to_vec()),
            security: SecurityType::Wpa2Psk,
            channel: None,
        }
    }
    
    pub fn with_bssid(mut self, bssid: [u8; 6]) -> Self {
        self.bssid = Some(bssid);
        self
    }
}

/// Authentication result
#[derive(Debug, Clone, Copy)]
pub struct AuthResult {
    pub success: bool,
    pub algorithm: u16,
    pub status: u16,
}

/// Association result
#[derive(Debug, Clone)]
pub struct AssocResult {
    pub success: bool,
    pub status: u16,
    pub aid: u16,
    pub capabilities: u16,
    pub ies: Vec<u8>,
}

/// Connection action (output from state machine)
#[derive(Debug, Clone)]
pub enum ConnAction {
    None,
    StartScan(Vec<u8>),  // SSID
    SendAuth(AuthFrame),
    SendAssoc(AssocFrame),
    SendEapol(Vec<u8>),
    InstallPtk(PtkInfo),
    InstallGtk(GtkInfo),
    SetChannel(u32),
    EnableDataPath,
    DisableDataPath,
    SendDeauth(u16),
    NotifyConnected(ConnectedInfo),
    NotifyDisconnected(u16),
    NotifyFailed(ConnectError),
    ScheduleTimeout(u64),
    CancelTimeout,
}

/// Authentication frame to send
#[derive(Debug, Clone)]
pub struct AuthFrame {
    pub bssid: [u8; 6],
    pub algorithm: u16,
    pub seq_num: u16,
    pub status: u16,
}

impl AuthFrame {
    pub fn open_system(bssid: [u8; 6]) -> Self {
        Self {
            bssid,
            algorithm: 0,  // Open System
            seq_num: 1,
            status: 0,
        }
    }
    
    pub fn build_frame(&self, sa: [u8; 6]) -> Vec<u8> {
        let mut frame = Vec::with_capacity(30);
        // Frame Control: Authentication
        frame.extend_from_slice(&[0xB0, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (BSSID)
        frame.extend_from_slice(&self.bssid);
        // SA
        frame.extend_from_slice(&sa);
        // BSSID
        frame.extend_from_slice(&self.bssid);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        // Auth Algorithm
        frame.extend_from_slice(&self.algorithm.to_le_bytes());
        // Auth Seq Num
        frame.extend_from_slice(&self.seq_num.to_le_bytes());
        // Status
        frame.extend_from_slice(&self.status.to_le_bytes());
        frame
    }
}

/// Association request frame
#[derive(Debug, Clone)]
pub struct AssocFrame {
    pub bssid: [u8; 6],
    pub ssid: Vec<u8>,
    pub capabilities: u16,
    pub listen_interval: u16,
    pub supported_rates: Vec<u8>,
    pub ht_capabilities: Option<Vec<u8>>,
    pub rsn_ie: Option<Vec<u8>>,
}

impl AssocFrame {
    pub fn new(bssid: [u8; 6], ssid: &[u8]) -> Self {
        Self {
            bssid,
            ssid: ssid.to_vec(),
            capabilities: 0x0411,  // ESS, Short Preamble, Short Slot
            listen_interval: 10,
            supported_rates: vec![0x82, 0x84, 0x8B, 0x96, 0x0C, 0x12, 0x18, 0x24],
            ht_capabilities: None,
            rsn_ie: None,
        }
    }
    
    pub fn with_rsn(mut self, rsn_ie: Vec<u8>) -> Self {
        self.rsn_ie = Some(rsn_ie);
        self
    }
    
    pub fn with_ht(mut self, ht_cap: Vec<u8>) -> Self {
        self.ht_capabilities = Some(ht_cap);
        self
    }
    
    pub fn build_frame(&self, sa: [u8; 6]) -> Vec<u8> {
        let mut frame = Vec::with_capacity(256);
        // Frame Control: Association Request
        frame.extend_from_slice(&[0x00, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (BSSID)
        frame.extend_from_slice(&self.bssid);
        // SA
        frame.extend_from_slice(&sa);
        // BSSID
        frame.extend_from_slice(&self.bssid);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        // Capabilities
        frame.extend_from_slice(&self.capabilities.to_le_bytes());
        // Listen Interval
        frame.extend_from_slice(&self.listen_interval.to_le_bytes());
        
        // SSID IE
        frame.push(0);  // Element ID
        frame.push(self.ssid.len() as u8);
        frame.extend_from_slice(&self.ssid);
        
        // Supported Rates IE
        frame.push(1);  // Element ID
        frame.push(self.supported_rates.len() as u8);
        frame.extend_from_slice(&self.supported_rates);
        
        // HT Capabilities IE
        if let Some(ht) = &self.ht_capabilities {
            frame.push(45);  // Element ID
            frame.push(ht.len() as u8);
            frame.extend_from_slice(ht);
        }
        
        // RSN IE
        if let Some(rsn) = &self.rsn_ie {
            frame.extend_from_slice(rsn);
        }
        
        frame
    }
}

/// PTK (Pairwise Transient Key) info
#[derive(Debug, Clone)]
pub struct PtkInfo {
    pub kck: [u8; 16],
    pub kek: [u8; 16],
    pub tk: [u8; 16],
    pub key_id: u8,
}

/// GTK (Group Temporal Key) info
#[derive(Debug, Clone)]
pub struct GtkInfo {
    pub gtk: [u8; 16],
    pub key_id: u8,
    pub tx: bool,
}

/// Connected info
#[derive(Debug, Clone)]
pub struct ConnectedInfo {
    pub bssid: [u8; 6],
    pub ssid: Vec<u8>,
    pub frequency: u32,
    pub signal: i8,
    pub security: SecurityType,
    pub aid: u16,
}

/// Connection error
#[derive(Debug, Clone, Copy)]
pub enum ConnectError {
    ScanFailed,
    NetworkNotFound,
    AuthFailed(u16),
    AssocFailed(u16),
    HandshakeFailed,
    Timeout,
    Aborted,
}

/// Connection state machine
pub struct ConnectionStateMachine {
    state: ConnState,
    request: Option<ConnectRequest>,
    target_bss: Option<ScanResult>,
    our_mac: [u8; 6],
    aid: u16,
    retry_count: u8,
    max_retries: u8,
    auth_timeout_ms: u64,
    assoc_timeout_ms: u64,
    handshake_timeout_ms: u64,
}

impl ConnectionStateMachine {
    pub fn new(mac: [u8; 6]) -> Self {
        Self {
            state: ConnState::Disconnected,
            request: None,
            target_bss: None,
            our_mac: mac,
            aid: 0,
            retry_count: 0,
            max_retries: 3,
            auth_timeout_ms: 500,
            assoc_timeout_ms: 500,
            handshake_timeout_ms: 2000,
        }
    }
    
    pub fn state(&self) -> ConnState {
        self.state
    }
    
    pub fn is_connected(&self) -> bool {
        self.state == ConnState::Connected
    }
    
    pub fn current_bss(&self) -> Option<&ScanResult> {
        if self.is_connected() {
            self.target_bss.as_ref()
        } else {
            None
        }
    }
    
    /// Process an event and return actions to take
    pub fn process(&mut self, event: ConnEvent) -> Vec<ConnAction> {
        let mut actions = Vec::new();
        
        match (&self.state, event) {
            // --- Disconnected State ---
            (ConnState::Disconnected, ConnEvent::Connect(req)) => {
                self.request = Some(req.clone());
                self.retry_count = 0;
                self.state = ConnState::Scanning;
                actions.push(ConnAction::StartScan(req.ssid));
                actions.push(ConnAction::ScheduleTimeout(5000));
            }
            
            // --- Scanning State ---
            (ConnState::Scanning, ConnEvent::ScanComplete(results)) => {
                actions.push(ConnAction::CancelTimeout);
                
                if let Some(req) = &self.request {
                    // Find best matching BSS
                    let target = results.iter()
                        .filter(|r| r.ssid == req.ssid)
                        .filter(|r| req.bssid.map_or(true, |b| r.bssid == b))
                        .max_by_key(|r| r.signal);
                    
                    if let Some(bss) = target {
                        self.target_bss = Some(bss.clone());
                        self.state = ConnState::Authenticating;
                        
                        actions.push(ConnAction::SetChannel(bss.frequency));
                        actions.push(ConnAction::SendAuth(AuthFrame::open_system(bss.bssid)));
                        actions.push(ConnAction::ScheduleTimeout(self.auth_timeout_ms));
                    } else {
                        self.state = ConnState::Disconnected;
                        actions.push(ConnAction::NotifyFailed(ConnectError::NetworkNotFound));
                    }
                }
            }
            
            (ConnState::Scanning, ConnEvent::Timeout) => {
                self.retry_count += 1;
                if self.retry_count < self.max_retries {
                    if let Some(req) = &self.request {
                        actions.push(ConnAction::StartScan(req.ssid.clone()));
                        actions.push(ConnAction::ScheduleTimeout(5000));
                    }
                } else {
                    self.state = ConnState::Disconnected;
                    actions.push(ConnAction::NotifyFailed(ConnectError::ScanFailed));
                }
            }
            
            // --- Authenticating State ---
            (ConnState::Authenticating, ConnEvent::AuthResponse(result)) => {
                actions.push(ConnAction::CancelTimeout);
                
                if result.success {
                    self.state = ConnState::Associating;
                    
                    if let (Some(bss), Some(req)) = (&self.target_bss, &self.request) {
                        let mut assoc = AssocFrame::new(bss.bssid, &req.ssid);
                        
                        // Add RSN IE for WPA2
                        if req.security == SecurityType::Wpa2Psk {
                            assoc = assoc.with_rsn(Self::build_rsn_ie());
                        }
                        
                        actions.push(ConnAction::SendAssoc(assoc));
                        actions.push(ConnAction::ScheduleTimeout(self.assoc_timeout_ms));
                    }
                } else {
                    self.retry_count += 1;
                    if self.retry_count < self.max_retries {
                        if let Some(bss) = &self.target_bss {
                            actions.push(ConnAction::SendAuth(AuthFrame::open_system(bss.bssid)));
                            actions.push(ConnAction::ScheduleTimeout(self.auth_timeout_ms));
                        }
                    } else {
                        self.state = ConnState::Disconnected;
                        actions.push(ConnAction::NotifyFailed(ConnectError::AuthFailed(result.status)));
                    }
                }
            }
            
            (ConnState::Authenticating, ConnEvent::Timeout) => {
                self.retry_count += 1;
                if self.retry_count < self.max_retries {
                    if let Some(bss) = &self.target_bss {
                        actions.push(ConnAction::SendAuth(AuthFrame::open_system(bss.bssid)));
                        actions.push(ConnAction::ScheduleTimeout(self.auth_timeout_ms));
                    }
                } else {
                    self.state = ConnState::Disconnected;
                    actions.push(ConnAction::NotifyFailed(ConnectError::Timeout));
                }
            }
            
            // --- Associating State ---
            (ConnState::Associating, ConnEvent::AssocResponse(result)) => {
                actions.push(ConnAction::CancelTimeout);
                
                if result.success {
                    self.aid = result.aid;
                    
                    if let Some(req) = &self.request {
                        if req.security == SecurityType::Open {
                            // Open network - connected!
                            self.state = ConnState::Connected;
                            actions.push(ConnAction::EnableDataPath);
                            
                            if let Some(bss) = &self.target_bss {
                                actions.push(ConnAction::NotifyConnected(ConnectedInfo {
                                    bssid: bss.bssid,
                                    ssid: bss.ssid.clone(),
                                    frequency: bss.frequency,
                                    signal: bss.signal,
                                    security: SecurityType::Open,
                                    aid: self.aid,
                                }));
                            }
                        } else {
                            // WPA - wait for EAPOL
                            self.state = ConnState::FourWayHandshake;
                            self.retry_count = 0;
                            actions.push(ConnAction::ScheduleTimeout(self.handshake_timeout_ms));
                        }
                    }
                } else {
                    self.retry_count += 1;
                    if self.retry_count < self.max_retries {
                        if let (Some(bss), Some(req)) = (&self.target_bss, &self.request) {
                            let assoc = AssocFrame::new(bss.bssid, &req.ssid);
                            actions.push(ConnAction::SendAssoc(assoc));
                            actions.push(ConnAction::ScheduleTimeout(self.assoc_timeout_ms));
                        }
                    } else {
                        self.state = ConnState::Disconnected;
                        actions.push(ConnAction::NotifyFailed(ConnectError::AssocFailed(result.status)));
                    }
                }
            }
            
            // --- Four-Way Handshake State ---
            (ConnState::FourWayHandshake, ConnEvent::EapolRx(data)) => {
                // EAPOL processing handled externally, this just tracks state
                actions.push(ConnAction::CancelTimeout);
                actions.push(ConnAction::ScheduleTimeout(self.handshake_timeout_ms));
                // External handler will send ConnEvent::PtkInstalled when ready
            }
            
            (ConnState::FourWayHandshake, ConnEvent::PtkInstalled) => {
                actions.push(ConnAction::CancelTimeout);
                self.state = ConnState::GroupKeyHandshake;
                actions.push(ConnAction::ScheduleTimeout(self.handshake_timeout_ms));
            }
            
            (ConnState::FourWayHandshake, ConnEvent::Timeout) => {
                self.state = ConnState::Disconnected;
                actions.push(ConnAction::NotifyFailed(ConnectError::HandshakeFailed));
                if let Some(bss) = &self.target_bss {
                    actions.push(ConnAction::SendDeauth(bss.bssid[0] as u16)); // Reason: Unspecified
                }
            }
            
            // --- Group Key Handshake State ---
            (ConnState::GroupKeyHandshake, ConnEvent::GtkInstalled) => {
                actions.push(ConnAction::CancelTimeout);
                self.state = ConnState::Connected;
                actions.push(ConnAction::EnableDataPath);
                
                if let (Some(bss), Some(req)) = (&self.target_bss, &self.request) {
                    actions.push(ConnAction::NotifyConnected(ConnectedInfo {
                        bssid: bss.bssid,
                        ssid: bss.ssid.clone(),
                        frequency: bss.frequency,
                        signal: bss.signal,
                        security: req.security,
                        aid: self.aid,
                    }));
                }
            }
            
            (ConnState::GroupKeyHandshake, ConnEvent::Timeout) => {
                // GTK is optional for initial connection
                self.state = ConnState::Connected;
                actions.push(ConnAction::EnableDataPath);
                
                if let (Some(bss), Some(req)) = (&self.target_bss, &self.request) {
                    actions.push(ConnAction::NotifyConnected(ConnectedInfo {
                        bssid: bss.bssid,
                        ssid: bss.ssid.clone(),
                        frequency: bss.frequency,
                        signal: bss.signal,
                        security: req.security,
                        aid: self.aid,
                    }));
                }
            }
            
            // --- Connected State ---
            (ConnState::Connected, ConnEvent::Disconnect) => {
                self.state = ConnState::Disconnecting;
                actions.push(ConnAction::DisableDataPath);
                if let Some(bss) = &self.target_bss {
                    actions.push(ConnAction::SendDeauth(3));  // Reason: Deauth leaving
                }
                self.state = ConnState::Disconnected;
                actions.push(ConnAction::NotifyDisconnected(3));
            }
            
            (ConnState::Connected, ConnEvent::Deauth(reason)) => {
                self.state = ConnState::Disconnected;
                actions.push(ConnAction::DisableDataPath);
                actions.push(ConnAction::NotifyDisconnected(reason));
            }
            
            (ConnState::Connected, ConnEvent::BeaconLoss) => {
                // Could trigger roaming here
                self.state = ConnState::Disconnected;
                actions.push(ConnAction::DisableDataPath);
                actions.push(ConnAction::NotifyDisconnected(4));  // Disassoc: inactivity
            }
            
            (ConnState::Connected, ConnEvent::RoamTrigger(new_bss)) => {
                self.state = ConnState::Roaming;
                self.target_bss = Some(new_bss.clone());
                actions.push(ConnAction::SetChannel(new_bss.frequency));
                actions.push(ConnAction::SendAuth(AuthFrame::open_system(new_bss.bssid)));
                actions.push(ConnAction::ScheduleTimeout(self.auth_timeout_ms));
            }
            
            // --- Any state: Disconnect request ---
            (_, ConnEvent::Disconnect) => {
                actions.push(ConnAction::CancelTimeout);
                if let Some(bss) = &self.target_bss {
                    if self.state != ConnState::Disconnected {
                        actions.push(ConnAction::SendDeauth(3));
                    }
                }
                self.state = ConnState::Disconnected;
                self.request = None;
                self.target_bss = None;
                actions.push(ConnAction::DisableDataPath);
            }
            
            _ => {}
        }
        
        actions
    }
    
    /// Build RSN IE for WPA2-PSK
    fn build_rsn_ie() -> Vec<u8> {
        vec![
            0x30,       // RSN IE
            0x14,       // Length
            0x01, 0x00, // Version 1
            0x00, 0x0F, 0xAC, 0x04, // Group cipher: CCMP
            0x01, 0x00, // Pairwise cipher count: 1
            0x00, 0x0F, 0xAC, 0x04, // Pairwise cipher: CCMP
            0x01, 0x00, // AKM count: 1
            0x00, 0x0F, 0xAC, 0x02, // AKM: PSK
            0x00, 0x00, // RSN capabilities
        ]
    }
}

impl Default for ConnectionStateMachine {
    fn default() -> Self {
        Self::new([0; 6])
    }
}
