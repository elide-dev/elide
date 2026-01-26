//! RTL8188EU USB WiFi Driver for Colide OS
//!
//! Realtek RTL8188EU/RTL8188EUS - Common USB WiFi dongle chip
//! Found in: TP-Link TL-WN725N, many generic "nano" USB adapters
//!
//! Features:
//! - 802.11b/g/n (2.4GHz only)
//! - Single spatial stream (1x1)
//! - Up to 150 Mbps PHY rate
//! - USB 2.0 High Speed

use heapless::Vec as HVec;

/// RTL8188EU USB IDs
pub const RTL8188EU_VENDOR_ID: u16 = 0x0BDA;  // Realtek
pub const RTL8188EU_PRODUCT_IDS: &[u16] = &[
    0x8179,  // RTL8188EUS
    0x0179,  // RTL8188ETV
    0x8176,  // RTL8188EU
    0x8178,  // RTL8192EU
];

/// Additional vendor IDs for RTL8188EU-based devices
pub const RTL8188EU_DEVICES: &[(u16, u16)] = &[
    (0x2357, 0x010C),  // TP-Link TL-WN725N v2
    (0x2357, 0x0111),  // TP-Link TL-WN727N v5
    (0x0BDA, 0x8179),  // Realtek reference
    (0x0BDA, 0x0179),  // Realtek ETV
    (0x7392, 0x7811),  // Edimax EW-7811Un
    (0x2001, 0x3311),  // D-Link DWA-125
];

/// RTL8188EU register addresses
pub mod Regs {
    // System Configuration
    pub const SYS_FUNC_EN: u16 = 0x0002;
    pub const SYS_CLK: u16 = 0x0008;
    pub const AFE_PLL_CTRL: u16 = 0x0028;
    pub const AFE_XTAL_CTRL: u16 = 0x0024;
    
    // MAC Configuration
    pub const CR: u16 = 0x0100;           // Command Register
    pub const TCR: u16 = 0x0200;          // Transmit Configuration
    pub const RCR: u16 = 0x0608;          // Receive Configuration
    pub const MSR: u16 = 0x0102;          // Media Status
    pub const BSSID: u16 = 0x0618;        // BSSID (6 bytes)
    pub const MAC_ADDR: u16 = 0x0610;     // MAC Address (6 bytes)
    
    // RF Configuration
    pub const RF_CTRL: u16 = 0x001F;
    pub const HSSI_PARAM1: u16 = 0x0820;
    pub const HSSI_PARAM2: u16 = 0x0824;
    
    // EFUSE/EEPROM
    pub const EFUSE_CTRL: u16 = 0x0030;
    pub const EFUSE_DATA: u16 = 0x0034;
    
    // USB
    pub const USB_AGG_TO: u16 = 0xFE5B;
    pub const USB_DMA_AGG_TO: u16 = 0xFE5C;
    
    // Interrupt
    pub const HISR: u16 = 0x0124;         // Host ISR
    pub const HIMR: u16 = 0x0120;         // Host IMR
}

/// Driver state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Rtl8188euState {
    Uninitialized,
    Initializing,
    Ready,
    Scanning,
    Connecting,
    Connected,
    Error,
}

/// EFUSE/EEPROM data
#[derive(Debug, Clone)]
pub struct Rtl8188euEfuse {
    pub mac_addr: [u8; 6],
    pub channel_plan: u8,
    pub crystal_cap: u8,
    pub thermal_meter: u8,
    pub pa_type: u8,
    pub lna_type: u8,
}

impl Default for Rtl8188euEfuse {
    fn default() -> Self {
        Self {
            mac_addr: [0x00, 0xE0, 0x4C, 0x00, 0x00, 0x00], // Realtek OUI
            channel_plan: 0x20,  // World Safe
            crystal_cap: 0x20,
            thermal_meter: 0x12,
            pa_type: 0,
            lna_type: 0,
        }
    }
}

/// USB vendor request codes for RTL8188EU
mod usb_req {
    pub const READ_REG: u8 = 0x05;
    pub const WRITE_REG: u8 = 0x05;
}

use crate::usb::{UsbHostController, UsbSetupPacket};

/// RTL8188EU driver
pub struct Rtl8188euDriver {
    pub state: Rtl8188euState,
    pub efuse: Rtl8188euEfuse,
    pub current_channel: u8,
    pub tx_power: u8,
    pub rf_type: u8,          // 1T1R
    pub usb_address: u8,      // USB device address
}

