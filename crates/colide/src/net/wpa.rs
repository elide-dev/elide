// WPA2-PSK Implementation for Colide OS
// Minimal implementation for connecting to WPA2-Personal networks
// Based on IEEE 802.11i and RFC 4764

use super::linux_compat::SpinLock;
use super::aes::{Aes128, AesCcm, aes_key_unwrap as aes_unwrap_impl, KEY_SIZE};

/// WPA versions
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WpaVersion {
    Wpa1 = 1,
    Wpa2 = 2,
    Wpa3 = 3,
}

/// Key management types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum KeyMgmt {
    Psk,        // WPA2-Personal
    PskSha256,  // WPA2-Personal with SHA256
    Sae,        // WPA3-Personal (SAE)
    Eap,        // WPA2-Enterprise
    FtPsk,      // Fast Transition with PSK
    FtSae,      // Fast Transition with SAE
}

/// Pairwise cipher suites
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PairwiseCipher {
    None,
    Wep40,
    Wep104,
    Tkip,
    Ccmp,       // AES-CCMP (WPA2)
    Gcmp,       // AES-GCMP (WPA3)
    Ccmp256,
    Gcmp256,
}

/// Group cipher suites
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum GroupCipher {
    None,
    Wep40,
    Wep104,
    Tkip,
    Ccmp,
    Gcmp,
}

/// EAPOL key types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum EapolKeyType {
    Rc4 = 1,
    Rsc = 2,   // RSN (WPA2)
}

/// EAPOL-Key frame
#[derive(Debug, Clone)]
pub struct EapolKeyFrame {
    pub protocol_version: u8,
    pub packet_type: u8,      // 3 = EAPOL-Key
    pub packet_body_length: u16,
    pub descriptor_type: u8,  // 2 = RSN
    pub key_info: KeyInfo,
    pub key_length: u16,
    pub replay_counter: u64,
    pub key_nonce: [u8; 32],
    pub key_iv: [u8; 16],
    pub key_rsc: [u8; 8],
    pub key_id: [u8; 8],
    pub key_mic: [u8; 16],
    pub key_data_length: u16,
    pub key_data: Vec<u8>,
}

impl EapolKeyFrame {
    pub const PROTOCOL_VERSION: u8 = 2;  // 802.1X-2004
    pub const PACKET_TYPE_KEY: u8 = 3;
    pub const DESCRIPTOR_TYPE_RSN: u8 = 2;
    
    pub fn new() -> Self {
        Self {
            protocol_version: Self::PROTOCOL_VERSION,
            packet_type: Self::PACKET_TYPE_KEY,
            packet_body_length: 0,
            descriptor_type: Self::DESCRIPTOR_TYPE_RSN,
            key_info: KeyInfo::default(),
            key_length: 0,
            replay_counter: 0,
            key_nonce: [0u8; 32],
            key_iv: [0u8; 16],
            key_rsc: [0u8; 8],
            key_id: [0u8; 8],
            key_mic: [0u8; 16],
            key_data_length: 0,
            key_data: Vec::new(),
        }
    }
    
    /// Parse EAPOL-Key frame from bytes
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 99 {  // Minimum EAPOL-Key frame size
            return None;
        }
        
        let protocol_version = data[0];
        let packet_type = data[1];
        if packet_type != Self::PACKET_TYPE_KEY {
            return None;
        }
        
        let packet_body_length = u16::from_be_bytes([data[2], data[3]]);
        let descriptor_type = data[4];
        
        let key_info_raw = u16::from_be_bytes([data[5], data[6]]);
        let key_info = KeyInfo::from_u16(key_info_raw);
        
        let key_length = u16::from_be_bytes([data[7], data[8]]);
        let replay_counter = u64::from_be_bytes([
            data[9], data[10], data[11], data[12],
            data[13], data[14], data[15], data[16],
        ]);
        
        let mut key_nonce = [0u8; 32];
        key_nonce.copy_from_slice(&data[17..49]);
        
        let mut key_iv = [0u8; 16];
        key_iv.copy_from_slice(&data[49..65]);
        
        let mut key_rsc = [0u8; 8];
        key_rsc.copy_from_slice(&data[65..73]);
        
        let mut key_id = [0u8; 8];
        key_id.copy_from_slice(&data[73..81]);
        
        let mut key_mic = [0u8; 16];
        key_mic.copy_from_slice(&data[81..97]);
        
