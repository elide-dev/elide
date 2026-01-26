// MediaTek MT7601U USB WiFi Driver for Colide OS
// Based on Linux kernel mt7601u driver (drivers/net/wireless/mediatek/mt7601u/)
// Standalone driver skeleton - mac80211 integration to be added later

use crate::usb::{UsbHostController, UsbDevice, UsbSetupPacket};

/// MT7601U USB Vendor/Product IDs
pub const MT7601U_VENDOR_ID: u16 = 0x148f;
pub const MT7601U_PRODUCT_ID: u16 = 0x7601;

/// Known VID/PIDs for MT7601U and rebranded devices
pub const MT7601U_IDS: &[(u16, u16)] = &[
    (0x148f, 0x7601),  // MediaTek
    (0x148f, 0x760b),  // MediaTek variant
    (0x2717, 0x4106),  // Xiaomi
    (0x0e8d, 0x760a),  // MediaTek variant
    (0x0e8d, 0x760b),  // MediaTek variant
    (0x2357, 0x0105),  // TP-Link
    (0x0b05, 0x17d3),  // ASUS
];

/// Register addresses
mod regs {
    pub const SYS_CTRL: u16 = 0x0400;
    pub const MAC_SYS_CTRL: u16 = 0x1004;
    pub const MAC_ADDR_DW0: u16 = 0x1008;
    pub const MAC_ADDR_DW1: u16 = 0x100c;
    pub const MAC_BSSID_DW0: u16 = 0x1010;
    pub const MAC_BSSID_DW1: u16 = 0x1014;
    pub const TX_PWR_CFG_0: u16 = 0x1314;
    pub const USB_DMA_CFG: u16 = 0x02a0;
    pub const RF_BYPASS_0: u16 = 0x0504;
    pub const RF_SETTING_0: u16 = 0x050c;
    pub const BBP_CSR_CFG: u16 = 0x101c;
}

/// USB request types
mod usb_req {
    pub const SINGLE_READ: u8 = 0x02;
    pub const SINGLE_WRITE: u8 = 0x03;
    pub const EEPROM_READ: u8 = 0x06;
}

/// Channel definition
#[derive(Debug, Clone, Copy)]
pub struct Mt7601uChannel {
    pub freq_mhz: u16,
    pub number: u8,
    pub max_power_dbm: u8,
}

/// 2.4 GHz channels
pub const MT7601U_CHANNELS: &[Mt7601uChannel] = &[
    Mt7601uChannel { freq_mhz: 2412, number: 1, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2417, number: 2, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2422, number: 3, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2427, number: 4, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2432, number: 5, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2437, number: 6, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2442, number: 7, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2447, number: 8, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2452, number: 9, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2457, number: 10, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2462, number: 11, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2467, number: 12, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2472, number: 13, max_power_dbm: 20 },
    Mt7601uChannel { freq_mhz: 2484, number: 14, max_power_dbm: 20 },
];

/// Driver state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Mt7601uState {
    Uninitialized,
    Initialized,
    Started,
    Scanning,
    Connecting,
    Connected,
    Error,
}

/// EEPROM calibration data
#[derive(Debug, Clone)]
pub struct Mt7601uEeprom {
    pub mac_addr: [u8; 6],
    pub vendor_id: u16,
    pub product_id: u16,
    pub tx_power: [u8; 14],
    pub freq_offset: u8,
    pub country_region: u8,
}

impl Default for Mt7601uEeprom {
    fn default() -> Self {
        Self {
            mac_addr: [0x00, 0x0c, 0x43, 0x76, 0x01, 0x00],
            vendor_id: MT7601U_VENDOR_ID,
            product_id: MT7601U_PRODUCT_ID,
            tx_power: [0x0f; 14],
            freq_offset: 0,
            country_region: 0,
        }
    }
}

/// MT7601U WiFi Driver
pub struct Mt7601uDriver {
    state: Mt7601uState,
    usb_address: u8,
    eeprom: Mt7601uEeprom,
    current_channel: u8,
    tx_power: u8,
    mac_addr: [u8; 6],
    bssid: [u8; 6],
    rx_filter: u32,
}

