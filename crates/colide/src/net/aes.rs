// AES-128 Implementation for Colide OS WiFi
// Pure Rust implementation for bare metal - no external dependencies
// Based on FIPS 197 (AES) and RFC 3610 (CCM mode)

/// AES block size in bytes
pub const BLOCK_SIZE: usize = 16;

/// AES-128 key size in bytes
pub const KEY_SIZE: usize = 16;

/// Number of rounds for AES-128
const NR: usize = 10;

/// S-box for SubBytes transformation
const SBOX: [u8; 256] = [
    0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
    0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
    0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
    0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
    0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
    0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
    0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
    0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
    0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
    0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
    0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
    0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
    0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
    0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
    0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
    0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16,
];

/// Inverse S-box for InvSubBytes transformation
const INV_SBOX: [u8; 256] = [
    0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
    0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
    0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
    0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
    0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
    0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
    0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
    0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
    0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
    0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
    0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
    0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
    0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
    0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
    0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
    0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d,
];

/// Round constants for key expansion
const RCON: [u8; 11] = [0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36];

/// AES-128 cipher context
#[derive(Clone)]
pub struct Aes128 {
    round_keys: [[u8; 16]; NR + 1],
}

impl Aes128 {
    /// Create new AES-128 context with key
    pub fn new(key: &[u8; KEY_SIZE]) -> Self {
        let mut ctx = Self {
            round_keys: [[0u8; 16]; NR + 1],
        };
        ctx.key_expansion(key);
        ctx
    }
    
    /// Key expansion to generate round keys
    fn key_expansion(&mut self, key: &[u8; KEY_SIZE]) {
        // First round key is the key itself
        self.round_keys[0].copy_from_slice(key);
        
        let mut temp = [0u8; 4];
        
        for i in 1..=NR {
            // Get last 4 bytes of previous round key
            temp.copy_from_slice(&self.round_keys[i - 1][12..16]);
            
            // RotWord
            temp.rotate_left(1);
            
            // SubWord
            for j in 0..4 {
                temp[j] = SBOX[temp[j] as usize];
            }
            
            // XOR with Rcon
            temp[0] ^= RCON[i];
            
            // Generate round key
            for j in 0..4 {
                self.round_keys[i][j] = self.round_keys[i - 1][j] ^ temp[j];
            }
            for j in 4..16 {
                self.round_keys[i][j] = self.round_keys[i - 1][j] ^ self.round_keys[i][j - 4];
            }
        }
    }
    
    /// Encrypt a single 16-byte block
    pub fn encrypt_block(&self, plaintext: &[u8; BLOCK_SIZE]) -> [u8; BLOCK_SIZE] {
        let mut state = *plaintext;
        
        // Initial round key addition
        self.add_round_key(&mut state, 0);
        
        // Main rounds
        for round in 1..NR {
            self.sub_bytes(&mut state);
            self.shift_rows(&mut state);
            self.mix_columns(&mut state);
            self.add_round_key(&mut state, round);
        }
        
        // Final round (no MixColumns)
        self.sub_bytes(&mut state);
        self.shift_rows(&mut state);
        self.add_round_key(&mut state, NR);
        
        state
    }
    
    /// Decrypt a single 16-byte block
    pub fn decrypt_block(&self, ciphertext: &[u8; BLOCK_SIZE]) -> [u8; BLOCK_SIZE] {
        let mut state = *ciphertext;
        
        // Initial round key addition
        self.add_round_key(&mut state, NR);
        
        // Main rounds (in reverse)
        for round in (1..NR).rev() {
            self.inv_shift_rows(&mut state);
            self.inv_sub_bytes(&mut state);
            self.add_round_key(&mut state, round);
            self.inv_mix_columns(&mut state);
        }
        
        // Final round
        self.inv_shift_rows(&mut state);
        self.inv_sub_bytes(&mut state);
        self.add_round_key(&mut state, 0);
        
        state
    }
    
    /// SubBytes transformation
    fn sub_bytes(&self, state: &mut [u8; 16]) {
        for byte in state.iter_mut() {
            *byte = SBOX[*byte as usize];
        }
    }
    
    /// Inverse SubBytes transformation
    fn inv_sub_bytes(&self, state: &mut [u8; 16]) {
        for byte in state.iter_mut() {
            *byte = INV_SBOX[*byte as usize];
        }
    }
    