        let key_data_length = u16::from_be_bytes([data[97], data[98]]);
        
        let key_data = if key_data_length > 0 && data.len() >= 99 + key_data_length as usize {
            data[99..99 + key_data_length as usize].to_vec()
        } else {
            Vec::new()
        };
        
        Some(Self {
            protocol_version,
            packet_type,
            packet_body_length,
            descriptor_type,
            key_info,
            key_length,
            replay_counter,
            key_nonce,
            key_iv,
            key_rsc,
            key_id,
            key_mic,
            key_data_length,
            key_data,
        })
    }
    
    /// Serialize to bytes
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut result = Vec::with_capacity(99 + self.key_data.len());
        
        result.push(self.protocol_version);
        result.push(self.packet_type);
        result.extend_from_slice(&self.packet_body_length.to_be_bytes());
        result.push(self.descriptor_type);
        result.extend_from_slice(&self.key_info.to_u16().to_be_bytes());
        result.extend_from_slice(&self.key_length.to_be_bytes());
        result.extend_from_slice(&self.replay_counter.to_be_bytes());
        result.extend_from_slice(&self.key_nonce);
        result.extend_from_slice(&self.key_iv);
        result.extend_from_slice(&self.key_rsc);
        result.extend_from_slice(&self.key_id);
        result.extend_from_slice(&self.key_mic);
        result.extend_from_slice(&self.key_data_length.to_be_bytes());
        result.extend_from_slice(&self.key_data);
        
        result
    }
}

/// Key information field in EAPOL-Key
#[derive(Debug, Clone, Copy, Default)]
pub struct KeyInfo {
    pub key_descriptor_version: u8,  // 1=HMAC-MD5/RC4, 2=HMAC-SHA1/AES
    pub key_type: bool,              // 0=group, 1=pairwise
    pub key_index: u8,               // For group keys
    pub install: bool,               // Install key
    pub key_ack: bool,               // Key ACK (AP sets)
    pub key_mic: bool,               // MIC present
    pub secure: bool,                // Secure bit (after 4-way done)
    pub error: bool,                 // Error detected
    pub request: bool,               // Request (STA sets)
    pub encrypted_key_data: bool,    // Key data encrypted
    pub smk_message: bool,           // SMK Message
}

impl KeyInfo {
    pub fn from_u16(val: u16) -> Self {
        Self {
            key_descriptor_version: (val & 0x07) as u8,
            key_type: (val & 0x08) != 0,
            key_index: ((val >> 4) & 0x03) as u8,
            install: (val & 0x40) != 0,
            key_ack: (val & 0x80) != 0,
            key_mic: (val & 0x100) != 0,
            secure: (val & 0x200) != 0,
            error: (val & 0x400) != 0,
            request: (val & 0x800) != 0,
            encrypted_key_data: (val & 0x1000) != 0,
            smk_message: (val & 0x2000) != 0,
        }
    }
    
    pub fn to_u16(&self) -> u16 {
        let mut val = self.key_descriptor_version as u16;
        if self.key_type { val |= 0x08; }
        val |= (self.key_index as u16 & 0x03) << 4;
        if self.install { val |= 0x40; }
        if self.key_ack { val |= 0x80; }
        if self.key_mic { val |= 0x100; }
        if self.secure { val |= 0x200; }
        if self.error { val |= 0x400; }
        if self.request { val |= 0x800; }
        if self.encrypted_key_data { val |= 0x1000; }
        if self.smk_message { val |= 0x2000; }
        val
    }
}

/// PMK (Pairwise Master Key) - derived from passphrase
pub const PMK_LEN: usize = 32;

/// PTK (Pairwise Transient Key) components
pub const PTK_KCK_LEN: usize = 16;   // Key Confirmation Key
pub const PTK_KEK_LEN: usize = 16;   // Key Encryption Key
pub const PTK_TK_LEN: usize = 16;    // Temporal Key (for CCMP)

/// GTK (Group Temporal Key)
pub const GTK_LEN: usize = 16;       // For CCMP

/// Nonce length
pub const NONCE_LEN: usize = 32;

/// WPA2-PSK state machine states
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WpaState {
    Disconnected,
    Associating,
    Associated,
    FourWayMsg1,
    FourWayMsg2Sent,
    FourWayMsg3,
    FourWayMsg4Sent,
    GroupMsg1,
    GroupMsg2Sent,
    Completed,
    Failed,
}

