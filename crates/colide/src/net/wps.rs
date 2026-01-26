//! WiFi Protected Setup (WPS) Implementation
//!
//! Implements WPS protocol for simplified WiFi configuration
//! including Push Button Configuration (PBC) and PIN methods.

use crate::net::aes;

/// WPS state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum WpsState {
    #[default]
    Idle,
    Started,
    M1Sent,
    M2Received,
    M3Sent,
    M4Received,
    M5Sent,
    M6Received,
    M7Sent,
    M8Received,
    Done,
    Failed,
}

/// WPS method
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum WpsMethod {
    #[default]
    PushButton,
    PinDisplay,
    PinKeypad,
    NfcInterface,
    NfcTag,
    UsbFlash,
}

/// WPS configuration
#[derive(Debug, Clone)]
pub struct WpsConfig {
    pub method: WpsMethod,
    pub pin: Option<String>,
    pub timeout_ms: u64,
    pub uuid: [u8; 16],
    pub device_name: String,
    pub manufacturer: String,
    pub model_name: String,
    pub model_number: String,
    pub serial_number: String,
    pub device_type: WpsDeviceType,
}

impl Default for WpsConfig {
    fn default() -> Self {
        Self {
            method: WpsMethod::PushButton,
            pin: None,
            timeout_ms: 120000,  // 2 minutes
            uuid: [0; 16],
            device_name: "Colide Device".to_string(),
            manufacturer: "Elide".to_string(),
            model_name: "Colide".to_string(),
            model_number: "1.0".to_string(),
            serial_number: "000000".to_string(),
            device_type: WpsDeviceType::default(),
        }
    }
}

/// WPS device type
#[derive(Debug, Clone, Copy, Default)]
pub struct WpsDeviceType {
    pub category: u16,
    pub oui: [u8; 4],
    pub subcategory: u16,
}

impl WpsDeviceType {
    pub fn computer() -> Self {
        Self {
            category: 1,
            oui: [0x00, 0x50, 0xF2, 0x04],
            subcategory: 1,  // PC
        }
    }
    
    pub fn to_bytes(&self) -> [u8; 8] {
        let mut bytes = [0u8; 8];
        bytes[0..2].copy_from_slice(&self.category.to_be_bytes());
        bytes[2..6].copy_from_slice(&self.oui);
        bytes[6..8].copy_from_slice(&self.subcategory.to_be_bytes());
        bytes
    }
}

/// WPS attribute types
#[derive(Debug, Clone, Copy)]
#[repr(u16)]
pub enum WpsAttr {
    Version = 0x104A,
    MessageType = 0x1022,
    EnrolleeNonce = 0x101A,
    RegistrarNonce = 0x1039,
    PublicKey = 0x1032,
    AuthType = 0x1003,
    EncryptionType = 0x100F,
    ConnectionType = 0x100D,
    ConfigMethods = 0x1008,
    WpsState = 0x1044,
    Manufacturer = 0x1021,
    ModelName = 0x1023,
    ModelNumber = 0x1024,
    SerialNumber = 0x1042,
    DeviceName = 0x1011,
    PrimaryDeviceType = 0x1054,
    RfBands = 0x103C,
    AssocState = 0x1002,
    ConfigError = 0x1009,
    DevicePassword = 0x1012,
    OsVersion = 0x102D,
    AuthTypeFlags = 0x1004,
    EncryptionTypeFlags = 0x1010,
    UuidE = 0x1047,
    UuidR = 0x1048,
    MacAddress = 0x1020,
    KeyWrapAuth = 0x101E,
    Authenticator = 0x1005,
    EHash1 = 0x1014,
    EHash2 = 0x1015,
    RHash1 = 0x103D,
    RHash2 = 0x103E,
    ESnonce1 = 0x1016,
    ESnonce2 = 0x1017,
    RSnonce1 = 0x103F,
    RSnonce2 = 0x1040,
    EncryptedSettings = 0x1018,
    Credential = 0x100E,
    Ssid = 0x1045,
    NetworkKey = 0x1027,
    NetworkIndex = 0x1026,
}