impl Rtl8188euDriver {
    pub fn new() -> Self {
        Self {
            state: Rtl8188euState::Uninitialized,
            efuse: Rtl8188euEfuse::default(),
            current_channel: 1,
            tx_power: 20,
            rf_type: 0x11,  // 1T1R
            usb_address: 0,
        }
    }
    
    /// Set USB device address (call after enumeration)
    pub fn set_usb_address(&mut self, addr: u8) {
        self.usb_address = addr;
    }
    
    /// Initialize the RTL8188EU hardware
    pub fn init(&mut self) -> Result<(), Rtl8188euError> {
        self.state = Rtl8188euState::Initializing;
        
        // Power sequence
        self.power_on()?;
        
        // Load EFUSE data
        self.load_efuse()?;
        
        // Initialize MAC
        self.init_mac()?;
        
        // Initialize BB (Baseband)
        self.init_bb()?;
        
        // Initialize RF
        self.init_rf()?;
        
        // Initialize DM (Dynamic Mechanism)
        self.init_dm()?;
        
        self.state = Rtl8188euState::Ready;
        Ok(())
    }
    
    /// Power on sequence
    fn power_on(&mut self) -> Result<(), Rtl8188euError> {
        // Enable system clocks
        self.write_reg8(Regs::SYS_FUNC_EN, 0x76)?;
        
        // Wait for PLL lock
        for _ in 0..100 {
            let val = self.read_reg8(Regs::SYS_CLK)?;
            if (val & 0x80) != 0 {
                return Ok(());
            }
        }
        
        Err(Rtl8188euError::PllTimeout)
    }
    
    /// Load EFUSE/EEPROM data
    fn load_efuse(&mut self) -> Result<(), Rtl8188euError> {
        // Read MAC address from EFUSE offset 0xD0
        for i in 0..6 {
            self.efuse.mac_addr[i] = self.read_efuse(0xD0 + i as u16)?;
        }
        
        // Read channel plan
        self.efuse.channel_plan = self.read_efuse(0xC8)?;
        
        // Read crystal cap
        self.efuse.crystal_cap = self.read_efuse(0xB9)?;
        
        // Read thermal meter
        self.efuse.thermal_meter = self.read_efuse(0x12)?;
        
        Ok(())
    }
    
    /// Read EFUSE byte
    fn read_efuse(&self, addr: u16) -> Result<u8, Rtl8188euError> {
        // Write address and trigger read
        self.write_reg32(Regs::EFUSE_CTRL, (addr as u32) | 0x80000000)?;
        
        // Wait for completion
        for _ in 0..1000 {
            let val = self.read_reg32(Regs::EFUSE_CTRL)?;
            if (val & 0x80000000) == 0 {
                return Ok((val >> 24) as u8);
            }
        }
        
        Err(Rtl8188euError::EfuseTimeout)
    }
    
    /// Initialize MAC
    fn init_mac(&mut self) -> Result<(), Rtl8188euError> {
        // Set MAC address
        for i in 0..6 {
            self.write_reg8(Regs::MAC_ADDR + i as u16, self.efuse.mac_addr[i])?;
        }
        
        // Configure RX filter
        let rcr = 0x7000200E_u32; // Accept all data, mgmt, ctrl frames
        self.write_reg32(Regs::RCR, rcr)?;
        
        // Configure TX
        let tcr = 0x00004000_u32; // Normal TX
        self.write_reg32(Regs::TCR, tcr)?;
        
        Ok(())
    }
    
    /// Initialize Baseband
    fn init_bb(&mut self) -> Result<(), Rtl8188euError> {
        // BB initialization values would go here
        // This is chip-specific and requires PHY register writes
        Ok(())
    }
    
    /// Initialize RF
    fn init_rf(&mut self) -> Result<(), Rtl8188euError> {
        // RF initialization values would go here
        // Requires RF register writes via HSSI
        Ok(())
    }
    
    /// Initialize Dynamic Mechanism
    fn init_dm(&mut self) -> Result<(), Rtl8188euError> {
        // Rate adaptation, power control, etc.
        Ok(())
    }
    