/// WPA2-PSK supplicant context
pub struct WpaSupplicant {
    pub state: WpaState,
    pub ssid: [u8; 32],
    pub ssid_len: usize,
    pub bssid: [u8; 6],
    pub own_addr: [u8; 6],
    pub pmk: [u8; PMK_LEN],
    pub ptk_kck: [u8; PTK_KCK_LEN],
    pub ptk_kek: [u8; PTK_KEK_LEN],
    pub ptk_tk: [u8; PTK_TK_LEN],
    pub gtk: [u8; GTK_LEN],
    pub gtk_idx: u8,
    pub anonce: [u8; NONCE_LEN],     // AP nonce (from Msg1)
    pub snonce: [u8; NONCE_LEN],     // Our nonce (we generate)
    pub replay_counter: u64,
    pub key_mgmt: KeyMgmt,
    pub pairwise_cipher: PairwiseCipher,
    pub group_cipher: GroupCipher,
}

impl WpaSupplicant {
    pub fn new() -> Self {
        Self {
            state: WpaState::Disconnected,
            ssid: [0u8; 32],
            ssid_len: 0,
            bssid: [0u8; 6],
            own_addr: [0u8; 6],
            pmk: [0u8; PMK_LEN],
            ptk_kck: [0u8; PTK_KCK_LEN],
            ptk_kek: [0u8; PTK_KEK_LEN],
            ptk_tk: [0u8; PTK_TK_LEN],
            gtk: [0u8; GTK_LEN],
            gtk_idx: 0,
            anonce: [0u8; NONCE_LEN],
            snonce: [0u8; NONCE_LEN],
            replay_counter: 0,
            key_mgmt: KeyMgmt::Psk,
            pairwise_cipher: PairwiseCipher::Ccmp,
            group_cipher: GroupCipher::Ccmp,
        }
    }
    
    /// Set credentials (SSID and passphrase)
    pub fn set_credentials(&mut self, ssid: &[u8], passphrase: &str) {
        let len = ssid.len().min(32);
        self.ssid[..len].copy_from_slice(&ssid[..len]);
        self.ssid_len = len;
        
        // Derive PMK from passphrase using PBKDF2-SHA1
        self.pmk = pbkdf2_sha1(passphrase.as_bytes(), &self.ssid[..self.ssid_len], 4096);
    }
    
    /// Set target BSS
    pub fn set_bss(&mut self, bssid: [u8; 6], own_addr: [u8; 6]) {
        self.bssid = bssid;
        self.own_addr = own_addr;
    }
    
    /// Generate random SNonce
    pub fn generate_snonce(&mut self) {
        // In real implementation, use proper CSPRNG
        // For now, use a simple PRNG seeded from system state
        let seed = unsafe {
            core::arch::x86_64::_rdtsc() as u64
        };
        let mut state = seed;
        for i in 0..32 {
            state = state.wrapping_mul(6364136223846793005).wrapping_add(1);
            self.snonce[i] = (state >> 56) as u8;
        }
    }
    
    /// Process received EAPOL-Key frame
    pub fn process_eapol(&mut self, frame: &EapolKeyFrame) -> Option<EapolKeyFrame> {
        match self.state {
            WpaState::Associated | WpaState::FourWayMsg1 => {
                // Expect Message 1 (ANonce from AP)
                if frame.key_info.key_ack && !frame.key_info.key_mic {
                    return self.handle_msg1(frame);
                }
            }
            WpaState::FourWayMsg2Sent | WpaState::FourWayMsg3 => {
                // Expect Message 3 (Install key)
                if frame.key_info.key_ack && frame.key_info.key_mic && frame.key_info.install {
                    return self.handle_msg3(frame);
                }
            }
            WpaState::FourWayMsg4Sent | WpaState::GroupMsg1 => {
                // Expect Group Key Message 1
                if !frame.key_info.key_type && frame.key_info.key_ack {
                    return self.handle_group_msg1(frame);
                }
            }
            _ => {}
        }
        None
    }
    
    /// Handle 4-Way Handshake Message 1
    fn handle_msg1(&mut self, frame: &EapolKeyFrame) -> Option<EapolKeyFrame> {
        // Save ANonce
        self.anonce = frame.key_nonce;
        self.replay_counter = frame.replay_counter;
        
        // Generate our SNonce
        self.generate_snonce();
        
        // Derive PTK
        self.derive_ptk();
        
        // Build Message 2
        let msg2 = self.build_msg2();
        
        self.state = WpaState::FourWayMsg2Sent;
        Some(msg2)
    }
    