/// WPS message type
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum WpsMessageType {
    Beacon = 0x01,
    ProbeRequest = 0x02,
    ProbeResponse = 0x03,
    M1 = 0x04,
    M2 = 0x05,
    M2D = 0x06,
    M3 = 0x07,
    M4 = 0x08,
    M5 = 0x09,
    M6 = 0x0A,
    M7 = 0x0B,
    M8 = 0x0C,
    WscAck = 0x0D,
    WscNack = 0x0E,
    WscDone = 0x0F,
}

/// WPS authentication types
#[derive(Debug, Clone, Copy)]
pub struct AuthTypeFlags(u16);

impl AuthTypeFlags {
    pub const OPEN: u16 = 0x0001;
    pub const WPAPSK: u16 = 0x0002;
    pub const SHARED: u16 = 0x0004;
    pub const WPA: u16 = 0x0008;
    pub const WPA2: u16 = 0x0010;
    pub const WPA2PSK: u16 = 0x0020;
    
    pub fn new(flags: u16) -> Self {
        Self(flags)
    }
    
    pub fn wpa2_psk() -> Self {
        Self(Self::WPA2PSK)
    }
}

/// WPS encryption types
#[derive(Debug, Clone, Copy)]
pub struct EncryptionTypeFlags(u16);

impl EncryptionTypeFlags {
    pub const NONE: u16 = 0x0001;
    pub const WEP: u16 = 0x0002;
    pub const TKIP: u16 = 0x0004;
    pub const AES: u16 = 0x0008;
    
    pub fn new(flags: u16) -> Self {
        Self(flags)
    }
    
    pub fn aes() -> Self {
        Self(Self::AES)
    }
}

/// WPS credential (result of successful WPS)
#[derive(Debug, Clone)]
pub struct WpsCredential {
    pub ssid: Vec<u8>,
    pub auth_type: u16,
    pub encryption_type: u16,
    pub network_key: Vec<u8>,
    pub mac_addr: [u8; 6],
}

/// WPS session data
pub struct WpsSession {
    state: WpsState,
    config: WpsConfig,
    enrollee_nonce: [u8; 16],
    registrar_nonce: [u8; 16],
    enrollee_public_key: Vec<u8>,
    registrar_public_key: Vec<u8>,
    auth_key: [u8; 32],
    key_wrap_key: [u8; 16],
    emsk: [u8; 32],
    e_snonce1: [u8; 16],
    e_snonce2: [u8; 16],
    r_snonce1: [u8; 16],
    r_snonce2: [u8; 16],
    psk1: [u8; 16],
    psk2: [u8; 16],
    credential: Option<WpsCredential>,
}

impl WpsSession {
    pub fn new(config: WpsConfig) -> Self {
        let mut enrollee_nonce = [0u8; 16];
        // In real implementation, use secure random
        for (i, b) in enrollee_nonce.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(17).wrapping_add(42);
        }
        
