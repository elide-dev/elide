//! Firmware Loading Infrastructure for Colide OS
//!
//! Provides firmware loading for WiFi and other hardware drivers.
//! Supports both embedded (in /zip/) and external firmware sources.
//!
//! Firmware locations (in order of preference):
//! 1. /zip/lib/firmware/  - Embedded in APE binary
//! 2. /firmware/          - External storage
//! 3. /lib/firmware/      - Linux-compatible path

use heapless::Vec as HVec;

/// Maximum firmware size (4MB should cover most WiFi firmware)
pub const MAX_FIRMWARE_SIZE: usize = 4 * 1024 * 1024;

/// Firmware loading error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum FirmwareError {
    NotFound,
    TooLarge,
    InvalidFormat,
    ChecksumMismatch,
    ParseError,
    IoError,
}

/// Firmware metadata
#[derive(Debug, Clone)]
pub struct FirmwareInfo {
    pub name: HVec<u8, 64>,
    pub version: u32,
    pub size: usize,
    pub checksum: u32,
}

/// Firmware loader trait
pub trait FirmwareLoader {
    /// Load firmware by name
    fn load(&self, name: &str) -> Result<FirmwareData, FirmwareError>;
    
    /// Check if firmware exists
    fn exists(&self, name: &str) -> bool;
    
    /// Get firmware info without loading
    fn info(&self, name: &str) -> Result<FirmwareInfo, FirmwareError>;
}

/// Loaded firmware data
pub struct FirmwareData {
    pub data: HVec<u8, MAX_FIRMWARE_SIZE>,
    pub info: FirmwareInfo,
}

impl FirmwareData {
    /// Create empty firmware data
    pub fn new() -> Self {
        Self {
            data: HVec::new(),
            info: FirmwareInfo {
                name: HVec::new(),
                version: 0,
                size: 0,
                checksum: 0,
            },
        }
    }
    
    /// Get firmware bytes
    pub fn bytes(&self) -> &[u8] {
        &self.data
    }
    
    /// Verify checksum (CRC32)
    pub fn verify_checksum(&self) -> bool {
        let calculated = crc32(&self.data);
        calculated == self.info.checksum
    }
}

impl Default for FirmwareData {
    fn default() -> Self {
        Self::new()
    }
}

/// Intel WiFi firmware header
#[derive(Debug, Clone, Copy)]
#[repr(C)]
pub struct IwlFirmwareHeader {
    pub magic: u32,           // 0x0A4C5749 ("IWL\n")
    pub version: u32,
    pub build: u32,
    pub inst_size: u32,       // Instruction section size
    pub data_size: u32,       // Data section size
    pub init_size: u32,       // Init section size
    pub init_data_size: u32,
    pub boot_size: u32,
}

impl IwlFirmwareHeader {
    pub const MAGIC: u32 = 0x0A4C5749; // "IWL\n"
    
    /// Parse header from bytes
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 32 {
            return None;
        }
        
        let magic = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
        if magic != Self::MAGIC {
            return None;
        }
        
        Some(Self {
            magic,
            version: u32::from_le_bytes([data[4], data[5], data[6], data[7]]),
            build: u32::from_le_bytes([data[8], data[9], data[10], data[11]]),
            inst_size: u32::from_le_bytes([data[12], data[13], data[14], data[15]]),
            data_size: u32::from_le_bytes([data[16], data[17], data[18], data[19]]),
            init_size: u32::from_le_bytes([data[20], data[21], data[22], data[23]]),
            init_data_size: u32::from_le_bytes([data[24], data[25], data[26], data[27]]),
            boot_size: u32::from_le_bytes([data[28], data[29], data[30], data[31]]),
        })
    }
    
    /// Total firmware size
    pub fn total_size(&self) -> usize {
        32 + self.inst_size as usize + self.data_size as usize +
            self.init_size as usize + self.init_data_size as usize +
            self.boot_size as usize
    }
}

/// Mediatek firmware header (for MT7601U, etc.)
#[derive(Debug, Clone, Copy)]
#[repr(C)]
pub struct MtFirmwareHeader {
    pub ilm_len: u32,         // Instruction memory length
    pub dlm_len: u32,         // Data memory length
    pub fw_ver: u16,
    pub build_ver: u16,
    pub extra: u32,
}