    /// Handle 4-Way Handshake Message 3
    fn handle_msg3(&mut self, frame: &EapolKeyFrame) -> Option<EapolKeyFrame> {
        // Verify MIC
        if !self.verify_mic(frame) {
            self.state = WpaState::Failed;
            return None;
        }
        
        // Extract and install GTK from encrypted key data
        if frame.key_data_length > 0 {
            self.decrypt_and_install_gtk(&frame.key_data);
        }
        
        self.replay_counter = frame.replay_counter;
        
        // Build Message 4
        let msg4 = self.build_msg4();
        
        self.state = WpaState::FourWayMsg4Sent;
        Some(msg4)
    }
    
    /// Handle Group Key Handshake Message 1
    fn handle_group_msg1(&mut self, frame: &EapolKeyFrame) -> Option<EapolKeyFrame> {
        // Verify MIC
        if !self.verify_mic(frame) {
            return None;
        }
        
        // Decrypt and install new GTK
        if frame.key_data_length > 0 {
            self.decrypt_and_install_gtk(&frame.key_data);
        }
        
        self.replay_counter = frame.replay_counter;
        
        // Build Group Message 2
        let msg2 = self.build_group_msg2();
        
        self.state = WpaState::Completed;
        Some(msg2)
    }
    
    /// Derive PTK from PMK, addresses, and nonces
    fn derive_ptk(&mut self) {
        // PTK = PRF-X(PMK, "Pairwise key expansion", Min(AA,SPA) || Max(AA,SPA) || Min(ANonce,SNonce) || Max(ANonce,SNonce))
        
        let mut data = Vec::with_capacity(76);
        
        // Sort addresses (AA = authenticator, SPA = supplicant)
        if self.bssid < self.own_addr {
            data.extend_from_slice(&self.bssid);
            data.extend_from_slice(&self.own_addr);
        } else {
            data.extend_from_slice(&self.own_addr);
            data.extend_from_slice(&self.bssid);
        }
        
        // Sort nonces
        if self.anonce < self.snonce {
            data.extend_from_slice(&self.anonce);
            data.extend_from_slice(&self.snonce);
        } else {
            data.extend_from_slice(&self.snonce);
            data.extend_from_slice(&self.anonce);
        }
        
        // PRF-384 for CCMP (KCK + KEK + TK = 48 bytes)
        let ptk = prf_sha1(&self.pmk, b"Pairwise key expansion", &data, 48);
        
        self.ptk_kck.copy_from_slice(&ptk[0..16]);
        self.ptk_kek.copy_from_slice(&ptk[16..32]);
        self.ptk_tk.copy_from_slice(&ptk[32..48]);
    }
    
    /// Build Message 2 of 4-Way Handshake
    fn build_msg2(&self) -> EapolKeyFrame {
        let mut frame = EapolKeyFrame::new();
        
        frame.key_info = KeyInfo {
            key_descriptor_version: 2,  // HMAC-SHA1/AES
            key_type: true,             // Pairwise
            key_mic: true,
            ..Default::default()
        };
        
        frame.key_length = 0;
        frame.replay_counter = self.replay_counter;
        frame.key_nonce = self.snonce;
        
        // Add RSN IE in key data
        frame.key_data = self.build_rsn_ie();
        frame.key_data_length = frame.key_data.len() as u16;
        
        // Calculate body length
        frame.packet_body_length = 95 + frame.key_data_length;
        
        // Calculate MIC
        self.add_mic(&mut frame);
        
        frame
    }
    
    /// Build Message 4 of 4-Way Handshake
    fn build_msg4(&self) -> EapolKeyFrame {
        let mut frame = EapolKeyFrame::new();
        
        frame.key_info = KeyInfo {
            key_descriptor_version: 2,
            key_type: true,             // Pairwise
            key_mic: true,
            secure: true,
            ..Default::default()
        };
        
        frame.replay_counter = self.replay_counter;
        frame.packet_body_length = 95;
        
        self.add_mic(&mut frame);
        
        frame
    }
    