        Self {
            state: WpsState::Idle,
            config,
            enrollee_nonce,
            registrar_nonce: [0; 16],
            enrollee_public_key: Vec::new(),
            registrar_public_key: Vec::new(),
            auth_key: [0; 32],
            key_wrap_key: [0; 16],
            emsk: [0; 32],
            e_snonce1: [0; 16],
            e_snonce2: [0; 16],
            r_snonce1: [0; 16],
            r_snonce2: [0; 16],
            psk1: [0; 16],
            psk2: [0; 16],
            credential: None,
        }
    }
    
    /// Start WPS session
    pub fn start(&mut self) -> WpsAction {
        self.state = WpsState::Started;
        WpsAction::SendProbeRequest
    }
    
    /// Generate M1 message
    pub fn build_m1(&mut self, mac: [u8; 6]) -> Vec<u8> {
        self.state = WpsState::M1Sent;
        
        // Generate DH public key (simplified - real impl needs proper DH)
        self.enrollee_public_key = vec![0u8; 192];
        for (i, b) in self.enrollee_public_key.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(7);
        }
        
        // Generate secret nonces
        for (i, b) in self.e_snonce1.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(13).wrapping_add(1);
        }
        for (i, b) in self.e_snonce2.iter_mut().enumerate() {
            *b = (i as u8).wrapping_mul(19).wrapping_add(2);
        }
        
        let mut msg = Vec::with_capacity(512);
        
        // Version
        Self::add_attr(&mut msg, WpsAttr::Version, &[0x10]);
        // Message Type
        Self::add_attr(&mut msg, WpsAttr::MessageType, &[WpsMessageType::M1 as u8]);
        // UUID-E
        Self::add_attr(&mut msg, WpsAttr::UuidE, &self.config.uuid);
        // MAC Address
        Self::add_attr(&mut msg, WpsAttr::MacAddress, &mac);
        // Enrollee Nonce
        Self::add_attr(&mut msg, WpsAttr::EnrolleeNonce, &self.enrollee_nonce);
        // Public Key
        Self::add_attr(&mut msg, WpsAttr::PublicKey, &self.enrollee_public_key);
        // Auth Type Flags
        Self::add_attr(&mut msg, WpsAttr::AuthTypeFlags, &AuthTypeFlags::wpa2_psk().0.to_be_bytes());
        // Encryption Type Flags
        Self::add_attr(&mut msg, WpsAttr::EncryptionTypeFlags, &EncryptionTypeFlags::aes().0.to_be_bytes());
        // Connection Type Flags
        Self::add_attr(&mut msg, WpsAttr::ConnectionType, &[0x01]); // ESS
        // Config Methods
        let methods = match self.config.method {
            WpsMethod::PushButton => 0x0080u16,
            WpsMethod::PinDisplay => 0x0008,
            WpsMethod::PinKeypad => 0x0100,
            _ => 0x0008,
        };
        Self::add_attr(&mut msg, WpsAttr::ConfigMethods, &methods.to_be_bytes());
        // WPS State
        Self::add_attr(&mut msg, WpsAttr::WpsState, &[0x01]); // Not Configured
        // Manufacturer
        Self::add_attr(&mut msg, WpsAttr::Manufacturer, self.config.manufacturer.as_bytes());
        // Model Name
        Self::add_attr(&mut msg, WpsAttr::ModelName, self.config.model_name.as_bytes());
        // Model Number
        Self::add_attr(&mut msg, WpsAttr::ModelNumber, self.config.model_number.as_bytes());
        // Serial Number
        Self::add_attr(&mut msg, WpsAttr::SerialNumber, self.config.serial_number.as_bytes());
        // Primary Device Type
        Self::add_attr(&mut msg, WpsAttr::PrimaryDeviceType, &self.config.device_type.to_bytes());
        // Device Name
        Self::add_attr(&mut msg, WpsAttr::DeviceName, self.config.device_name.as_bytes());
        // RF Bands
        Self::add_attr(&mut msg, WpsAttr::RfBands, &[0x03]); // 2.4 + 5 GHz
        // Association State
        Self::add_attr(&mut msg, WpsAttr::AssocState, &[0x00, 0x00]); // Not Associated
        // Device Password ID
        let pwd_id: u16 = match self.config.method {
            WpsMethod::PushButton => 0x0004,
            _ => 0x0000, // Default PIN
        };
        Self::add_attr(&mut msg, WpsAttr::DevicePassword, &pwd_id.to_be_bytes());
        // Config Error
        Self::add_attr(&mut msg, WpsAttr::ConfigError, &[0x00, 0x00]); // No Error
        // OS Version
        Self::add_attr(&mut msg, WpsAttr::OsVersion, &[0x80, 0x00, 0x00, 0x00]);
        
        msg
    }
    
    /// Process M2 message
    pub fn process_m2(&mut self, data: &[u8]) -> Result<WpsAction, WpsError> {
        // Parse attributes
        let attrs = Self::parse_attrs(data)?;
        
        // Get registrar nonce
        if let Some(nonce) = attrs.get(&(WpsAttr::RegistrarNonce as u16)) {
            if nonce.len() == 16 {
                self.registrar_nonce.copy_from_slice(nonce);
            }
        }
        
        // Get registrar public key
        if let Some(pk) = attrs.get(&(WpsAttr::PublicKey as u16)) {
            self.registrar_public_key = pk.clone();
        }
        
        // Derive keys (simplified - real impl needs proper DH + KDF)
        self.derive_keys();
        
        self.state = WpsState::M2Received;
        Ok(WpsAction::SendM3)
    }
    
    /// Build M3 message
    pub fn build_m3(&mut self) -> Vec<u8> {
        self.state = WpsState::M3Sent;
        
        let mut msg = Vec::with_capacity(256);
        
        // Version
        Self::add_attr(&mut msg, WpsAttr::Version, &[0x10]);
        // Message Type
        Self::add_attr(&mut msg, WpsAttr::MessageType, &[WpsMessageType::M3 as u8]);
        // Registrar Nonce
        Self::add_attr(&mut msg, WpsAttr::RegistrarNonce, &self.registrar_nonce);
        // E-Hash1
        let e_hash1 = self.compute_e_hash1();
        Self::add_attr(&mut msg, WpsAttr::EHash1, &e_hash1);
        // E-Hash2
        let e_hash2 = self.compute_e_hash2();
        Self::add_attr(&mut msg, WpsAttr::EHash2, &e_hash2);
        // Authenticator
        let auth = self.compute_authenticator(&msg);
        Self::add_attr(&mut msg, WpsAttr::Authenticator, &auth);
        
        msg
    }
    
    /// Process M4 message
    pub fn process_m4(&mut self, data: &[u8]) -> Result<WpsAction, WpsError> {
        let attrs = Self::parse_attrs(data)?;
        
        // Get R-SNonce1 from encrypted settings
        if let Some(encrypted) = attrs.get(&(WpsAttr::EncryptedSettings as u16)) {
            if let Ok(decrypted) = self.decrypt_settings(encrypted) {
                let inner_attrs = Self::parse_attrs(&decrypted)?;
                if let Some(snonce) = inner_attrs.get(&(WpsAttr::RSnonce1 as u16)) {
                    if snonce.len() == 16 {
                        self.r_snonce1.copy_from_slice(snonce);
                    }
                }
            }
        }
        
        self.state = WpsState::M4Received;
        Ok(WpsAction::SendM5)
    }
    
    /// Build M5 message
    pub fn build_m5(&mut self) -> Vec<u8> {
        self.state = WpsState::M5Sent;
        
        let mut msg = Vec::with_capacity(256);
        
        // Version
        Self::add_attr(&mut msg, WpsAttr::Version, &[0x10]);
        // Message Type
        Self::add_attr(&mut msg, WpsAttr::MessageType, &[WpsMessageType::M5 as u8]);
        // Registrar Nonce
        Self::add_attr(&mut msg, WpsAttr::RegistrarNonce, &self.registrar_nonce);
        // Encrypted Settings (E-SNonce1)
        let mut inner = Vec::new();
        Self::add_attr(&mut inner, WpsAttr::ESnonce1, &self.e_snonce1);
        let encrypted = self.encrypt_settings(&inner);
        Self::add_attr(&mut msg, WpsAttr::EncryptedSettings, &encrypted);
        // Authenticator
        let auth = self.compute_authenticator(&msg);
        Self::add_attr(&mut msg, WpsAttr::Authenticator, &auth);
        
        msg
    }
    
    /// Process M6 message
    pub fn process_m6(&mut self, data: &[u8]) -> Result<WpsAction, WpsError> {
        let attrs = Self::parse_attrs(data)?;
        
        // Get R-SNonce2 from encrypted settings
        if let Some(encrypted) = attrs.get(&(WpsAttr::EncryptedSettings as u16)) {
            if let Ok(decrypted) = self.decrypt_settings(encrypted) {
                let inner_attrs = Self::parse_attrs(&decrypted)?;
                if let Some(snonce) = inner_attrs.get(&(WpsAttr::RSnonce2 as u16)) {
                    if snonce.len() == 16 {
                        self.r_snonce2.copy_from_slice(snonce);
                    }
                }
            }
        }
        
        self.state = WpsState::M6Received;
        Ok(WpsAction::SendM7)
    }
    
    /// Build M7 message
    pub fn build_m7(&mut self) -> Vec<u8> {
        self.state = WpsState::M7Sent;
        
        let mut msg = Vec::with_capacity(256);
        
        // Version
        Self::add_attr(&mut msg, WpsAttr::Version, &[0x10]);
        // Message Type
        Self::add_attr(&mut msg, WpsAttr::MessageType, &[WpsMessageType::M7 as u8]);
        // Registrar Nonce
        Self::add_attr(&mut msg, WpsAttr::RegistrarNonce, &self.registrar_nonce);
        // Encrypted Settings (E-SNonce2)
        let mut inner = Vec::new();
        Self::add_attr(&mut inner, WpsAttr::ESnonce2, &self.e_snonce2);
        let encrypted = self.encrypt_settings(&inner);
        Self::add_attr(&mut msg, WpsAttr::EncryptedSettings, &encrypted);
        // Authenticator
        let auth = self.compute_authenticator(&msg);
        Self::add_attr(&mut msg, WpsAttr::Authenticator, &auth);
        
        msg
    }
    
    /// Process M8 message (contains credentials)
    pub fn process_m8(&mut self, data: &[u8]) -> Result<WpsCredential, WpsError> {
        let attrs = Self::parse_attrs(data)?;
        
        // Get credentials from encrypted settings
        if let Some(encrypted) = attrs.get(&(WpsAttr::EncryptedSettings as u16)) {
            let decrypted = self.decrypt_settings(encrypted)?;
            let inner_attrs = Self::parse_attrs(&decrypted)?;
            
            // Parse credential
            if let Some(cred_data) = inner_attrs.get(&(WpsAttr::Credential as u16)) {
                let cred_attrs = Self::parse_attrs(cred_data)?;
                
                let ssid = cred_attrs.get(&(WpsAttr::Ssid as u16))
                    .cloned()
                    .unwrap_or_default();
                let auth_type = cred_attrs.get(&(WpsAttr::AuthType as u16))
                    .map(|v| u16::from_be_bytes([v[0], v[1]]))
                    .unwrap_or(0);
                let enc_type = cred_attrs.get(&(WpsAttr::EncryptionType as u16))
                    .map(|v| u16::from_be_bytes([v[0], v[1]]))
                    .unwrap_or(0);
                let key = cred_attrs.get(&(WpsAttr::NetworkKey as u16))
                    .cloned()
                    .unwrap_or_default();
                let mac = cred_attrs.get(&(WpsAttr::MacAddress as u16))
                    .map(|v| {
                        let mut arr = [0u8; 6];
                        arr.copy_from_slice(&v[..6.min(v.len())]);
                        arr
                    })
                    .unwrap_or([0; 6]);
                
                let credential = WpsCredential {
                    ssid,
                    auth_type,
                    encryption_type: enc_type,
                    network_key: key,
                    mac_addr: mac,
                };
                
                self.credential = Some(credential.clone());
                self.state = WpsState::M8Received;
                return Ok(credential);
            }
        }
        
        Err(WpsError::InvalidMessage)
    }
    
    /// Build WSC_Done message
    pub fn build_done(&mut self) -> Vec<u8> {
        self.state = WpsState::Done;
        
        let mut msg = Vec::with_capacity(64);
        
        // Version
        Self::add_attr(&mut msg, WpsAttr::Version, &[0x10]);
        // Message Type
        Self::add_attr(&mut msg, WpsAttr::MessageType, &[WpsMessageType::WscDone as u8]);
        // Enrollee Nonce
        Self::add_attr(&mut msg, WpsAttr::EnrolleeNonce, &self.enrollee_nonce);
        // Registrar Nonce
        Self::add_attr(&mut msg, WpsAttr::RegistrarNonce, &self.registrar_nonce);
        
        msg
    }
    
    /// Add attribute to message
    fn add_attr(msg: &mut Vec<u8>, attr: WpsAttr, value: &[u8]) {
        msg.extend_from_slice(&(attr as u16).to_be_bytes());
        msg.extend_from_slice(&(value.len() as u16).to_be_bytes());
        msg.extend_from_slice(value);
    }
    
    /// Parse attributes from message
    fn parse_attrs(data: &[u8]) -> Result<std::collections::HashMap<u16, Vec<u8>>, WpsError> {
        let mut attrs = std::collections::HashMap::new();
        let mut offset = 0;
        
        while offset + 4 <= data.len() {
            let attr_type = u16::from_be_bytes([data[offset], data[offset + 1]]);
            let attr_len = u16::from_be_bytes([data[offset + 2], data[offset + 3]]) as usize;
            offset += 4;
            
            if offset + attr_len > data.len() {
                return Err(WpsError::InvalidMessage);
            }
            
            attrs.insert(attr_type, data[offset..offset + attr_len].to_vec());
            offset += attr_len;
        }
        
        Ok(attrs)
    }
    
    /// Derive session keys (simplified)
    fn derive_keys(&mut self) {
        // Real implementation would use DH shared secret + KDF
        // This is a placeholder showing the structure
        let mut seed = Vec::new();
        seed.extend_from_slice(&self.enrollee_nonce);
        seed.extend_from_slice(&self.registrar_nonce);
        
        // Derive AuthKey, KeyWrapKey, EMSK
        // Using simple hash for demo (real: PRF with DHKey)
        for (i, b) in self.auth_key.iter_mut().enumerate() {
            *b = seed[i % seed.len()].wrapping_mul(3);
        }
        for (i, b) in self.key_wrap_key.iter_mut().enumerate() {
            *b = seed[i % seed.len()].wrapping_mul(5);
        }
    }
    
    fn compute_e_hash1(&self) -> [u8; 32] {
        // E-Hash1 = HMAC(AuthKey, E-S1 || PSK1 || PKE || PKR)
        // Simplified placeholder
        let mut hash = [0u8; 32];
        for (i, b) in hash.iter_mut().enumerate() {
            *b = self.e_snonce1[i % 16] ^ self.psk1[i % 16];
        }
        hash
    }
    
    fn compute_e_hash2(&self) -> [u8; 32] {
        let mut hash = [0u8; 32];
        for (i, b) in hash.iter_mut().enumerate() {
            *b = self.e_snonce2[i % 16] ^ self.psk2[i % 16];
        }
        hash
    }
    
    fn compute_authenticator(&self, _msg: &[u8]) -> [u8; 8] {
        // HMAC-SHA256 of message with AuthKey, truncated to 8 bytes
        [0u8; 8]  // Placeholder
    }
    
    fn encrypt_settings(&self, data: &[u8]) -> Vec<u8> {
        // AES-CBC with KeyWrapKey
        // Simplified - real impl needs IV and proper padding
        let mut encrypted = vec![0u8; 16];  // IV
        encrypted.extend_from_slice(data);
        encrypted
    }
    
    fn decrypt_settings(&self, data: &[u8]) -> Result<Vec<u8>, WpsError> {
        // Skip IV
        if data.len() < 16 {
            return Err(WpsError::InvalidMessage);
        }
        Ok(data[16..].to_vec())
    }
    
    pub fn state(&self) -> WpsState {
        self.state
    }
    
    pub fn credential(&self) -> Option<&WpsCredential> {
        self.credential.as_ref()
    }
}