    /// Set channel
    pub fn set_channel(&mut self, channel: u8) -> Result<(), Rtl8188euError> {
        if channel < 1 || channel > 14 {
            return Err(Rtl8188euError::InvalidChannel);
        }
        
        // Channel to frequency mapping
        let freq = match channel {
            1 => 2412,
            2 => 2417,
            3 => 2422,
            4 => 2427,
            5 => 2432,
            6 => 2437,
            7 => 2442,
            8 => 2447,
            9 => 2452,
            10 => 2457,
            11 => 2462,
            12 => 2467,
            13 => 2472,
            14 => 2484,
            _ => return Err(Rtl8188euError::InvalidChannel),
        };
        
        // Write RF channel (simplified)
        self.write_rf(0x18, freq as u32)?;
        
        self.current_channel = channel;
        Ok(())
    }
    
    /// Write RF register via HSSI
    fn write_rf(&self, addr: u8, data: u32) -> Result<(), Rtl8188euError> {
        let val = ((addr as u32) << 20) | (data & 0xFFFFF);
        self.write_reg32(Regs::HSSI_PARAM2, val | 0x80000000)?;
        Ok(())
    }
    
    /// Read register (8-bit) via USB control transfer
    fn read_reg8_usb<C: UsbHostController>(&self, controller: &mut C, addr: u16) -> Result<u8, Rtl8188euError> {
        let setup = UsbSetupPacket {
            request_type: 0xC0,  // Device-to-host, vendor, device
            request: usb_req::READ_REG,
            value: addr,
            index: 0,
            length: 1,
        };
        let mut buf = [0u8; 1];
        controller.control_transfer(self.usb_address, &setup, Some(&mut buf), 1000)
            .map_err(|_| Rtl8188euError::UsbRead)?;
        Ok(buf[0])
    }
    
    /// Read register (32-bit) via USB control transfer
    fn read_reg32_usb<C: UsbHostController>(&self, controller: &mut C, addr: u16) -> Result<u32, Rtl8188euError> {
        let setup = UsbSetupPacket {
            request_type: 0xC0,
            request: usb_req::READ_REG,
            value: addr,
            index: 0,
            length: 4,
        };
        let mut buf = [0u8; 4];
        controller.control_transfer(self.usb_address, &setup, Some(&mut buf), 1000)
            .map_err(|_| Rtl8188euError::UsbRead)?;
        Ok(u32::from_le_bytes(buf))
    }
    
    /// Write register (8-bit) via USB control transfer
    fn write_reg8_usb<C: UsbHostController>(&self, controller: &mut C, addr: u16, val: u8) -> Result<(), Rtl8188euError> {
        let setup = UsbSetupPacket {
            request_type: 0x40,  // Host-to-device, vendor, device
            request: usb_req::WRITE_REG,
            value: addr,
            index: 0,
            length: 1,
        };
        let mut buf = [val];
        controller.control_transfer(self.usb_address, &setup, Some(&mut buf), 1000)
            .map_err(|_| Rtl8188euError::UsbWrite)?;
        Ok(())
    }
    
    /// Write register (32-bit) via USB control transfer
    fn write_reg32_usb<C: UsbHostController>(&self, controller: &mut C, addr: u16, val: u32) -> Result<(), Rtl8188euError> {
        let setup = UsbSetupPacket {
            request_type: 0x40,
            request: usb_req::WRITE_REG,
            value: addr,
            index: 0,
            length: 4,
        };
        let mut buf = val.to_le_bytes();
        controller.control_transfer(self.usb_address, &setup, Some(&mut buf), 1000)
            .map_err(|_| Rtl8188euError::UsbWrite)?;
        Ok(())
    }
    
    /// Read register (8-bit) - standalone version (returns 0 if no controller)
    fn read_reg8(&self, _addr: u16) -> Result<u8, Rtl8188euError> {
        // Use with controller version for actual hardware
        Ok(0)
    }
    
    /// Read register (32-bit) - standalone version
    fn read_reg32(&self, _addr: u16) -> Result<u32, Rtl8188euError> {
        Ok(0)
    }
    
    /// Write register (8-bit) - standalone version
    fn write_reg8(&self, _addr: u16, _val: u8) -> Result<(), Rtl8188euError> {
        Ok(())
    }
    
    /// Write register (32-bit) - standalone version
    fn write_reg32(&self, _addr: u16, _val: u32) -> Result<(), Rtl8188euError> {
        Ok(())
    }
    
    /// Get MAC address
    pub fn mac_address(&self) -> [u8; 6] {
        self.efuse.mac_addr
    }
    
    /// Start scanning
    pub fn start_scan(&mut self) -> Result<(), Rtl8188euError> {
        if self.state != Rtl8188euState::Ready && 
           self.state != Rtl8188euState::Connected {
            return Err(Rtl8188euError::NotReady);
        }
        
        self.state = Rtl8188euState::Scanning;
        
        // Set to promiscuous mode
        let rcr = self.read_reg32(Regs::RCR)?;
        self.write_reg32(Regs::RCR, rcr | 0x00000001)?; // Accept all
        
        Ok(())
    }
    