    /// Build Group Key Message 2
    fn build_group_msg2(&self) -> EapolKeyFrame {
        let mut frame = EapolKeyFrame::new();
        
        frame.key_info = KeyInfo {
            key_descriptor_version: 2,
            key_type: false,            // Group
            key_mic: true,
            secure: true,
            ..Default::default()
        };
        
        frame.replay_counter = self.replay_counter;
        frame.packet_body_length = 95;
        
        self.add_mic(&mut frame);
        
        frame
    }
    
    /// Build RSN Information Element
    fn build_rsn_ie(&self) -> Vec<u8> {
        let mut ie = Vec::with_capacity(22);
        
        ie.push(48);  // RSN IE tag
        ie.push(20);  // Length
        ie.extend_from_slice(&[1, 0]);  // Version 1
        
        // Group cipher suite
        ie.extend_from_slice(&[0x00, 0x0F, 0xAC, 0x04]);  // CCMP
        
        // Pairwise cipher suite count
        ie.extend_from_slice(&[1, 0]);
        // Pairwise cipher suite
        ie.extend_from_slice(&[0x00, 0x0F, 0xAC, 0x04]);  // CCMP
        
        // AKM suite count
        ie.extend_from_slice(&[1, 0]);
        // AKM suite
        ie.extend_from_slice(&[0x00, 0x0F, 0xAC, 0x02]);  // PSK
        
        // RSN capabilities
        ie.extend_from_slice(&[0x00, 0x00]);
        
        ie
    }
    
    /// Verify MIC on received frame
    fn verify_mic(&self, frame: &EapolKeyFrame) -> bool {
        // Create copy with zeroed MIC for verification
        let mut verify_frame = frame.clone();
        verify_frame.key_mic = [0u8; 16];
        let data = verify_frame.to_bytes();
        
        // Calculate expected MIC
        let expected_mic = hmac_sha1(&self.ptk_kck, &data);
        
        // Compare first 16 bytes of HMAC-SHA1 output
        frame.key_mic == expected_mic[0..16]
    }
    
    /// Add MIC to outgoing frame
    fn add_mic(&self, frame: &mut EapolKeyFrame) {
        frame.key_mic = [0u8; 16];
        let data = frame.to_bytes();
        let mic = hmac_sha1(&self.ptk_kck, &data);
        frame.key_mic.copy_from_slice(&mic[0..16]);
    }
    
    /// Decrypt GTK from key data (AES Key Wrap)
    fn decrypt_and_install_gtk(&mut self, encrypted_data: &[u8]) {
        // AES Key Unwrap using KEK
        if let Some(plaintext) = aes_key_unwrap(&self.ptk_kek, encrypted_data) {
            // Parse GTK KDE (Key Data Encapsulation)
            // Format: DD <len> 00-0F-AC <type> <key>
            if plaintext.len() >= 8 && plaintext[0] == 0xDD {
                let kde_len = plaintext[1] as usize;
                if kde_len >= 6 && plaintext[2..5] == [0x00, 0x0F, 0xAC] {
                    let kde_type = plaintext[5];
                    if kde_type == 1 {  // GTK KDE
                        let key_info = plaintext[6];
                        self.gtk_idx = key_info & 0x03;
                        let key_start = 8;
                        let key_len = (kde_len - 6).min(GTK_LEN);
                        if plaintext.len() >= key_start + key_len {
                            self.gtk[..key_len].copy_from_slice(&plaintext[key_start..key_start + key_len]);
                        }
                    }
                }
            }
        }
    }
    
    /// Check if handshake is complete
    pub fn is_connected(&self) -> bool {
        matches!(self.state, WpaState::Completed | WpaState::FourWayMsg4Sent)
    }
    
    /// Get temporal key for encryption
    pub fn get_tk(&self) -> Option<&[u8; PTK_TK_LEN]> {
        if self.is_connected() {
            Some(&self.ptk_tk)
        } else {
            None
        }
    }
    
    /// Get group key for broadcast decryption
    pub fn get_gtk(&self) -> Option<(&[u8; GTK_LEN], u8)> {
        if self.is_connected() {
            Some((&self.gtk, self.gtk_idx))
        } else {
            None
        }
    }
}

// ============================================================================
// Cryptographic primitives (minimal implementations for bare metal)
// In production, use hardware crypto or verified implementations
// ============================================================================

