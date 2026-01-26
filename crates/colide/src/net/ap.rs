//! WiFi Access Point (SoftAP) Mode
//!
//! Implements hostapd-like functionality for running as an access point,
//! supporting client association, authentication, and data forwarding.

use crate::net::scan::SecurityType;

/// AP state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ApState {
    #[default]
    Disabled,
    Starting,
    Running,
    Stopping,
}

/// AP configuration
#[derive(Debug, Clone)]
pub struct ApConfig {
    /// SSID
    pub ssid: Vec<u8>,
    /// Channel
    pub channel: u8,
    /// Channel width (20/40/80 MHz)
    pub channel_width: u8,
    /// Security type
    pub security: SecurityType,
    /// WPA passphrase (for WPA2-PSK)
    pub passphrase: Option<Vec<u8>>,
    /// Hidden SSID
    pub hidden: bool,
    /// Maximum number of clients
    pub max_clients: u8,
    /// Beacon interval (TUs)
    pub beacon_interval: u16,
    /// DTIM period
    pub dtim_period: u8,
    /// Enable WMM/QoS
    pub wmm_enabled: bool,
    /// Enable 802.11n (HT)
    pub ht_enabled: bool,
    /// Enable 802.11ac (VHT)
    pub vht_enabled: bool,
    /// Country code
    pub country_code: [u8; 2],
    /// Inactivity timeout (seconds)
    pub inactivity_timeout: u32,
}

impl Default for ApConfig {
    fn default() -> Self {
        Self {
            ssid: b"ColideAP".to_vec(),
            channel: 6,
            channel_width: 20,
            security: SecurityType::Wpa2Psk,
            passphrase: None,
            hidden: false,
            max_clients: 10,
            beacon_interval: 100,
            dtim_period: 2,
            wmm_enabled: true,
            ht_enabled: true,
            vht_enabled: false,
            country_code: *b"US",
            inactivity_timeout: 300,
        }
    }
}

impl ApConfig {
    pub fn open(ssid: &[u8], channel: u8) -> Self {
        Self {
            ssid: ssid.to_vec(),
            channel,
            security: SecurityType::Open,
            passphrase: None,
            ..Default::default()
        }
    }
    
    pub fn wpa2(ssid: &[u8], passphrase: &[u8], channel: u8) -> Self {
        Self {
            ssid: ssid.to_vec(),
            channel,
            security: SecurityType::Wpa2Psk,
            passphrase: Some(passphrase.to_vec()),
            ..Default::default()
        }
    }
}

/// Connected client (STA) state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum ClientState {
    #[default]
    None,
    Authenticating,
    Associating,
    Associated,
    Authorized,
}

/// Connected client info
#[derive(Debug, Clone)]
pub struct ApClient {
    pub addr: [u8; 6],
    pub state: ClientState,
    pub aid: u16,
    pub capabilities: u16,
    pub listen_interval: u16,
    pub signal: i8,
    pub supported_rates: Vec<u8>,
    pub ht_capabilities: Option<HtCapabilities>,
    pub last_rx: u64,
    pub last_tx: u64,
    pub rx_bytes: u64,
    pub tx_bytes: u64,
    pub rx_packets: u64,
    pub tx_packets: u64,
    pub ptk: Option<[u8; 16]>,
}

impl ApClient {
    pub fn new(addr: [u8; 6]) -> Self {
        Self {
            addr,
            state: ClientState::None,
            aid: 0,
            capabilities: 0,
            listen_interval: 0,
            signal: -100,
            supported_rates: Vec::new(),
            ht_capabilities: None,
            last_rx: 0,
            last_tx: 0,
            rx_bytes: 0,
            tx_bytes: 0,
            rx_packets: 0,
            tx_packets: 0,
            ptk: None,
        }
    }
    
    pub fn is_authorized(&self) -> bool {
        self.state == ClientState::Authorized
    }
}

/// HT capabilities (simplified)
#[derive(Debug, Clone, Copy)]
pub struct HtCapabilities {
    pub cap_info: u16,
    pub ampdu_params: u8,
    pub mcs_set: [u8; 16],
}

/// AP Manager
pub struct ApManager {
    state: ApState,
    config: ApConfig,
    our_addr: [u8; 6],
    clients: Vec<ApClient>,
    next_aid: u16,
    beacon_count: u64,
    gtk: [u8; 16],
    gtk_index: u8,
}

impl ApManager {
    pub fn new(addr: [u8; 6]) -> Self {
        Self {
            state: ApState::Disabled,
            config: ApConfig::default(),
            our_addr: addr,
            clients: Vec::new(),
            next_aid: 1,
            beacon_count: 0,
            gtk: [0; 16],
            gtk_index: 1,
        }
    }
    