    /// ShiftRows transformation
    fn shift_rows(&self, state: &mut [u8; 16]) {
        // Row 1: shift left by 1
        let temp = state[1];
        state[1] = state[5];
        state[5] = state[9];
        state[9] = state[13];
        state[13] = temp;
        
        // Row 2: shift left by 2
        state.swap(2, 10);
        state.swap(6, 14);
        
        // Row 3: shift left by 3 (= shift right by 1)
        let temp = state[15];
        state[15] = state[11];
        state[11] = state[7];
        state[7] = state[3];
        state[3] = temp;
    }
    
    /// Inverse ShiftRows transformation
    fn inv_shift_rows(&self, state: &mut [u8; 16]) {
        // Row 1: shift right by 1
        let temp = state[13];
        state[13] = state[9];
        state[9] = state[5];
        state[5] = state[1];
        state[1] = temp;
        
        // Row 2: shift right by 2
        state.swap(2, 10);
        state.swap(6, 14);
        
        // Row 3: shift right by 3 (= shift left by 1)
        let temp = state[3];
        state[3] = state[7];
        state[7] = state[11];
        state[11] = state[15];
        state[15] = temp;
    }
    
    /// MixColumns transformation
    fn mix_columns(&self, state: &mut [u8; 16]) {
        for col in 0..4 {
            let i = col * 4;
            let a = state[i];
            let b = state[i + 1];
            let c = state[i + 2];
            let d = state[i + 3];
            
            state[i] = gf_mul(a, 2) ^ gf_mul(b, 3) ^ c ^ d;
            state[i + 1] = a ^ gf_mul(b, 2) ^ gf_mul(c, 3) ^ d;
            state[i + 2] = a ^ b ^ gf_mul(c, 2) ^ gf_mul(d, 3);
            state[i + 3] = gf_mul(a, 3) ^ b ^ c ^ gf_mul(d, 2);
        }
    }
    
    /// Inverse MixColumns transformation
    fn inv_mix_columns(&self, state: &mut [u8; 16]) {
        for col in 0..4 {
            let i = col * 4;
            let a = state[i];
            let b = state[i + 1];
            let c = state[i + 2];
            let d = state[i + 3];
            
            state[i] = gf_mul(a, 0x0e) ^ gf_mul(b, 0x0b) ^ gf_mul(c, 0x0d) ^ gf_mul(d, 0x09);
            state[i + 1] = gf_mul(a, 0x09) ^ gf_mul(b, 0x0e) ^ gf_mul(c, 0x0b) ^ gf_mul(d, 0x0d);
            state[i + 2] = gf_mul(a, 0x0d) ^ gf_mul(b, 0x09) ^ gf_mul(c, 0x0e) ^ gf_mul(d, 0x0b);
            state[i + 3] = gf_mul(a, 0x0b) ^ gf_mul(b, 0x0d) ^ gf_mul(c, 0x09) ^ gf_mul(d, 0x0e);
        }
    }
    
    /// AddRoundKey transformation
    fn add_round_key(&self, state: &mut [u8; 16], round: usize) {
        for (i, byte) in state.iter_mut().enumerate() {
            *byte ^= self.round_keys[round][i];
        }
    }
}

/// Galois Field multiplication
fn gf_mul(a: u8, b: u8) -> u8 {
    let mut result = 0u8;
    let mut a = a;
    let mut b = b;
    
    while b != 0 {
        if b & 1 != 0 {
            result ^= a;
        }
        let hi_bit = a & 0x80;
        a <<= 1;
        if hi_bit != 0 {
            a ^= 0x1b;  // x^8 + x^4 + x^3 + x + 1
        }
        b >>= 1;
    }
    
    result
}

/// AES-CCM mode for WiFi frame encryption (IEEE 802.11i)
pub struct AesCcm {
    aes: Aes128,
    mic_len: usize,  // 8 bytes for WiFi
}

impl AesCcm {
    /// Create new AES-CCM context
    pub fn new(key: &[u8; KEY_SIZE], mic_len: usize) -> Self {
        Self {
            aes: Aes128::new(key),
            mic_len,
        }
    }
    