/// PBKDF2-SHA1 key derivation (RFC 2898)
/// Derives PMK from passphrase and SSID
pub fn pbkdf2_sha1(password: &[u8], salt: &[u8], iterations: u32) -> [u8; 32] {
    let mut result = [0u8; 32];
    
    // DK = T1 || T2 where Ti = F(Password, Salt, c, i)
    for block in 0..2u32 {
        let mut u = hmac_sha1_with_counter(password, salt, block + 1);
        let mut t = u;
        
        for _ in 1..iterations {
            u = hmac_sha1_raw(password, &u);
            for j in 0..20 {
                t[j] ^= u[j];
            }
        }
        
        let start = (block as usize) * 16;
        let copy_len = 16.min(32 - start);
        result[start..start + copy_len].copy_from_slice(&t[..copy_len]);
    }
    
    result
}

/// HMAC-SHA1 with counter for PBKDF2
fn hmac_sha1_with_counter(key: &[u8], salt: &[u8], counter: u32) -> [u8; 20] {
    let mut data = Vec::with_capacity(salt.len() + 4);
    data.extend_from_slice(salt);
    data.extend_from_slice(&counter.to_be_bytes());
    hmac_sha1(key, &data)
}

/// HMAC-SHA1 (RFC 2104)
pub fn hmac_sha1(key: &[u8], data: &[u8]) -> [u8; 20] {
    const BLOCK_SIZE: usize = 64;
    
    // If key > block size, hash it first
    let key_block = if key.len() > BLOCK_SIZE {
        let hashed = sha1(key);
        let mut block = [0u8; BLOCK_SIZE];
        block[..20].copy_from_slice(&hashed);
        block
    } else {
        let mut block = [0u8; BLOCK_SIZE];
        block[..key.len()].copy_from_slice(key);
        block
    };
    
    // Inner padding
    let mut ipad = [0x36u8; BLOCK_SIZE];
    for i in 0..BLOCK_SIZE {
        ipad[i] ^= key_block[i];
    }
    
    // Outer padding
    let mut opad = [0x5cu8; BLOCK_SIZE];
    for i in 0..BLOCK_SIZE {
        opad[i] ^= key_block[i];
    }
    
    // H(K XOR opad, H(K XOR ipad, text))
    let mut inner = Vec::with_capacity(BLOCK_SIZE + data.len());
    inner.extend_from_slice(&ipad);
    inner.extend_from_slice(data);
    let inner_hash = sha1(&inner);
    
    let mut outer = Vec::with_capacity(BLOCK_SIZE + 20);
    outer.extend_from_slice(&opad);
    outer.extend_from_slice(&inner_hash);
    sha1(&outer)
}

/// Raw HMAC-SHA1 for PBKDF2 iterations
fn hmac_sha1_raw(key: &[u8], data: &[u8; 20]) -> [u8; 20] {
    hmac_sha1(key, data)
}

/// PRF-SHA1 for PTK derivation (IEEE 802.11i)
pub fn prf_sha1(key: &[u8], label: &[u8], data: &[u8], output_len: usize) -> Vec<u8> {
    let mut result = Vec::with_capacity(output_len);
    let mut counter = 0u8;
    
    while result.len() < output_len {
        let mut input = Vec::with_capacity(label.len() + data.len() + 2);
        input.extend_from_slice(label);
        input.push(0);  // Null terminator
        input.extend_from_slice(data);
        input.push(counter);
        
        let hash = hmac_sha1(key, &input);
        result.extend_from_slice(&hash);
        counter += 1;
    }
    
    result.truncate(output_len);
    result
}