    /// Start AP with configuration
    pub fn start(&mut self, config: ApConfig) -> ApAction {
        self.config = config;
        self.state = ApState::Starting;
        self.clients.clear();
        self.next_aid = 1;
        self.beacon_count = 0;
        
        // Generate GTK
        for (i, b) in self.gtk.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(23).wrapping_add(self.our_addr[i % 6]);
        }
        
        ApAction::ConfigureInterface(self.config.clone())
    }
    
    /// Interface configured, start beaconing
    pub fn on_interface_ready(&mut self) -> ApAction {
        self.state = ApState::Running;
        ApAction::StartBeaconing
    }
    
    /// Stop AP
    pub fn stop(&mut self) -> ApAction {
        self.state = ApState::Stopping;
        
        // Disconnect all clients
        let addrs: Vec<[u8; 6]> = self.clients.iter().map(|c| c.addr).collect();
        self.clients.clear();
        
        ApAction::DisconnectAll(addrs)
    }
    
    /// AP stopped
    pub fn on_stopped(&mut self) {
        self.state = ApState::Disabled;
    }
    
    /// Generate beacon frame
    pub fn build_beacon(&mut self, timestamp: u64) -> Vec<u8> {
        self.beacon_count += 1;
        
        let mut beacon = Vec::with_capacity(256);
        
        // Frame Control: Beacon
        beacon.extend_from_slice(&[0x80, 0x00]);
        // Duration
        beacon.extend_from_slice(&[0x00, 0x00]);
        // DA (broadcast)
        beacon.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);
        // SA (our address)
        beacon.extend_from_slice(&self.our_addr);
        // BSSID (our address)
        beacon.extend_from_slice(&self.our_addr);
        // Sequence Control
        let seq = ((self.beacon_count as u16) << 4) & 0xFFF0;
        beacon.extend_from_slice(&seq.to_le_bytes());
        
        // Fixed parameters
        // Timestamp (8 bytes)
        beacon.extend_from_slice(&timestamp.to_le_bytes());
        // Beacon Interval
        beacon.extend_from_slice(&self.config.beacon_interval.to_le_bytes());
        // Capability Info
        let cap: u16 = 0x0411;  // ESS, Short Preamble, Short Slot Time
        beacon.extend_from_slice(&cap.to_le_bytes());
        
        // Information Elements
        // SSID
        if !self.config.hidden {
            beacon.push(0);  // Element ID
            beacon.push(self.config.ssid.len() as u8);
            beacon.extend_from_slice(&self.config.ssid);
        } else {
            beacon.push(0);
            beacon.push(0);  // Hidden SSID
        }
        
        // Supported Rates
        beacon.push(1);  // Element ID
        beacon.push(8);
        beacon.extend_from_slice(&[0x82, 0x84, 0x8B, 0x96, 0x0C, 0x12, 0x18, 0x24]);
        
        // DS Parameter Set (channel)
        beacon.push(3);  // Element ID
        beacon.push(1);
        beacon.push(self.config.channel);
        
        // TIM (Traffic Indication Map)
        beacon.push(5);  // Element ID
        beacon.push(4);
        beacon.push(self.config.dtim_period);  // DTIM count
        beacon.push(self.config.dtim_period);  // DTIM period
        beacon.push(0);  // Bitmap control
        beacon.push(0);  // Partial virtual bitmap
        
        // Country IE
        beacon.push(7);  // Element ID
        beacon.push(6);
        beacon.extend_from_slice(&self.config.country_code);
        beacon.push(b' ');  // Environment
        beacon.push(1);   // First channel
        beacon.push(11);  // Number of channels
        beacon.push(20);  // Max TX power
        
        // ERP Information
        beacon.push(42);  // Element ID
        beacon.push(1);
        beacon.push(0);   // No protection
        
        // Extended Supported Rates
        beacon.push(50);  // Element ID
        beacon.push(4);
        beacon.extend_from_slice(&[0x30, 0x48, 0x60, 0x6C]);
        
        // HT Capabilities (if enabled)
        if self.config.ht_enabled {
            beacon.push(45);  // Element ID
            beacon.push(26);
            // HT Cap Info
            beacon.extend_from_slice(&[0x6F, 0x00]);  // 20/40MHz, Short GI
            // A-MPDU Parameters
            beacon.push(0x17);
            // MCS Set (16 bytes)
            beacon.extend_from_slice(&[0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
            beacon.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
            // HT Extended Cap, TX Beamforming, ASEL
            beacon.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00]);
        }
        
        // HT Operation
        if self.config.ht_enabled {
            beacon.push(61);  // Element ID
            beacon.push(22);
            beacon.push(self.config.channel);  // Primary channel
            beacon.push(0x00);  // HT Info subset 1
            beacon.extend_from_slice(&[0x00, 0x00]);  // HT Info subset 2-3
            beacon.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
            beacon.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
        }
        
        // RSN IE (for WPA2)
        if self.config.security == SecurityType::Wpa2Psk {
            beacon.extend_from_slice(&self.build_rsn_ie());
        }
        
        // WMM/WME Parameter Element
        if self.config.wmm_enabled {
            beacon.push(0xDD);  // Vendor Specific
            beacon.push(24);
            beacon.extend_from_slice(&[0x00, 0x50, 0xF2, 0x02]);  // WMM OUI + Type
            beacon.push(0x01);  // Subtype: WMM Parameter
            beacon.push(0x01);  // Version
            beacon.push(0x80);  // QoS Info (AP)
            beacon.push(0x00);  // Reserved
            // AC_BE parameters
            beacon.extend_from_slice(&[0x03, 0xA4, 0x00, 0x00]);
            // AC_BK parameters
            beacon.extend_from_slice(&[0x27, 0xA4, 0x00, 0x00]);
            // AC_VI parameters
            beacon.extend_from_slice(&[0x42, 0x43, 0x5E, 0x00]);
            // AC_VO parameters
            beacon.extend_from_slice(&[0x62, 0x32, 0x2F, 0x00]);
        }
        
        beacon
    }
    
    /// Build RSN IE for WPA2-PSK
    fn build_rsn_ie(&self) -> Vec<u8> {
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
    
    /// Process authentication request
    pub fn on_auth_request(&mut self, client_addr: [u8; 6], algorithm: u16, seq: u16) -> ApAction {
        if self.state != ApState::Running {
            return ApAction::None;
        }
        
        // Only support Open System auth
        if algorithm != 0 || seq != 1 {
            return ApAction::SendAuthResponse {
                addr: client_addr,
                status: 13,  // Unsupported auth algorithm
            };
        }
        
        // Find or create client
        if !self.clients.iter().any(|c| c.addr == client_addr) {
            if self.clients.len() >= self.config.max_clients as usize {
                return ApAction::SendAuthResponse {
                    addr: client_addr,
                    status: 17,  // AP unable to handle additional STAs
                };
            }
            let mut client = ApClient::new(client_addr);
            client.state = ClientState::Authenticating;
            self.clients.push(client);
        } else if let Some(client) = self.clients.iter_mut().find(|c| c.addr == client_addr) {
            client.state = ClientState::Authenticating;
        }
        
        ApAction::SendAuthResponse {
            addr: client_addr,
            status: 0,  // Success
        }
    }
    
    /// Build authentication response frame
    pub fn build_auth_response(&self, client_addr: [u8; 6], status: u16) -> Vec<u8> {
        let mut frame = Vec::with_capacity(30);
        
        // Frame Control: Authentication
        frame.extend_from_slice(&[0xB0, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (client)
        frame.extend_from_slice(&client_addr);
        // SA (our address)
        frame.extend_from_slice(&self.our_addr);
        // BSSID (our address)
        frame.extend_from_slice(&self.our_addr);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        // Auth Algorithm (Open System)
        frame.extend_from_slice(&0u16.to_le_bytes());
        // Auth Seq Num (2 = response)
        frame.extend_from_slice(&2u16.to_le_bytes());
        // Status
        frame.extend_from_slice(&status.to_le_bytes());
        
        frame
    }
    
    /// Process association request
    pub fn on_assoc_request(&mut self, client_addr: [u8; 6], capabilities: u16, 
                            listen_interval: u16, ssid: &[u8], rates: &[u8]) -> ApAction {
        if self.state != ApState::Running {
            return ApAction::None;
        }
        
        // Verify SSID matches
        if ssid != self.config.ssid.as_slice() {
            return ApAction::SendAssocResponse {
                addr: client_addr,
                status: 12,  // Association denied
                aid: 0,
            };
        }
        
        // Find client
        let client = match self.clients.iter_mut().find(|c| c.addr == client_addr) {
            Some(c) => c,
            None => {
                return ApAction::SendAssocResponse {
                    addr: client_addr,
                    status: 11,  // Association denied, not authenticated
                    aid: 0,
                };
            }
        };
        
        // Assign AID
        let aid = self.next_aid;
        self.next_aid = self.next_aid.wrapping_add(1).max(1);
        
        client.state = ClientState::Associated;
        client.aid = aid;
        client.capabilities = capabilities;
        client.listen_interval = listen_interval;
        client.supported_rates = rates.to_vec();
        
        // For open networks, client is immediately authorized
        if self.config.security == SecurityType::Open {
            client.state = ClientState::Authorized;
            return ApAction::ClientConnected(client_addr);
        }
        
        ApAction::SendAssocResponse {
            addr: client_addr,
            status: 0,
            aid,
        }
    }
    
    /// Build association response frame
    pub fn build_assoc_response(&self, client_addr: [u8; 6], status: u16, aid: u16) -> Vec<u8> {
        let mut frame = Vec::with_capacity(64);
        
        // Frame Control: Association Response
        frame.extend_from_slice(&[0x10, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (client)
        frame.extend_from_slice(&client_addr);
        // SA (our address)
        frame.extend_from_slice(&self.our_addr);
        // BSSID (our address)
        frame.extend_from_slice(&self.our_addr);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        // Capability Info
        frame.extend_from_slice(&0x0411u16.to_le_bytes());
        // Status
        frame.extend_from_slice(&status.to_le_bytes());
        // AID (with bits 14-15 set)
        frame.extend_from_slice(&(aid | 0xC000).to_le_bytes());
        
        // Supported Rates
        frame.push(1);
        frame.push(8);
        frame.extend_from_slice(&[0x82, 0x84, 0x8B, 0x96, 0x0C, 0x12, 0x18, 0x24]);
        
        // Extended Supported Rates
        frame.push(50);
        frame.push(4);
        frame.extend_from_slice(&[0x30, 0x48, 0x60, 0x6C]);
        
        // HT Capabilities (if enabled)
        if self.config.ht_enabled {
            frame.push(45);
            frame.push(26);
            frame.extend_from_slice(&[0x6F, 0x00]);
            frame.push(0x17);
            frame.extend_from_slice(&[0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
            frame.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
            frame.extend_from_slice(&[0x00, 0x00, 0x00, 0x00, 0x00]);
        }
        
        // HT Operation
        if self.config.ht_enabled {
            frame.push(61);
            frame.push(22);
            frame.push(self.config.channel);
            frame.extend_from_slice(&[0x00; 21]);
        }
        
        frame
    }
    
    /// Process EAPOL frame from client (WPA handshake)
    pub fn on_eapol(&mut self, client_addr: [u8; 6], _eapol: &[u8]) -> ApAction {
        // WPA 4-way handshake handling would go here
        // For now, simplified: mark client as authorized after receiving EAPOL
        if let Some(client) = self.clients.iter_mut().find(|c| c.addr == client_addr) {
            if client.state == ClientState::Associated {
                // In real impl, verify EAPOL and derive PTK
                client.state = ClientState::Authorized;
                return ApAction::ClientConnected(client_addr);
            }
        }
        ApAction::None
    }
    
    /// Process deauthentication from client
    pub fn on_deauth(&mut self, client_addr: [u8; 6], _reason: u16) {
        self.clients.retain(|c| c.addr != client_addr);
    }
    
    /// Process disassociation from client
    pub fn on_disassoc(&mut self, client_addr: [u8; 6], _reason: u16) {
        if let Some(client) = self.clients.iter_mut().find(|c| c.addr == client_addr) {
            client.state = ClientState::Authenticating;  // Go back to authenticated state
        }
    }
    
    /// Disconnect a client
    pub fn disconnect_client(&mut self, client_addr: [u8; 6], reason: u16) -> ApAction {
        self.clients.retain(|c| c.addr != client_addr);
        ApAction::SendDeauth { addr: client_addr, reason }
    }
    
    /// Update client activity
    pub fn on_client_activity(&mut self, client_addr: [u8; 6], timestamp: u64, signal: i8) {
        if let Some(client) = self.clients.iter_mut().find(|c| c.addr == client_addr) {
            client.last_rx = timestamp;
            client.signal = signal;
        }
    }
    
    /// Check for inactive clients
    pub fn check_inactivity(&mut self, now: u64) -> Vec<[u8; 6]> {
        let timeout = self.config.inactivity_timeout as u64 * 1000;
        let inactive: Vec<[u8; 6]> = self.clients.iter()
            .filter(|c| now - c.last_rx > timeout)
            .map(|c| c.addr)
            .collect();
        
        for addr in &inactive {
            self.clients.retain(|c| c.addr != *addr);
        }
        
        inactive
    }
    
    pub fn state(&self) -> ApState {
        self.state
    }
    
    pub fn config(&self) -> &ApConfig {
        &self.config
    }
    
    pub fn clients(&self) -> &[ApClient] {
        &self.clients
    }
    
    pub fn client_count(&self) -> usize {
        self.clients.iter().filter(|c| c.is_authorized()).count()
    }
}

impl Default for ApManager {
    fn default() -> Self {
        Self::new([0; 6])
    }
}

/// AP action
#[derive(Debug, Clone)]
pub enum ApAction {
    None,
    ConfigureInterface(ApConfig),
    StartBeaconing,
    SendAuthResponse { addr: [u8; 6], status: u16 },
    SendAssocResponse { addr: [u8; 6], status: u16, aid: u16 },
    SendDeauth { addr: [u8; 6], reason: u16 },
    StartWpaHandshake([u8; 6]),
    ClientConnected([u8; 6]),
    ClientDisconnected([u8; 6]),
    DisconnectAll(Vec<[u8; 6]>),
}