    /// Encrypt with CCM mode
    /// Returns ciphertext || MIC
    pub fn encrypt(&self, nonce: &[u8], aad: &[u8], plaintext: &[u8]) -> Vec<u8> {
        // Calculate MIC over AAD and plaintext
        let mic = self.compute_mic(nonce, aad, plaintext);
        
        // CTR encrypt plaintext
        let mut ciphertext = self.ctr_encrypt(nonce, plaintext, 1);
        
        // CTR encrypt MIC with counter 0
        let encrypted_mic = self.ctr_encrypt(nonce, &mic, 0);
        
        ciphertext.extend_from_slice(&encrypted_mic[..self.mic_len]);
        ciphertext
    }
    
    /// Decrypt with CCM mode
    /// Returns Some(plaintext) if MIC verification passes, None otherwise
    pub fn decrypt(&self, nonce: &[u8], aad: &[u8], ciphertext: &[u8]) -> Option<Vec<u8>> {
        if ciphertext.len() < self.mic_len {
            return None;
        }
        
        let data_len = ciphertext.len() - self.mic_len;
        let encrypted_data = &ciphertext[..data_len];
        let received_mic = &ciphertext[data_len..];
        
        // CTR decrypt plaintext
        let plaintext = self.ctr_encrypt(nonce, encrypted_data, 1);
        
        // Calculate expected MIC
        let expected_mic = self.compute_mic(nonce, aad, &plaintext);
        
        // CTR encrypt expected MIC with counter 0
        let encrypted_expected_mic = self.ctr_encrypt(nonce, &expected_mic, 0);
        
        // Constant-time comparison
        let mut diff = 0u8;
        for i in 0..self.mic_len {
            diff |= received_mic[i] ^ encrypted_expected_mic[i];
        }
        
        if diff == 0 {
            Some(plaintext)
        } else {
            None
        }
    }
    
    /// CTR mode encryption/decryption
    fn ctr_encrypt(&self, nonce: &[u8], data: &[u8], start_counter: u16) -> Vec<u8> {
        let mut result = Vec::with_capacity(data.len());
        let mut counter = start_counter;
        
        for chunk in data.chunks(BLOCK_SIZE) {
            let ctr_block = self.format_ctr_block(nonce, counter);
            let keystream = self.aes.encrypt_block(&ctr_block);
            
            for (i, &byte) in chunk.iter().enumerate() {
                result.push(byte ^ keystream[i]);
            }
            
            counter = counter.wrapping_add(1);
        }
        
        result
    }
    
    /// Compute MIC using CBC-MAC
    fn compute_mic(&self, nonce: &[u8], aad: &[u8], plaintext: &[u8]) -> Vec<u8> {
        let mut cbc_state = [0u8; BLOCK_SIZE];
        
        // Format B0 block
        let b0 = self.format_b0(nonce, aad.len(), plaintext.len());
        
        // First CBC-MAC block
        for i in 0..BLOCK_SIZE {
            cbc_state[i] ^= b0[i];
        }
        cbc_state = self.aes.encrypt_block(&cbc_state);
        
        // Process AAD if present
        if !aad.is_empty() {
            let mut aad_block = [0u8; BLOCK_SIZE];
            
            // First AAD block includes length encoding
            let aad_len = aad.len();
            if aad_len < 0xFF00 {
                aad_block[0] = (aad_len >> 8) as u8;
                aad_block[1] = aad_len as u8;
                let copy_len = (aad_len).min(BLOCK_SIZE - 2);
                aad_block[2..2 + copy_len].copy_from_slice(&aad[..copy_len]);
            }
            
            for i in 0..BLOCK_SIZE {
                cbc_state[i] ^= aad_block[i];
            }
            cbc_state = self.aes.encrypt_block(&cbc_state);
            
            // Process remaining AAD blocks
            let mut offset = BLOCK_SIZE - 2;
            while offset < aad.len() {
                let mut block = [0u8; BLOCK_SIZE];
                let copy_len = (aad.len() - offset).min(BLOCK_SIZE);
                block[..copy_len].copy_from_slice(&aad[offset..offset + copy_len]);
                
                for i in 0..BLOCK_SIZE {
                    cbc_state[i] ^= block[i];
                }
                cbc_state = self.aes.encrypt_block(&cbc_state);
                
                offset += BLOCK_SIZE;
            }
        }
        
        // Process plaintext
        for chunk in plaintext.chunks(BLOCK_SIZE) {
            let mut block = [0u8; BLOCK_SIZE];
            block[..chunk.len()].copy_from_slice(chunk);
            
            for i in 0..BLOCK_SIZE {
                cbc_state[i] ^= block[i];
            }
            cbc_state = self.aes.encrypt_block(&cbc_state);
        }
        
        cbc_state[..self.mic_len].to_vec()
    }
    