impl MtFirmwareHeader {
    /// Parse header from bytes
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 16 {
            return None;
        }
        
        Some(Self {
            ilm_len: u32::from_le_bytes([data[0], data[1], data[2], data[3]]),
            dlm_len: u32::from_le_bytes([data[4], data[5], data[6], data[7]]),
            fw_ver: u16::from_le_bytes([data[8], data[9]]),
            build_ver: u16::from_le_bytes([data[10], data[11]]),
            extra: u32::from_le_bytes([data[12], data[13], data[14], data[15]]),
        })
    }
}

/// Realtek firmware header
#[derive(Debug, Clone, Copy)]
#[repr(C)]
pub struct RtlFirmwareHeader {
    pub signature: u16,       // 0x92C1 for RTL
    pub category: u8,
    pub function: u8,
    pub version: u16,
    pub subversion: u8,
    pub reserved: u8,
    pub month: u8,
    pub day: u8,
    pub hour: u8,
    pub minute: u8,
    pub ramcode_size: u32,
}

impl RtlFirmwareHeader {
    pub const SIGNATURE: u16 = 0x92C1;
    
    /// Parse header from bytes
    pub fn parse(data: &[u8]) -> Option<Self> {
        if data.len() < 16 {
            return None;
        }
        
        let signature = u16::from_le_bytes([data[0], data[1]]);
        if signature != Self::SIGNATURE {
            return None;
        }
        
        Some(Self {
            signature,
            category: data[2],
            function: data[3],
            version: u16::from_le_bytes([data[4], data[5]]),
            subversion: data[6],
            reserved: data[7],
            month: data[8],
            day: data[9],
            hour: data[10],
            minute: data[11],
            ramcode_size: u32::from_le_bytes([data[12], data[13], data[14], data[15]]),
        })
    }
}

/// Known firmware files for WiFi chips
pub mod FirmwareFiles {
    // Intel WiFi firmware
    pub const IWL_AX200: &str = "iwlwifi-cc-a0-*.ucode";
    pub const IWL_AX210: &str = "iwlwifi-ty-a0-gf-a0-*.ucode";
    pub const IWL_AX211: &str = "iwlwifi-so-a0-gf4-a0-*.ucode";
    
    // MediaTek firmware
    pub const MT7601U: &str = "mt7601u.bin";
    pub const MT7610U: &str = "mt7610u.bin";
    pub const MT7612U: &str = "mt7612u.bin";
    
    // Realtek firmware
    pub const RTL8188EU: &str = "rtl8188eufw.bin";
    pub const RTL8192EU: &str = "rtl8192eu_nic.bin";
    pub const RTL8812AU: &str = "rtl8812au_fw.bin";
}

/// Embedded firmware loader (from /zip/)
pub struct EmbeddedFirmwareLoader;

impl EmbeddedFirmwareLoader {
    pub fn new() -> Self {
        Self
    }
    
    /// Get firmware path for embedded storage
    fn get_path(&self, name: &str) -> HVec<u8, 128> {
        let mut path = HVec::new();
        let _ = path.extend_from_slice(b"/zip/lib/firmware/");
        let _ = path.extend_from_slice(name.as_bytes());
        path
    }
}

impl Default for EmbeddedFirmwareLoader {
    fn default() -> Self {
        Self::new()
    }
}

impl FirmwareLoader for EmbeddedFirmwareLoader {
    fn load(&self, name: &str) -> Result<FirmwareData, FirmwareError> {
        let path = self.get_path(name);
        
        // Read from Cosmopolitan /zip/ filesystem using standard file operations
        // The /zip/ directory is embedded in the APE binary
        read_firmware_file(&path)
    }
    
    fn exists(&self, name: &str) -> bool {
        let path = self.get_path(name);
        file_exists(&path)
    }
    
    fn info(&self, name: &str) -> Result<FirmwareInfo, FirmwareError> {
        let path = self.get_path(name);
        get_firmware_info(&path, name)
    }
}