/// SHA-1 hash (FIPS 180-4)
/// Minimal implementation for bare metal
pub fn sha1(data: &[u8]) -> [u8; 20] {
    let mut h0: u32 = 0x67452301;
    let mut h1: u32 = 0xEFCDAB89;
    let mut h2: u32 = 0x98BADCFE;
    let mut h3: u32 = 0x10325476;
    let mut h4: u32 = 0xC3D2E1F0;
    
    // Pre-processing: pad message
    let ml = (data.len() as u64) * 8;  // Message length in bits
    let mut msg = data.to_vec();
    msg.push(0x80);
    
    while (msg.len() % 64) != 56 {
        msg.push(0);
    }
    msg.extend_from_slice(&ml.to_be_bytes());
    
    // Process each 512-bit chunk
    for chunk in msg.chunks(64) {
        let mut w = [0u32; 80];
        
        // Break chunk into 16 32-bit words
        for i in 0..16 {
            w[i] = u32::from_be_bytes([
                chunk[i * 4],
                chunk[i * 4 + 1],
                chunk[i * 4 + 2],
                chunk[i * 4 + 3],
            ]);
        }
        
        // Extend to 80 words
        for i in 16..80 {
            w[i] = (w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16]).rotate_left(1);
        }
        
        let mut a = h0;
        let mut b = h1;
        let mut c = h2;
        let mut d = h3;
        let mut e = h4;
        
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
        
        h0 = h0.wrapping_add(a);
        h1 = h1.wrapping_add(b);
        h2 = h2.wrapping_add(c);
        h3 = h3.wrapping_add(d);
        h4 = h4.wrapping_add(e);
    }
    
    let mut result = [0u8; 20];
    result[0..4].copy_from_slice(&h0.to_be_bytes());
    result[4..8].copy_from_slice(&h1.to_be_bytes());
    result[8..12].copy_from_slice(&h2.to_be_bytes());
    result[12..16].copy_from_slice(&h3.to_be_bytes());
    result[16..20].copy_from_slice(&h4.to_be_bytes());
    result
}

/// AES Key Unwrap (RFC 3394)
/// Used to decrypt GTK in EAPOL-Key messages
pub fn aes_key_unwrap(kek: &[u8], ciphertext: &[u8]) -> Option<Vec<u8>> {
    // Delegate to proper AES implementation
    if kek.len() != KEY_SIZE {
        return None;
    }
    let mut key = [0u8; KEY_SIZE];
    key.copy_from_slice(kek);
    aes_unwrap_impl(&key, ciphertext)
}

/// AES-128-CCM encryption for WiFi frames (IEEE 802.11i)
/// Uses proper AES-CCM implementation with 8-byte MIC
pub fn aes_ccm_encrypt(key: &[u8; 16], nonce: &[u8], aad: &[u8], plaintext: &[u8]) -> Vec<u8> {
    let ccm = AesCcm::new(key, 8);  // 8-byte MIC for WiFi
    ccm.encrypt(nonce, aad, plaintext)
}

/// AES-128-CCM decryption for WiFi frames (IEEE 802.11i)
/// Returns plaintext if MIC verification passes
pub fn aes_ccm_decrypt(key: &[u8; 16], nonce: &[u8], aad: &[u8], ciphertext: &[u8]) -> Option<Vec<u8>> {
    let ccm = AesCcm::new(key, 8);  // 8-byte MIC for WiFi
    ccm.decrypt(nonce, aad, ciphertext)
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_sha1() {
        // Test vector: SHA1("abc") = a9993e36...
        let result = sha1(b"abc");
        assert_eq!(result[0], 0xa9);
        assert_eq!(result[1], 0x99);
        assert_eq!(result[2], 0x3e);
        assert_eq!(result[3], 0x36);
    }
    
    #[test]
    fn test_hmac_sha1() {
        // HMAC-SHA1 test vector
        let key = b"key";
        let data = b"The quick brown fox jumps over the lazy dog";
        let result = hmac_sha1(key, data);
        // Expected: de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9
        assert_eq!(result[0], 0xde);
        assert_eq!(result[1], 0x7c);
    }
    
    #[test]
    fn test_pbkdf2_sha1() {
        // WPA2 PMK derivation test
        let password = b"password";
        let ssid = b"IEEE";
        let pmk = pbkdf2_sha1(password, ssid, 4096);
        // First 4 bytes of expected PMK
        assert_eq!(pmk[0], 0xf4);
        assert_eq!(pmk[1], 0x2c);
    }
    
    #[test]
    fn test_key_info() {
        let info = KeyInfo {
            key_descriptor_version: 2,
            key_type: true,
            key_mic: true,
            ..Default::default()
        };
        let encoded = info.to_u16();
        let decoded = KeyInfo::from_u16(encoded);
        assert_eq!(decoded.key_descriptor_version, 2);
        assert!(decoded.key_type);
        assert!(decoded.key_mic);
    }
    
    #[test]
    fn test_wpa_state_machine() {
        let mut supplicant = WpaSupplicant::new();
        supplicant.set_credentials(b"TestNetwork", "password123");
        assert_eq!(supplicant.state, WpaState::Disconnected);
        assert!(supplicant.pmk != [0u8; 32]);
    }
}