    /// Format B0 block for CBC-MAC
    fn format_b0(&self, nonce: &[u8], aad_len: usize, plaintext_len: usize) -> [u8; BLOCK_SIZE] {
        let mut b0 = [0u8; BLOCK_SIZE];
        
        // Flags byte: Reserved(1) | Adata(1) | M'(3) | L'(3)
        let adata = if aad_len > 0 { 1 } else { 0 };
        let m_prime = ((self.mic_len - 2) / 2) as u8;
        let l_prime = 1u8;  // L = 2 for WiFi (16-bit counter)
        
        b0[0] = (adata << 6) | (m_prime << 3) | l_prime;
        
        // Nonce (13 bytes for WiFi CCM)
        let nonce_len = nonce.len().min(13);
        b0[1..1 + nonce_len].copy_from_slice(&nonce[..nonce_len]);
        
        // Message length (2 bytes for L=2)
        b0[14] = (plaintext_len >> 8) as u8;
        b0[15] = plaintext_len as u8;
        
        b0
    }
    
    /// Format CTR block
    fn format_ctr_block(&self, nonce: &[u8], counter: u16) -> [u8; BLOCK_SIZE] {
        let mut ctr = [0u8; BLOCK_SIZE];
        
        // Flags byte: L' = 1 (L = 2)
        ctr[0] = 1;
        
        // Nonce
        let nonce_len = nonce.len().min(13);
        ctr[1..1 + nonce_len].copy_from_slice(&nonce[..nonce_len]);
        
        // Counter (2 bytes)
        ctr[14] = (counter >> 8) as u8;
        ctr[15] = counter as u8;
        
        ctr
    }
}

/// AES Key Wrap (RFC 3394) for GTK encryption
pub fn aes_key_wrap(kek: &[u8; KEY_SIZE], plaintext: &[u8]) -> Vec<u8> {
    if plaintext.len() % 8 != 0 || plaintext.is_empty() {
        return Vec::new();
    }
    
    let aes = Aes128::new(kek);
    let n = plaintext.len() / 8;
    
    // Initialize
    let mut a = [0xA6u8; 8];
    let mut r: Vec<[u8; 8]> = plaintext.chunks(8)
        .map(|c| {
            let mut block = [0u8; 8];
            block.copy_from_slice(c);
            block
        })
        .collect();
    
    // Wrap rounds
    for j in 0..6 {
        for i in 0..n {
            let mut input = [0u8; 16];
            input[0..8].copy_from_slice(&a);
            input[8..16].copy_from_slice(&r[i]);
            
            let output = aes.encrypt_block(&input);
            
            let t = ((n * j + i + 1) as u64).to_be_bytes();
            for k in 0..8 {
                a[k] = output[k] ^ t[k];
            }
            r[i].copy_from_slice(&output[8..16]);
        }
    }
    
    // Output C = A || R1 || R2 || ... || Rn
    let mut result = Vec::with_capacity(8 + n * 8);
    result.extend_from_slice(&a);
    for block in r {
        result.extend_from_slice(&block);
    }
    result
}