impl Mt7601uDriver {
    pub fn new() -> Self {
        Self {
            state: Mt7601uState::Uninitialized,
            usb_address: 0,
            eeprom: Mt7601uEeprom::default(),
            current_channel: 1,
            tx_power: 15,
            mac_addr: [0; 6],
            bssid: [0; 6],
            rx_filter: 0,
        }
    }
    
    /// Check if USB device is an MT7601U
    pub fn probe(device: &UsbDevice) -> bool {
        if let Some(desc) = &device.descriptor {
            for (vid, pid) in MT7601U_IDS {
                if desc.vendor_id == *vid && desc.product_id == *pid {
                    return true;
                }
            }
        }
        false
    }
    
    /// Initialize driver with USB device
    pub fn init<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        device: &UsbDevice,
    ) -> Result<(), Mt7601uError> {
        self.usb_address = device.address;
        
        // Read EEPROM data
        self.read_eeprom(controller)?;
        
        // Initialize hardware
        self.hw_init(controller)?;
        
        // Set MAC address from EEPROM
        self.mac_addr = self.eeprom.mac_addr;
        
        self.state = Mt7601uState::Initialized;
        Ok(())
    }
    
    /// Read EEPROM calibration data
    fn read_eeprom<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        let mut buf = [0u8; 128];
        
        let setup = UsbSetupPacket {
            request_type: 0xC0,
            request: usb_req::EEPROM_READ,
            value: 0,
            index: 0,
            length: 128,
        };
        
        match controller.control_transfer(self.usb_address, &setup, Some(&mut buf), 1000) {
            Ok(len) if len >= 32 => {
                self.eeprom.mac_addr.copy_from_slice(&buf[4..10]);
                self.eeprom.vendor_id = u16::from_le_bytes([buf[0], buf[1]]);
                self.eeprom.product_id = u16::from_le_bytes([buf[2], buf[3]]);
                self.eeprom.freq_offset = buf[26];
                self.eeprom.country_region = buf[27];
                for i in 0..14 {
                    self.eeprom.tx_power[i] = buf[32 + i];
                }
                Ok(())
            }
            Ok(_) => Err(Mt7601uError::EepromRead),
            Err(_) => Ok(()),  // Use defaults
        }
    }
    
    /// Initialize hardware
    fn hw_init<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        // Power on and reset
        self.write_reg(controller, regs::SYS_CTRL, 0x0001)?;
        self.delay_ms(10);
        self.write_reg(controller, regs::SYS_CTRL, 0x0000)?;
        
        // Initialize MAC
        self.write_reg(controller, regs::MAC_SYS_CTRL, 0x00)?;
        
        // Set USB DMA configuration
        self.write_reg(controller, regs::USB_DMA_CFG, 0x00c00020)?;
        
        // Initialize RF/BBP
        self.rf_init(controller)?;
        
        // Set default channel
        self.set_channel(controller, 1)?;
        
        Ok(())
    }
    
    /// Initialize RF subsystem
    fn rf_init<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        self.write_reg(controller, regs::RF_BYPASS_0, 0x00000000)?;
        
        // BBP register initialization sequence
        let bbp_init: &[(u8, u8)] = &[
            (1, 0x04), (4, 0x40), (20, 0x06), (21, 0x00),
            (22, 0x00), (27, 0x00), (62, 0x37), (63, 0x37),
            (64, 0x37), (65, 0x2c), (66, 0x1c), (68, 0x0b),
            (69, 0x12), (70, 0x0a), (73, 0x10), (81, 0x37),
            (82, 0x62), (83, 0x6a), (84, 0x99), (86, 0x00),
            (91, 0x04), (92, 0x00), (103, 0x00), (105, 0x05),
        ];
        
        for (reg, val) in bbp_init {
            self.write_bbp(controller, *reg, *val)?;
        }
        
        Ok(())
    }
    
    /// Write BBP register
    fn write_bbp<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        reg: u8,
        value: u8,
    ) -> Result<(), Mt7601uError> {
        let csr_val = ((reg as u32) << 8) | (value as u32) | (1 << 17);
        self.write_reg(controller, regs::BBP_CSR_CFG, csr_val)?;
        
        for _ in 0..100 {
            let status = self.read_reg(controller, regs::BBP_CSR_CFG)?;
            if status & (1 << 17) == 0 {
                return Ok(());
            }
            self.delay_ms(1);
        }
        
        Err(Mt7601uError::BbpTimeout)
    }
    
    /// Set operating channel (1-14)
    pub fn set_channel<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        channel: u8,
    ) -> Result<(), Mt7601uError> {
        if channel < 1 || channel > 14 {
            return Err(Mt7601uError::InvalidChannel);
        }
        
        let rf_val = 0x50 + (channel - 1) as u32;
        self.write_reg(controller, regs::RF_SETTING_0, rf_val)?;
        
        let tx_power = self.eeprom.tx_power[channel as usize - 1];
        self.write_reg(controller, regs::TX_PWR_CFG_0, tx_power as u32)?;
        
        self.current_channel = channel;
        Ok(())
    }
    
    /// Write 32-bit register
    fn write_reg<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        addr: u16,
        value: u32,
    ) -> Result<(), Mt7601uError> {
        let setup = UsbSetupPacket {
            request_type: 0x40,
            request: usb_req::SINGLE_WRITE,
            value: addr,
            index: 0,
            length: 4,
        };
        
        let mut data = value.to_le_bytes();
        controller.control_transfer(self.usb_address, &setup, Some(&mut data), 1000)
            .map_err(|_| Mt7601uError::UsbWrite)?;
        Ok(())
    }
    
    /// Read 32-bit register
    fn read_reg<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        addr: u16,
    ) -> Result<u32, Mt7601uError> {
        let setup = UsbSetupPacket {
            request_type: 0xC0,
            request: usb_req::SINGLE_READ,
            value: addr,
            index: 0,
            length: 4,
        };
        
        let mut data = [0u8; 4];
        controller.control_transfer(self.usb_address, &setup, Some(&mut data), 1000)
            .map_err(|_| Mt7601uError::UsbRead)?;
        Ok(u32::from_le_bytes(data))
    }
    
    /// Delay in milliseconds
    fn delay_ms(&self, ms: u32) {
        for _ in 0..(ms * 1000) {
            core::hint::spin_loop();
        }
    }
    
    /// Start the driver (enable TX/RX)
    pub fn start<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        if self.state != Mt7601uState::Initialized {
            return Err(Mt7601uError::NotInitialized);
        }
        
        // Enable MAC TX/RX
        self.write_reg(controller, regs::MAC_SYS_CTRL, 0x0c)?;
        
        // Set MAC address
        let mac_dw0 = u32::from_le_bytes([
            self.mac_addr[0], self.mac_addr[1],
            self.mac_addr[2], self.mac_addr[3],
        ]);
        let mac_dw1 = u16::from_le_bytes([self.mac_addr[4], self.mac_addr[5]]) as u32;
        
        self.write_reg(controller, regs::MAC_ADDR_DW0, mac_dw0)?;
        self.write_reg(controller, regs::MAC_ADDR_DW1, mac_dw1)?;
        
        self.state = Mt7601uState::Started;
        Ok(())
    }
    
    /// Stop the driver
    pub fn stop<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        self.write_reg(controller, regs::MAC_SYS_CTRL, 0x00)?;
        self.state = Mt7601uState::Initialized;
        Ok(())
    }
    
    /// Set BSSID for association
    pub fn set_bssid<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        bssid: [u8; 6],
    ) -> Result<(), Mt7601uError> {
        let bssid_dw0 = u32::from_le_bytes([bssid[0], bssid[1], bssid[2], bssid[3]]);
        let bssid_dw1 = u16::from_le_bytes([bssid[4], bssid[5]]) as u32;
        
        self.write_reg(controller, regs::MAC_BSSID_DW0, bssid_dw0)?;
        self.write_reg(controller, regs::MAC_BSSID_DW1, bssid_dw1)?;
        
        self.bssid = bssid;
        Ok(())
    }
    
    // Getters
    pub fn mac_address(&self) -> [u8; 6] { self.mac_addr }
    pub fn channel(&self) -> u8 { self.current_channel }
    pub fn state(&self) -> Mt7601uState { self.state }
    pub fn bssid(&self) -> [u8; 6] { self.bssid }
}

impl Default for Mt7601uDriver {
    fn default() -> Self {
        Self::new()
    }
}

/// Driver errors
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Mt7601uError {
    NotInitialized,
    EepromRead,
    UsbWrite,
    UsbRead,
    BbpTimeout,
    InvalidChannel,
    HwError,
}