    /// Stop scanning
    pub fn stop_scan(&mut self) -> Result<(), Rtl8188euError> {
        self.state = Rtl8188euState::Ready;
        
        // Restore normal RX filter
        self.write_reg32(Regs::RCR, 0x7000200E)?;
        
        Ok(())
    }
    
    /// Build probe request frame
    pub fn build_probe_request(&self, ssid: Option<&[u8]>) -> HVec<u8, 256> {
        let mut frame = HVec::new();
        
        // Frame Control: Probe Request (0x0040)
        let _ = frame.extend_from_slice(&[0x40, 0x00]);
        
        // Duration
        let _ = frame.extend_from_slice(&[0x00, 0x00]);
        
        // Destination: Broadcast
        let _ = frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);
        
        // Source: Our MAC
        let _ = frame.extend_from_slice(&self.efuse.mac_addr);
        
        // BSSID: Broadcast
        let _ = frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]);
        
        // Sequence control
        let _ = frame.extend_from_slice(&[0x00, 0x00]);
        
        // SSID IE
        let _ = frame.push(0x00); // Element ID
        if let Some(ssid_data) = ssid {
            let _ = frame.push(ssid_data.len() as u8);
            let _ = frame.extend_from_slice(ssid_data);
        } else {
            let _ = frame.push(0x00); // Wildcard SSID
        }
        
        // Supported Rates IE
        let _ = frame.extend_from_slice(&[
            0x01, 0x08,  // Element ID, Length
            0x82, 0x84, 0x8B, 0x96,  // 1, 2, 5.5, 11 Mbps (basic)
            0x0C, 0x12, 0x18, 0x24,  // 6, 9, 12, 18 Mbps
        ]);
        
        // Extended Supported Rates
        let _ = frame.extend_from_slice(&[
            0x32, 0x04,  // Element ID, Length
            0x30, 0x48, 0x60, 0x6C,  // 24, 36, 48, 54 Mbps
        ]);
        
        frame
    }
    
    /// Transmit frame
    pub fn transmit(&mut self, frame: &[u8]) -> Result<(), Rtl8188euError> {
        if self.state == Rtl8188euState::Uninitialized {
            return Err(Rtl8188euError::NotReady);
        }
        
        // Build TX descriptor
        let mut tx_desc = [0u8; 32];
        let pkt_len = frame.len() as u16;
        
        // TX desc word 0
        tx_desc[0] = (pkt_len & 0xFF) as u8;
        tx_desc[1] = ((pkt_len >> 8) & 0x0F) as u8;
        tx_desc[1] |= 0x40; // First segment
        tx_desc[1] |= 0x80; // Last segment
        
        // TX desc word 1 - rate
        tx_desc[4] = 0x00; // CCK 1Mbps for mgmt frames
        
        // Would send via USB bulk endpoint
        let _ = (tx_desc, frame);
        
        Ok(())
    }
}

impl Default for Rtl8188euDriver {
    fn default() -> Self {
        Self::new()
    }
}

/// Driver errors
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Rtl8188euError {
    NotReady,
    PllTimeout,
    EfuseTimeout,
    InvalidChannel,
    UsbError,
    UsbRead,
    UsbWrite,
    TxFailed,
    RxFailed,
}

/// RTL8188EU HT Capabilities
pub fn rtl8188eu_ht_caps() -> super::mt7601u::HtCapabilities {
    let mut mcs_set = [0u8; 16];
    mcs_set[0] = 0xFF; // MCS 0-7 (1 spatial stream)
    
    super::mt7601u::HtCapabilities {
        ht_supported: true,
        channel_width_40mhz: false, // 8188EU is 20MHz only in many designs
        short_gi_20mhz: true,
        short_gi_40mhz: false,
        rx_stbc: 1,
        tx_stbc: false,
        max_amsdu_len: 3839,
        mcs_set,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_driver_new() {
        let driver = Rtl8188euDriver::new();
        assert_eq!(driver.state, Rtl8188euState::Uninitialized);
    }
    
    #[test]
    fn test_probe_request() {
        let driver = Rtl8188euDriver::new();
        let frame = driver.build_probe_request(Some(b"TestSSID"));
        assert!(frame.len() > 24); // At least header + IEs
    }
}