/// External firmware loader (from filesystem)
pub struct ExternalFirmwareLoader {
    pub base_paths: [&'static str; 3],
}

impl ExternalFirmwareLoader {
    pub fn new() -> Self {
        Self {
            base_paths: [
                "/firmware/",
                "/lib/firmware/",
                "/usr/lib/firmware/",
            ],
        }
    }
}

impl Default for ExternalFirmwareLoader {
    fn default() -> Self {
        Self::new()
    }
}

impl FirmwareLoader for ExternalFirmwareLoader {
    fn load(&self, name: &str) -> Result<FirmwareData, FirmwareError> {
        for base in &self.base_paths {
            let mut path = HVec::<u8, 128>::new();
            let _ = path.extend_from_slice(base.as_bytes());
            let _ = path.extend_from_slice(name.as_bytes());
            
            if let Ok(fw) = read_firmware_file(&path) {
                return Ok(fw);
            }
        }
        
        Err(FirmwareError::NotFound)
    }
    
    fn exists(&self, name: &str) -> bool {
        for base in &self.base_paths {
            let mut path = HVec::<u8, 128>::new();
            let _ = path.extend_from_slice(base.as_bytes());
            let _ = path.extend_from_slice(name.as_bytes());
            
            if file_exists(&path) {
                return true;
            }
        }
        false
    }
    
    fn info(&self, name: &str) -> Result<FirmwareInfo, FirmwareError> {
        for base in &self.base_paths {
            let mut path = HVec::<u8, 128>::new();
            let _ = path.extend_from_slice(base.as_bytes());
            let _ = path.extend_from_slice(name.as_bytes());
            
            if let Ok(info) = get_firmware_info(&path, name) {
                return Ok(info);
            }
        }
        Err(FirmwareError::NotFound)
    }
}

/// Combined firmware loader (tries embedded first, then external)
pub struct CombinedFirmwareLoader {
    embedded: EmbeddedFirmwareLoader,
    external: ExternalFirmwareLoader,
}

impl CombinedFirmwareLoader {
    pub fn new() -> Self {
        Self {
            embedded: EmbeddedFirmwareLoader::new(),
            external: ExternalFirmwareLoader::new(),
        }
    }
}

impl Default for CombinedFirmwareLoader {
    fn default() -> Self {
        Self::new()
    }
}

impl FirmwareLoader for CombinedFirmwareLoader {
    fn load(&self, name: &str) -> Result<FirmwareData, FirmwareError> {
        // Try embedded first
        if let Ok(fw) = self.embedded.load(name) {
            return Ok(fw);
        }
        
        // Fall back to external
        self.external.load(name)
    }
    
    fn exists(&self, name: &str) -> bool {
        self.embedded.exists(name) || self.external.exists(name)
    }
    