/// WPS action
#[derive(Debug, Clone)]
pub enum WpsAction {
    None,
    SendProbeRequest,
    SendM1,
    SendM3,
    SendM5,
    SendM7,
    SendDone,
    Complete(WpsCredential),
    Failed(WpsError),
}

/// WPS error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WpsError {
    Timeout,
    InvalidMessage,
    AuthFailed,
    PinMismatch,
    ProtocolError,
    Cancelled,
}

/// Generate random WPS PIN
pub fn generate_pin() -> String {
    // Generate 7-digit random number + checksum
    let mut pin: u32 = 1234567;  // In real impl, use secure random
    
    // Compute checksum digit
    let mut accum = 0u32;
    let mut temp = pin;
    while temp > 0 {
        accum += 3 * (temp % 10);
        temp /= 10;
        accum += temp % 10;
        temp /= 10;
    }
    let checksum = (10 - (accum % 10)) % 10;
    
    format!("{:07}{}", pin, checksum)
}

/// Validate WPS PIN checksum
pub fn validate_pin(pin: &str) -> bool {
    if pin.len() != 8 {
        return false;
    }
    
    let digits: Vec<u32> = pin.chars()
        .filter_map(|c| c.to_digit(10))
        .collect();
    
    if digits.len() != 8 {
        return false;
    }
    
    let mut accum = 0u32;
    accum += 3 * (digits[0] + digits[2] + digits[4] + digits[6]);
    accum += digits[1] + digits[3] + digits[5] + digits[7];
    
    accum % 10 == 0
}