/// AES Key Unwrap (RFC 3394) for GTK decryption
pub fn aes_key_unwrap(kek: &[u8; KEY_SIZE], ciphertext: &[u8]) -> Option<Vec<u8>> {
    if ciphertext.len() < 24 || ciphertext.len() % 8 != 0 {
        return None;
    }
    
    let aes = Aes128::new(kek);
    let n = ciphertext.len() / 8 - 1;
    
    // Initialize
    let mut a = [0u8; 8];
    a.copy_from_slice(&ciphertext[0..8]);
    
    let mut r: Vec<[u8; 8]> = ciphertext[8..].chunks(8)
        .map(|c| {
            let mut block = [0u8; 8];
            block.copy_from_slice(c);
            block
        })
        .collect();
    
    // Unwrap rounds
    for j in (0..6).rev() {
        for i in (0..n).rev() {
            let t = ((n * j + i + 1) as u64).to_be_bytes();
            for k in 0..8 {
                a[k] ^= t[k];
            }
            
            let mut input = [0u8; 16];
            input[0..8].copy_from_slice(&a);
            input[8..16].copy_from_slice(&r[i]);
            
            let output = aes.decrypt_block(&input);
            
            a.copy_from_slice(&output[0..8]);
            r[i].copy_from_slice(&output[8..16]);
        }
    }
    
    // Verify integrity
    if a != [0xA6u8; 8] {
        return None;
    }
    
    // Output P = R1 || R2 || ... || Rn
    let mut result = Vec::with_capacity(n * 8);
    for block in r {
        result.extend_from_slice(&block);
    }
    Some(result)
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_aes_128_encrypt() {
        // NIST test vector
        let key = [
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        ];
        let plaintext = [
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            0x88, 0x99, 0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff,
        ];
        let expected = [
            0x69, 0xc4, 0xe0, 0xd8, 0x6a, 0x7b, 0x04, 0x30,
            0xd8, 0xcd, 0xb7, 0x80, 0x70, 0xb4, 0xc5, 0x5a,
        ];
        
        let aes = Aes128::new(&key);
        let ciphertext = aes.encrypt_block(&plaintext);
        assert_eq!(ciphertext, expected);
    }
    
    #[test]
    fn test_aes_128_decrypt() {
        let key = [
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        ];
        let ciphertext = [
            0x69, 0xc4, 0xe0, 0xd8, 0x6a, 0x7b, 0x04, 0x30,
            0xd8, 0xcd, 0xb7, 0x80, 0x70, 0xb4, 0xc5, 0x5a,
        ];
        let expected = [
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            0x88, 0x99, 0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff,
        ];
        
        let aes = Aes128::new(&key);
        let plaintext = aes.decrypt_block(&ciphertext);
        assert_eq!(plaintext, expected);
    }
    
    #[test]
    fn test_aes_roundtrip() {
        let key = [0x42u8; 16];
        let plaintext = [0x55u8; 16];
        
        let aes = Aes128::new(&key);
        let ciphertext = aes.encrypt_block(&plaintext);
        let decrypted = aes.decrypt_block(&ciphertext);
        
        assert_eq!(decrypted, plaintext);
    }
    
    #[test]
    fn test_aes_ccm_roundtrip() {
        let key = [0x42u8; 16];
        let nonce = [0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d];
        let aad = b"additional authenticated data";
        let plaintext = b"This is a test message for WiFi encryption!";
        
        let ccm = AesCcm::new(&key, 8);
        let ciphertext = ccm.encrypt(&nonce, aad, plaintext);
        let decrypted = ccm.decrypt(&nonce, aad, &ciphertext);
        
        assert!(decrypted.is_some());
        assert_eq!(decrypted.unwrap(), plaintext);
    }
    
    #[test]
    fn test_aes_ccm_tampered() {
        let key = [0x42u8; 16];
        let nonce = [0x01u8; 13];
        let aad = b"aad";
        let plaintext = b"secret";
        
        let ccm = AesCcm::new(&key, 8);
        let mut ciphertext = ccm.encrypt(&nonce, aad, plaintext);
        
        // Tamper with ciphertext
        ciphertext[0] ^= 0x01;
        
        let result = ccm.decrypt(&nonce, aad, &ciphertext);
        assert!(result.is_none());
    }
    
    #[test]
    fn test_aes_key_wrap() {
        // RFC 3394 test vector
        let kek = [
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
        ];
        let plaintext = [
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            0x88, 0x99, 0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff,
        ];
        
        let wrapped = aes_key_wrap(&kek, &plaintext);
        let unwrapped = aes_key_unwrap(&kek, &wrapped);
        
        assert!(unwrapped.is_some());
        assert_eq!(unwrapped.unwrap(), plaintext);
    }
    
    #[test]
    fn test_gf_mul() {
        // Test known GF(2^8) multiplications
        assert_eq!(gf_mul(0x57, 0x13), 0xfe);
        assert_eq!(gf_mul(0x02, 0x87), 0x15);
    }
}