    fn info(&self, name: &str) -> Result<FirmwareInfo, FirmwareError> {
        self.embedded.info(name)
            .or_else(|_| self.external.info(name))
    }
}

/// Check if a file exists at the given path
fn file_exists(path: &[u8]) -> bool {
    // Use Cosmopolitan/libc stat() to check file existence
    // Path must be null-terminated
    let mut path_buf = [0u8; 129];
    let len = path.len().min(128);
    path_buf[..len].copy_from_slice(&path[..len]);
    path_buf[len] = 0; // Null terminate
    
    #[cfg(target_os = "none")]
    {
        // Bare metal: use our filesystem abstraction
        extern "C" {
            fn colide_file_exists(path: *const u8) -> i32;
        }
        unsafe { colide_file_exists(path_buf.as_ptr()) != 0 }
    }
    
    #[cfg(not(target_os = "none"))]
    {
        // Hosted: use std or libc
        use core::ffi::CStr;
        let path_cstr = unsafe { CStr::from_ptr(path_buf.as_ptr() as *const i8) };
        if let Ok(path_str) = path_cstr.to_str() {
            std::path::Path::new(path_str).exists()
        } else {
            false
        }
    }
}

/// Read firmware file from path
fn read_firmware_file(path: &[u8]) -> Result<FirmwareData, FirmwareError> {
    let mut path_buf = [0u8; 129];
    let len = path.len().min(128);
    path_buf[..len].copy_from_slice(&path[..len]);
    path_buf[len] = 0;
    
    #[cfg(target_os = "none")]
    {
        // Bare metal: use our filesystem abstraction
        extern "C" {
            fn colide_file_size(path: *const u8) -> i64;
            fn colide_file_read(path: *const u8, buf: *mut u8, size: usize) -> i64;
        }
        
        let size = unsafe { colide_file_size(path_buf.as_ptr()) };
        if size <= 0 {
            return Err(FirmwareError::NotFound);
        }
        if size as usize > MAX_FIRMWARE_SIZE {
            return Err(FirmwareError::TooLarge);
        }
        
        let mut fw = FirmwareData::new();
        fw.data.resize(size as usize, 0).map_err(|_| FirmwareError::TooLarge)?;
        
        let read = unsafe { colide_file_read(path_buf.as_ptr(), fw.data.as_mut_ptr(), size as usize) };
        if read != size {
            return Err(FirmwareError::IoError);
        }
        
        fw.info.size = size as usize;
        fw.info.checksum = crc32(&fw.data);
        Ok(fw)
    }
    
    #[cfg(not(target_os = "none"))]
    {
        use core::ffi::CStr;
        let path_cstr = unsafe { CStr::from_ptr(path_buf.as_ptr() as *const i8) };
        if let Ok(path_str) = path_cstr.to_str() {
            match std::fs::read(path_str) {
                Ok(data) => {
                    if data.len() > MAX_FIRMWARE_SIZE {
                        return Err(FirmwareError::TooLarge);
                    }
                    let mut fw = FirmwareData::new();
                    fw.data.extend_from_slice(&data).map_err(|_| FirmwareError::TooLarge)?;
                    fw.info.size = data.len();
                    fw.info.checksum = crc32(&fw.data);
                    Ok(fw)
                }
                Err(_) => Err(FirmwareError::NotFound),
            }
        } else {
            Err(FirmwareError::NotFound)
        }
    }
}

/// Get firmware info without fully loading
fn get_firmware_info(path: &[u8], name: &str) -> Result<FirmwareInfo, FirmwareError> {
    let mut path_buf = [0u8; 129];
    let len = path.len().min(128);
    path_buf[..len].copy_from_slice(&path[..len]);
    path_buf[len] = 0;
    
    #[cfg(target_os = "none")]
    {
        extern "C" {
            fn colide_file_size(path: *const u8) -> i64;
        }
        
        let size = unsafe { colide_file_size(path_buf.as_ptr()) };
        if size <= 0 {
            return Err(FirmwareError::NotFound);
        }
        
        let mut info = FirmwareInfo {
            name: HVec::new(),
            version: 0,
            size: size as usize,
            checksum: 0,
        };
        let _ = info.name.extend_from_slice(name.as_bytes());
        Ok(info)
    }
    
    #[cfg(not(target_os = "none"))]
    {
        use core::ffi::CStr;
        let path_cstr = unsafe { CStr::from_ptr(path_buf.as_ptr() as *const i8) };
        if let Ok(path_str) = path_cstr.to_str() {
            match std::fs::metadata(path_str) {
                Ok(meta) => {
                    let mut info = FirmwareInfo {
                        name: HVec::new(),
                        version: 0,
                        size: meta.len() as usize,
                        checksum: 0,
                    };
                    let _ = info.name.extend_from_slice(name.as_bytes());
                    Ok(info)
                }
                Err(_) => Err(FirmwareError::NotFound),
            }
        } else {
            Err(FirmwareError::NotFound)
        }
    }
}

/// Simple CRC32 implementation
fn crc32(data: &[u8]) -> u32 {
    let mut crc = 0xFFFFFFFF_u32;
    
    for byte in data {
        crc ^= *byte as u32;
        for _ in 0..8 {
            if (crc & 1) != 0 {
                crc = (crc >> 1) ^ 0xEDB88320;
            } else {
                crc >>= 1;
            }
        }
    }
    
    !crc
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_crc32() {
        let data = b"123456789";
        let crc = crc32(data);
        assert_eq!(crc, 0xCBF43926); // Known CRC32 of "123456789"
    }
    
    #[test]
    fn test_iwl_header_parse() {
        let mut data = [0u8; 32];
        data[0..4].copy_from_slice(&0x0A4C5749_u32.to_le_bytes()); // Magic
        
        let header = IwlFirmwareHeader::parse(&data);
        assert!(header.is_some());
        assert_eq!(header.unwrap().magic, IwlFirmwareHeader::MAGIC);
    }
}
