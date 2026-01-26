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
    pub const RX_FILTER_CFG: u16 = 0x1400;
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
    
    /// Transmit an 802.11 frame via USB bulk endpoint
    pub fn tx_frame<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        frame: &[u8],
    ) -> Result<(), Mt7601uError> {
        if self.state != Mt7601uState::Started && self.state != Mt7601uState::Connected {
            return Err(Mt7601uError::NotInitialized);
        }
        
        // Build TXWI (TX Wireless Info) header - 20 bytes
        let mut txwi = [0u8; 20];
        txwi[0] = 0x00;  // FRAG, MPDUtotalByteCount[7:0]
        txwi[1] = (frame.len() as u8) & 0x0f;  // MPDUtotalByteCount[11:8]
        txwi[2] = 0x00;  // TX_PACKET_ID, MCS
        txwi[3] = 0x00;  // BW, ShortGI, STBC, PHYMODE
        txwi[4] = 0x00;  // ACK, NSEQ, BAWinSize[5:0]
        txwi[5] = 0x00;  // WCID
        txwi[6] = 0x00;  // PacketId
        txwi[7] = 0x00;  // Reserved
        
        // Build USB TX packet: TXINFO (4) + TXWI (20) + Frame + Padding
        let frame_len = frame.len();
        let total_len = 4 + 20 + frame_len;
        let padded_len = (total_len + 3) & !3;  // 4-byte alignment
        
        let mut tx_buf = vec![0u8; padded_len];
        
        // TXINFO header (4 bytes)
        let info_len = (20 + frame_len) as u32;
        tx_buf[0..4].copy_from_slice(&info_len.to_le_bytes());
        tx_buf[0] |= 0x80;  // USB_TX_BURST flag
        
        // TXWI
        tx_buf[4..24].copy_from_slice(&txwi);
        
        // Frame data
        tx_buf[24..24 + frame_len].copy_from_slice(frame);
        
        // Send via bulk OUT endpoint (EP1 OUT = 0x01)
        controller.bulk_transfer(self.usb_address, 0x01, &mut tx_buf, 1000)
            .map_err(|_| Mt7601uError::TxFailed)?;
        
        Ok(())
    }
    
    /// Receive 802.11 frames via USB bulk endpoint
    pub fn rx_frame<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        buf: &mut [u8],
    ) -> Result<RxFrameInfo, Mt7601uError> {
        if self.state != Mt7601uState::Started && self.state != Mt7601uState::Connected {
            return Err(Mt7601uError::NotInitialized);
        }
        
        // Receive from bulk IN endpoint (EP1 IN = 0x81)
        let mut rx_buf = [0u8; 2048];
        let len = controller.bulk_transfer(self.usb_address, 0x81, &mut rx_buf, 100)
            .map_err(|_| Mt7601uError::RxFailed)?;
        
        if len < 8 {
            return Err(Mt7601uError::RxFailed);
        }
        
        // Parse RXINFO (4 bytes) + RXWI (24 bytes)
        let rx_len = u32::from_le_bytes([rx_buf[0], rx_buf[1], rx_buf[2], rx_buf[3]]) & 0xffff;
        
        // RXWI fields
        let rssi = rx_buf[6] as i8 - 110;  // Convert to dBm
        let snr = rx_buf[7];
        let mcs = rx_buf[4] & 0x7f;
        let bw = (rx_buf[5] >> 7) & 0x01;
        let sgi = (rx_buf[5] >> 6) & 0x01;
        
        // Frame starts after RXINFO (4) + RXWI (24) = 28 bytes
        let frame_start = 28;
        let frame_len = (rx_len as usize).saturating_sub(24).min(buf.len());
        
        if frame_start + frame_len <= len {
            buf[..frame_len].copy_from_slice(&rx_buf[frame_start..frame_start + frame_len]);
        }
        
        Ok(RxFrameInfo {
            length: frame_len,
            rssi,
            snr,
            rate_mcs: mcs,
            bandwidth_40mhz: bw != 0,
            short_gi: sgi != 0,
        })
    }
    
    /// Start a passive scan on current channel
    pub fn start_scan<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        if self.state != Mt7601uState::Started {
            return Err(Mt7601uError::NotInitialized);
        }
        
        // Set promiscuous mode for scanning
        self.rx_filter = 0x0001;  // Accept all frames
        self.write_reg(controller, regs::RX_FILTER_CFG, self.rx_filter)?;
        
        self.state = Mt7601uState::Scanning;
        Ok(())
    }
    
    /// Stop scanning and return to normal mode
    pub fn stop_scan<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        // Restore normal RX filter
        self.rx_filter = 0x17f9;  // Normal station mode filter
        self.write_reg(controller, regs::RX_FILTER_CFG, self.rx_filter)?;
        
        self.state = Mt7601uState::Started;
        Ok(())
    }
    
    /// Send a probe request for active scanning
    pub fn send_probe_request<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        ssid: Option<&[u8]>,
    ) -> Result<(), Mt7601uError> {
        // Build 802.11 probe request frame
        let mut frame = Vec::with_capacity(128);
        
        // Frame Control: Probe Request (0x0040)
        frame.extend_from_slice(&[0x40, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA: Broadcast
        frame.extend_from_slice(&[0xff, 0xff, 0xff, 0xff, 0xff, 0xff]);
        // SA: Our MAC
        frame.extend_from_slice(&self.mac_addr);
        // BSSID: Broadcast
        frame.extend_from_slice(&[0xff, 0xff, 0xff, 0xff, 0xff, 0xff]);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        
        // Information Elements
        // SSID
        frame.push(0);  // Element ID
        if let Some(s) = ssid {
            frame.push(s.len() as u8);
            frame.extend_from_slice(s);
        } else {
            frame.push(0);  // Wildcard SSID
        }
        
        // Supported Rates
        frame.push(1);  // Element ID
        frame.push(8);  // Length
        frame.extend_from_slice(&[0x82, 0x84, 0x8b, 0x96, 0x0c, 0x12, 0x18, 0x24]);
        
        // Extended Supported Rates
        frame.push(50);  // Element ID
        frame.push(4);   // Length
        frame.extend_from_slice(&[0x30, 0x48, 0x60, 0x6c]);
        
        self.tx_frame(controller, &frame)
    }
}

/// Received frame information
#[derive(Debug, Clone, Copy)]
pub struct RxFrameInfo {
    pub length: usize,
    pub rssi: i8,
    pub snr: u8,
    pub rate_mcs: u8,
    pub bandwidth_40mhz: bool,
    pub short_gi: bool,
}

/// Scan result from beacon/probe response
#[derive(Debug, Clone)]
pub struct ScanResult {
    pub bssid: [u8; 6],
    pub ssid: heapless::String<32>,
    pub channel: u8,
    pub rssi: i8,
    pub capability: u16,
    pub beacon_interval: u16,
    pub is_wpa: bool,
    pub is_wpa2: bool,
    pub is_open: bool,
}

/// 802.11 frame types
mod frame_type {
    pub const MGMT_BEACON: u16 = 0x0080;
    pub const MGMT_PROBE_RESP: u16 = 0x0050;
    pub const MGMT_AUTH: u16 = 0x00b0;
    pub const MGMT_DEAUTH: u16 = 0x00c0;
    pub const MGMT_ASSOC_REQ: u16 = 0x0000;
    pub const MGMT_ASSOC_RESP: u16 = 0x0010;
    pub const MGMT_DISASSOC: u16 = 0x00a0;
}

/// Information Element IDs
mod ie_id {
    pub const SSID: u8 = 0;
    pub const SUPPORTED_RATES: u8 = 1;
    pub const DS_PARAMS: u8 = 3;
    pub const TIM: u8 = 5;
    pub const RSN: u8 = 48;
    pub const VENDOR: u8 = 221;
}

/// WPA OUI
const WPA_OUI: [u8; 4] = [0x00, 0x50, 0xf2, 0x01];

impl ScanResult {
    /// Parse beacon or probe response frame into ScanResult
    pub fn from_frame(frame: &[u8], rssi: i8) -> Option<Self> {
        if frame.len() < 36 {
            return None;
        }
        
        // Check frame type (beacon or probe response)
        let fc = u16::from_le_bytes([frame[0], frame[1]]);
        if fc != frame_type::MGMT_BEACON && fc != frame_type::MGMT_PROBE_RESP {
            return None;
        }
        
        // Extract BSSID (offset 16)
        let mut bssid = [0u8; 6];
        bssid.copy_from_slice(&frame[16..22]);
        
        // Fixed fields start at offset 24
        // Timestamp (8) + Beacon Interval (2) + Capability (2) = 12 bytes
        let beacon_interval = u16::from_le_bytes([frame[32], frame[33]]);
        let capability = u16::from_le_bytes([frame[34], frame[35]]);
        
        // Parse Information Elements starting at offset 36
        let mut ssid = heapless::String::new();
        let mut channel = 0u8;
        let mut is_wpa = false;
        let mut is_wpa2 = false;
        
        let mut pos = 36;
        while pos + 2 <= frame.len() {
            let ie_id = frame[pos];
            let ie_len = frame[pos + 1] as usize;
            pos += 2;
            
            if pos + ie_len > frame.len() {
                break;
            }
            
            match ie_id {
                ie_id::SSID => {
                    if ie_len <= 32 {
                        if let Ok(s) = core::str::from_utf8(&frame[pos..pos + ie_len]) {
                            let _ = ssid.push_str(s);
                        }
                    }
                }
                ie_id::DS_PARAMS => {
                    if ie_len >= 1 {
                        channel = frame[pos];
                    }
                }
                ie_id::RSN => {
                    is_wpa2 = true;
                }
                ie_id::VENDOR => {
                    if ie_len >= 4 && frame[pos..pos + 4] == WPA_OUI {
                        is_wpa = true;
                    }
                }
                _ => {}
            }
            
            pos += ie_len;
        }
        
        let is_open = !is_wpa && !is_wpa2 && (capability & 0x0010) == 0;
        
        Some(ScanResult {
            bssid,
            ssid,
            channel,
            rssi,
            capability,
            beacon_interval,
            is_wpa,
            is_wpa2,
            is_open,
        })
    }
}

/// Scan state for collecting results
pub struct ScanState {
    pub results: heapless::Vec<ScanResult, 32>,
    pub current_channel: u8,
    pub channels_to_scan: heapless::Vec<u8, 14>,
    pub active: bool,
}

impl ScanState {
    pub fn new() -> Self {
        Self {
            results: heapless::Vec::new(),
            current_channel: 1,
            channels_to_scan: heapless::Vec::new(),
            active: false,
        }
    }
    
    /// Start a new scan on specified channels
    pub fn start(&mut self, channels: &[u8]) {
        self.results.clear();
        self.channels_to_scan.clear();
        for &ch in channels {
            let _ = self.channels_to_scan.push(ch);
        }
        self.active = true;
        self.current_channel = channels.first().copied().unwrap_or(1);
    }
    
    /// Process received frame during scan
    pub fn process_frame(&mut self, frame: &[u8], rssi: i8) {
        if !self.active {
            return;
        }
        
        if let Some(result) = ScanResult::from_frame(frame, rssi) {
            // Check if we already have this BSSID
            let exists = self.results.iter().any(|r| r.bssid == result.bssid);
            if !exists && !self.results.is_full() {
                let _ = self.results.push(result);
            }
        }
    }
    
    /// Move to next channel, returns false when done
    pub fn next_channel(&mut self) -> Option<u8> {
        if let Some(pos) = self.channels_to_scan.iter().position(|&c| c == self.current_channel) {
            if pos + 1 < self.channels_to_scan.len() {
                self.current_channel = self.channels_to_scan[pos + 1];
                return Some(self.current_channel);
            }
        }
        self.active = false;
        None
    }
    
    /// Get best network by RSSI
    pub fn best_network(&self, ssid: &str) -> Option<&ScanResult> {
        self.results.iter()
            .filter(|r| r.ssid.as_str() == ssid)
            .max_by_key(|r| r.rssi)
    }
}

impl Default for Mt7601uDriver {
    fn default() -> Self {
        Self::new()
    }
}

impl Default for ScanState {
    fn default() -> Self {
        Self::new()
    }
}

/// 802.11 Authentication frame builder
pub struct AuthFrame;

impl AuthFrame {
    /// Build Open System authentication request (seq 1)
    pub fn build_request(sa: &[u8; 6], da: &[u8; 6], bssid: &[u8; 6]) -> Vec<u8> {
        let mut frame = Vec::with_capacity(30);
        
        // Frame Control: Authentication (0x00b0)
        frame.extend_from_slice(&[0xb0, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (AP)
        frame.extend_from_slice(da);
        // SA (us)
        frame.extend_from_slice(sa);
        // BSSID
        frame.extend_from_slice(bssid);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        
        // Authentication Algorithm: Open System (0)
        frame.extend_from_slice(&[0x00, 0x00]);
        // Authentication Sequence: 1
        frame.extend_from_slice(&[0x01, 0x00]);
        // Status Code: Success (0)
        frame.extend_from_slice(&[0x00, 0x00]);
        
        frame
    }
    
    /// Parse authentication response
    pub fn parse_response(frame: &[u8]) -> Option<AuthResponse> {
        if frame.len() < 30 {
            return None;
        }
        
        let fc = u16::from_le_bytes([frame[0], frame[1]]);
        if fc != frame_type::MGMT_AUTH {
            return None;
        }
        
        let algo = u16::from_le_bytes([frame[24], frame[25]]);
        let seq = u16::from_le_bytes([frame[26], frame[27]]);
        let status = u16::from_le_bytes([frame[28], frame[29]]);
        
        Some(AuthResponse { algo, seq, status })
    }
}

/// Authentication response
#[derive(Debug, Clone, Copy)]
pub struct AuthResponse {
    pub algo: u16,
    pub seq: u16,
    pub status: u16,
}

impl AuthResponse {
    pub fn is_success(&self) -> bool {
        self.status == 0 && self.seq == 2
    }
}

/// 802.11 Association frame builder
pub struct AssocFrame;

impl AssocFrame {
    /// Build association request
    pub fn build_request(
        sa: &[u8; 6],
        bssid: &[u8; 6],
        ssid: &[u8],
        supported_rates: &[u8],
        rsn_ie: Option<&[u8]>,
    ) -> Vec<u8> {
        let mut frame = Vec::with_capacity(128);
        
        // Frame Control: Association Request (0x0000)
        frame.extend_from_slice(&[0x00, 0x00]);
        // Duration
        frame.extend_from_slice(&[0x00, 0x00]);
        // DA (AP/BSSID)
        frame.extend_from_slice(bssid);
        // SA (us)
        frame.extend_from_slice(sa);
        // BSSID
        frame.extend_from_slice(bssid);
        // Sequence Control
        frame.extend_from_slice(&[0x00, 0x00]);
        
        // Capability Info: ESS, Short Preamble
        frame.extend_from_slice(&[0x21, 0x00]);
        // Listen Interval
        frame.extend_from_slice(&[0x0a, 0x00]);
        
        // SSID IE
        frame.push(ie_id::SSID);
        frame.push(ssid.len() as u8);
        frame.extend_from_slice(ssid);
        
        // Supported Rates IE
        frame.push(ie_id::SUPPORTED_RATES);
        let rate_len = supported_rates.len().min(8);
        frame.push(rate_len as u8);
        frame.extend_from_slice(&supported_rates[..rate_len]);
        
        // Extended Supported Rates if needed
        if supported_rates.len() > 8 {
            frame.push(50); // Extended Supported Rates ID
            let ext_len = supported_rates.len() - 8;
            frame.push(ext_len as u8);
            frame.extend_from_slice(&supported_rates[8..]);
        }
        
        // RSN IE for WPA2
        if let Some(rsn) = rsn_ie {
            frame.push(ie_id::RSN);
            frame.push(rsn.len() as u8);
            frame.extend_from_slice(rsn);
        }
        
        frame
    }
    
    /// Parse association response
    pub fn parse_response(frame: &[u8]) -> Option<AssocResponse> {
        if frame.len() < 30 {
            return None;
        }
        
        let fc = u16::from_le_bytes([frame[0], frame[1]]);
        if fc != frame_type::MGMT_ASSOC_RESP {
            return None;
        }
        
        let capability = u16::from_le_bytes([frame[24], frame[25]]);
        let status = u16::from_le_bytes([frame[26], frame[27]]);
        let aid = u16::from_le_bytes([frame[28], frame[29]]) & 0x3fff;
        
        Some(AssocResponse { capability, status, aid })
    }
}

/// Association response
#[derive(Debug, Clone, Copy)]
pub struct AssocResponse {
    pub capability: u16,
    pub status: u16,
    pub aid: u16,
}

impl AssocResponse {
    pub fn is_success(&self) -> bool {
        self.status == 0
    }
}

/// Default RSN IE for WPA2-PSK with CCMP
pub const WPA2_RSN_IE: &[u8] = &[
    0x01, 0x00,             // Version 1
    0x00, 0x0f, 0xac, 0x04, // Group Cipher: CCMP
    0x01, 0x00,             // Pairwise Cipher Count: 1
    0x00, 0x0f, 0xac, 0x04, // Pairwise Cipher: CCMP
    0x01, 0x00,             // AKM Count: 1
    0x00, 0x0f, 0xac, 0x02, // AKM: PSK
    0x00, 0x00,             // RSN Capabilities
];

/// Default supported rates for 802.11bgn
pub const DEFAULT_RATES: &[u8] = &[
    0x82, 0x84, 0x8b, 0x96,  // 1, 2, 5.5, 11 Mbps (basic)
    0x0c, 0x12, 0x18, 0x24,  // 6, 9, 12, 18 Mbps
    0x30, 0x48, 0x60, 0x6c,  // 24, 36, 48, 54 Mbps
];

/// Connection state machine
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ConnState {
    Disconnected,
    Authenticating,
    Authenticated,
    Associating,
    Associated,
    WpaHandshake,
    Connected,
}

/// Connection manager for MT7601U
pub struct ConnectionManager {
    pub state: ConnState,
    pub target_ssid: heapless::String<32>,
    pub target_bssid: [u8; 6],
    pub aid: u16,
    pub retry_count: u8,
}

impl ConnectionManager {
    pub fn new() -> Self {
        Self {
            state: ConnState::Disconnected,
            target_ssid: heapless::String::new(),
            target_bssid: [0; 6],
            aid: 0,
            retry_count: 0,
        }
    }
    
    /// Start connection to a network
    pub fn connect(&mut self, ssid: &str, bssid: [u8; 6]) {
        self.target_ssid.clear();
        let _ = self.target_ssid.push_str(ssid);
        self.target_bssid = bssid;
        self.state = ConnState::Authenticating;
        self.retry_count = 0;
    }
    
    /// Handle authentication response
    pub fn handle_auth_response(&mut self, resp: &AuthResponse) {
        if self.state != ConnState::Authenticating {
            return;
        }
        
        if resp.is_success() {
            self.state = ConnState::Authenticated;
        } else {
            self.retry_count += 1;
            if self.retry_count >= 3 {
                self.state = ConnState::Disconnected;
            }
        }
    }
    
    /// Handle association response
    pub fn handle_assoc_response(&mut self, resp: &AssocResponse) {
        if self.state != ConnState::Associating {
            return;
        }
        
        if resp.is_success() {
            self.aid = resp.aid;
            self.state = ConnState::Associated;
        } else {
            self.retry_count += 1;
            if self.retry_count >= 3 {
                self.state = ConnState::Disconnected;
            }
        }
    }
    
    /// Disconnect
    pub fn disconnect(&mut self) {
        self.state = ConnState::Disconnected;
        self.aid = 0;
    }
}

impl Default for ConnectionManager {
    fn default() -> Self {
        Self::new()
    }
}

/// High-level WiFi connection API integrating driver + WPA
pub struct WifiConnection {
    pub driver: Mt7601uDriver,
    pub scan_state: ScanState,
    pub conn_mgr: ConnectionManager,
    pub passphrase: heapless::String<64>,
}

impl WifiConnection {
    pub fn new() -> Self {
        Self {
            driver: Mt7601uDriver::new(),
            scan_state: ScanState::new(),
            conn_mgr: ConnectionManager::new(),
            passphrase: heapless::String::new(),
        }
    }
    
    /// Initialize the WiFi hardware
    pub fn init<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        device: &UsbDevice,
    ) -> Result<(), Mt7601uError> {
        self.driver.init(controller, device)?;
        self.driver.start(controller)?;
        Ok(())
    }
    
    /// Scan for available networks
    pub fn scan<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<&[ScanResult], Mt7601uError> {
        // Scan channels 1-11 (US)
        let channels: &[u8] = &[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
        self.scan_state.start(channels);
        
        self.driver.start_scan(controller)?;
        
        // Scan each channel
        while let Some(ch) = self.scan_state.next_channel() {
            self.driver.set_channel(controller, ch)?;
            
            // Send probe request
            self.driver.send_probe_request(controller, None)?;
            
            // Listen for responses (100ms per channel)
            for _ in 0..10 {
                let mut buf = [0u8; 2048];
                if let Ok(info) = self.driver.rx_frame(controller, &mut buf) {
                    self.scan_state.process_frame(&buf[..info.length], info.rssi);
                }
                self.driver.delay_ms(10);
            }
        }
        
        self.driver.stop_scan(controller)?;
        Ok(&self.scan_state.results)
    }
    
    /// Connect to a WPA2 network
    pub fn connect<C: UsbHostController>(
        &mut self,
        controller: &mut C,
        ssid: &str,
        passphrase: &str,
    ) -> Result<(), Mt7601uError> {
        // Find best AP for this SSID
        let network = self.scan_state.best_network(ssid)
            .ok_or(Mt7601uError::NetworkNotFound)?;
        
        if !network.is_wpa2 && !network.is_wpa && !network.is_open {
            return Err(Mt7601uError::UnsupportedSecurity);
        }
        
        let bssid = network.bssid;
        let channel = network.channel;
        
        // Store passphrase for WPA handshake
        self.passphrase.clear();
        let _ = self.passphrase.push_str(passphrase);
        
        // Set channel
        self.driver.set_channel(controller, channel)?;
        
        // Start connection
        self.conn_mgr.connect(ssid, bssid);
        
        // Send authentication request
        let auth_frame = AuthFrame::build_request(
            &self.driver.mac_address(),
            &bssid,
            &bssid,
        );
        self.driver.tx_frame(controller, &auth_frame)?;
        
        // Wait for auth response
        for _ in 0..50 {
            let mut buf = [0u8; 2048];
            if let Ok(info) = self.driver.rx_frame(controller, &mut buf) {
                if let Some(resp) = AuthFrame::parse_response(&buf[..info.length]) {
                    self.conn_mgr.handle_auth_response(&resp);
                    break;
                }
            }
            self.driver.delay_ms(20);
        }
        
        if self.conn_mgr.state != ConnState::Authenticated {
            return Err(Mt7601uError::AuthFailed);
        }
        
        // Send association request
        self.conn_mgr.state = ConnState::Associating;
        let rsn_ie = if network.is_wpa2 { Some(WPA2_RSN_IE) } else { None };
        let assoc_frame = AssocFrame::build_request(
            &self.driver.mac_address(),
            &bssid,
            ssid.as_bytes(),
            DEFAULT_RATES,
            rsn_ie,
        );
        self.driver.tx_frame(controller, &assoc_frame)?;
        
        // Wait for assoc response
        for _ in 0..50 {
            let mut buf = [0u8; 2048];
            if let Ok(info) = self.driver.rx_frame(controller, &mut buf) {
                if let Some(resp) = AssocFrame::parse_response(&buf[..info.length]) {
                    self.conn_mgr.handle_assoc_response(&resp);
                    break;
                }
            }
            self.driver.delay_ms(20);
        }
        
        if self.conn_mgr.state != ConnState::Associated {
            return Err(Mt7601uError::AssocFailed);
        }
        
        // Set BSSID in hardware
        self.driver.set_bssid(controller, bssid)?;
        
        // For WPA2, wait for EAPOL handshake
        if network.is_wpa2 || network.is_wpa {
            self.conn_mgr.state = ConnState::WpaHandshake;
            // WPA handshake handled by wpa.rs WpaSupplicant
            // Driver just needs to pass EAPOL frames
        } else {
            self.conn_mgr.state = ConnState::Connected;
        }
        
        Ok(())
    }
    
    /// Check if connected
    pub fn is_connected(&self) -> bool {
        self.conn_mgr.state == ConnState::Connected
    }
    
    /// Disconnect from network
    pub fn disconnect<C: UsbHostController>(
        &mut self,
        controller: &mut C,
    ) -> Result<(), Mt7601uError> {
        self.conn_mgr.disconnect();
        self.driver.stop(controller)?;
        Ok(())
    }
}

impl Default for WifiConnection {
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
    TxFailed,
    RxFailed,
    NetworkNotFound,
    UnsupportedSecurity,
    AuthFailed,
    AssocFailed,
}
