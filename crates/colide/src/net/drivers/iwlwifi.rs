//! Intel WiFi Driver (iwlwifi) for Colide OS
//!
//! Skeleton driver for Intel WiFi 6/6E chips (AX200, AX210, AX211).
//! Based on Fuchsia's iwlwifi port and Linux iwlwifi architecture.
//!
//! Intel WiFi uses a complex firmware-based architecture with:
//! - PCIe interface for register access and DMA
//! - Firmware image loaded at runtime
//! - Command/response protocol over shared memory
//! - Multiple TX/RX queues for QoS

use crate::pcie::{PcieDevice, PcieAddress, PcieError, MmioRegion};
use crate::pcie::device::IntelWifiIds;
use crate::net::firmware::{FirmwareLoader, CombinedFirmwareLoader, FirmwareError};
use heapless::Vec as HVec;

/// Intel WiFi vendor ID
pub const INTEL_VENDOR_ID: u16 = 0x8086;

/// iwlwifi register offsets (from CSR - Control and Status Registers)
pub mod Csr {
    pub const HW_IF_CONFIG_REG: u32 = 0x000;
    pub const INT_COALESCING: u32 = 0x004;
    pub const INT: u32 = 0x008;
    pub const INT_MASK: u32 = 0x00C;
    pub const FH_INT_STATUS: u32 = 0x010;
    pub const GPIO_IN: u32 = 0x018;
    pub const RESET: u32 = 0x020;
    pub const GP_CNTRL: u32 = 0x024;
    pub const HW_REV: u32 = 0x028;
    pub const EEPROM_REG: u32 = 0x02C;
    pub const EEPROM_GP: u32 = 0x030;
    pub const OTP_GP_REG: u32 = 0x034;
    pub const GIO_REG: u32 = 0x03C;
    pub const GP_UCODE_REG: u32 = 0x048;
    pub const GP_DRIVER_REG: u32 = 0x050;
    pub const UCODE_DRV_GP1: u32 = 0x054;
    pub const UCODE_DRV_GP2: u32 = 0x058;
    pub const DRAM_INT_TBL_REG: u32 = 0x0A0;
    pub const MAC_SHADOW_REG_CTRL: u32 = 0x0A8;
    pub const FW_MEM_DUMP_REG: u32 = 0x0B0;
}

/// GP_CNTRL register bits
pub mod GpCntrl {
    pub const MAC_ACCESS_ENA: u32 = 0x00000001;
    pub const MAC_CLOCK_READY: u32 = 0x00000002;
    pub const INIT_DONE: u32 = 0x00000004;
    pub const MAC_ACCESS_REQ: u32 = 0x00000008;
    pub const SLEEP_NOW: u32 = 0x00000010;
    pub const REG_FLAG_MAC_CLOCK_READY: u32 = 0x00000080;
    pub const REG_FLAG_HW_RF_KILL_SW: u32 = 0x08000000;
}

/// Hardware revision info
#[derive(Debug, Clone, Copy)]
pub struct HwRevision {
    pub hw_rev: u32,
    pub hw_rev_step: u8,
}

impl HwRevision {
    pub fn from_reg(reg: u32) -> Self {
        Self {
            hw_rev: reg,
            hw_rev_step: ((reg >> 2) & 0x3) as u8,
        }
    }
    
    pub fn is_ax200(&self) -> bool {
        // AX200 family detection
        matches!(self.hw_rev & 0xFFFF, 0x0310 | 0x0318)
    }
    
    pub fn is_ax210(&self) -> bool {
        // AX210/AX211 family detection
        matches!(self.hw_rev & 0xFFFF, 0x0510 | 0x0514)
    }
}

/// Firmware status
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum FirmwareState {
    NotLoaded,
    Loading,
    Loaded,
    Running,
    Error,
}

/// iwlwifi driver state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum IwlState {
    Uninitialized,
    Probed,
    FirmwareLoaded,
    Ready,
    Scanning,
    Connecting,
    Connected,
    Error,
}

/// iwlwifi driver error types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum IwlError {
    NotInitialized,
    HardwareNotFound,
    PcieError,
    FirmwareError,
    Timeout,
    RfKill,
    NicError,
    CommandFailed,
    InvalidState,
    NoMemory,
}

impl From<PcieError> for IwlError {
    fn from(_: PcieError) -> Self {
        IwlError::PcieError
    }
}

impl From<FirmwareError> for IwlError {
    fn from(_: FirmwareError) -> Self {
        IwlError::FirmwareError
    }
}

/// TX queue for transmitting packets
pub struct TxQueue {
    pub id: u8,
    pub entries: u16,
    pub write_ptr: u16,
    pub read_ptr: u16,
}

impl TxQueue {
    pub fn new(id: u8, entries: u16) -> Self {
        Self {
            id,
            entries,
            write_ptr: 0,
            read_ptr: 0,
        }
    }
    
    pub fn is_full(&self) -> bool {
        let next = (self.write_ptr + 1) % self.entries;
        next == self.read_ptr
    }
    
    pub fn advance_write(&mut self) {
        self.write_ptr = (self.write_ptr + 1) % self.entries;
    }
}

/// RX queue for receiving packets
pub struct RxQueue {
    pub entries: u16,
    pub write_ptr: u16,
    pub read_ptr: u16,
}

impl RxQueue {
    pub fn new(entries: u16) -> Self {
        Self {
            entries,
            write_ptr: 0,
            read_ptr: 0,
        }
    }
    
    pub fn has_data(&self) -> bool {
        self.read_ptr != self.write_ptr
    }
    
    pub fn advance_read(&mut self) {
        self.read_ptr = (self.read_ptr + 1) % self.entries;
    }
}

/// Host command opcodes
pub mod HostCmd {
    pub const SCAN_REQ: u8 = 0x80;
    pub const SCAN_ABORT: u8 = 0x81;
    pub const SCAN_COMPLETE: u8 = 0x84;
    pub const RXON: u8 = 0x10;
    pub const RXON_ASSOC: u8 = 0x11;
    pub const TX_CMD: u8 = 0x1C;
    pub const REPLY_TX: u8 = 0x1D;
    pub const POWER_TABLE: u8 = 0x77;
    pub const PHY_CONTEXT: u8 = 0x08;
    pub const MAC_CONTEXT: u8 = 0x28;
    pub const BINDING: u8 = 0x29;
    pub const ADD_STA: u8 = 0x18;
    pub const REMOVE_STA: u8 = 0x19;
}

/// Host command header
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct HostCmdHeader {
    pub cmd: u8,
    pub group_id: u8,
    pub sequence: u16,
    pub length: u16,
    pub reserved: u16,
}

impl HostCmdHeader {
    pub fn new(cmd: u8, length: u16, sequence: u16) -> Self {
        Self {
            cmd,
            group_id: 0,
            sequence,
            length,
            reserved: 0,
        }
    }
}

/// Intel WiFi driver
pub struct IwlwifiDriver {
    device: Option<PcieDevice>,
    mmio: Option<MmioRegion>,
    state: IwlState,
    fw_state: FirmwareState,
    hw_rev: HwRevision,
    tx_queues: [Option<TxQueue>; 32],
    rx_queue: Option<RxQueue>,
    cmd_sequence: u16,
    rf_kill: bool,
    mac_addr: [u8; 6],
}

impl IwlwifiDriver {
    /// Create new uninitialized driver
    pub const fn new() -> Self {
        Self {
            device: None,
            mmio: None,
            state: IwlState::Uninitialized,
            fw_state: FirmwareState::NotLoaded,
            hw_rev: HwRevision { hw_rev: 0, hw_rev_step: 0 },
            tx_queues: [const { None }; 32],
            rx_queue: None,
            cmd_sequence: 0,
            rf_kill: false,
            mac_addr: [0; 6],
        }
    }
    
    /// Probe for Intel WiFi hardware
    pub fn probe() -> Result<Self, IwlError> {
        use crate::pcie::device::find_intel_wifi;
        
        let device = find_intel_wifi()?.ok_or(IwlError::HardwareNotFound)?;
        
        // Verify it's a supported device
        if !IntelWifiIds::is_intel_wifi(device.device_id) {
            return Err(IwlError::HardwareNotFound);
        }
        
        // Get memory BAR
        let bar = device.memory_bar().ok_or(IwlError::HardwareNotFound)?;
        let mmio = MmioRegion::new(bar.base, bar.size);
        
        // Enable bus mastering and memory space
        device.enable_bus_master()?;
        device.enable_memory_space()?;
        
        let mut driver = Self {
            device: Some(device),
            mmio: Some(mmio),
            state: IwlState::Probed,
            ..Self::new()
        };
        
        // Read hardware revision
        driver.hw_rev = HwRevision::from_reg(driver.read_csr(Csr::HW_REV));
        
        Ok(driver)
    }
    
    /// Read CSR register
    fn read_csr(&self, offset: u32) -> u32 {
        self.mmio.as_ref().map(|m| m.read32(offset as u64)).unwrap_or(0)
    }
    
    /// Write CSR register
    fn write_csr(&self, offset: u32, value: u32) {
        if let Some(ref mmio) = self.mmio {
            mmio.write32(offset as u64, value);
        }
    }
    
    /// Check if RF kill switch is active
    pub fn is_rf_kill(&self) -> bool {
        let gp = self.read_csr(Csr::GP_CNTRL);
        (gp & GpCntrl::REG_FLAG_HW_RF_KILL_SW) == 0
    }
    
    /// Request MAC access
    fn request_mac_access(&self) -> Result<(), IwlError> {
        self.write_csr(Csr::GP_CNTRL, 
            self.read_csr(Csr::GP_CNTRL) | GpCntrl::MAC_ACCESS_REQ);
        
        // Wait for MAC clock ready
        for _ in 0..1000 {
            if (self.read_csr(Csr::GP_CNTRL) & GpCntrl::REG_FLAG_MAC_CLOCK_READY) != 0 {
                return Ok(());
            }
            // Busy wait
            for _ in 0..100 {
                core::hint::spin_loop();
            }
        }
        
        Err(IwlError::Timeout)
    }
    
    /// Release MAC access
    fn release_mac_access(&self) {
        self.write_csr(Csr::GP_CNTRL,
            self.read_csr(Csr::GP_CNTRL) & !GpCntrl::MAC_ACCESS_REQ);
    }
    
    /// Initialize the NIC
    pub fn init(&mut self) -> Result<(), IwlError> {
        if self.state == IwlState::Uninitialized {
            return Err(IwlError::NotInitialized);
        }
        
        // Check RF kill
        if self.is_rf_kill() {
            self.rf_kill = true;
            return Err(IwlError::RfKill);
        }
        
        // Request MAC access
        self.request_mac_access()?;
        
        // Reset the NIC
        self.write_csr(Csr::RESET, 0x80000000);
        
        // Wait for reset complete
        for _ in 0..100 {
            if (self.read_csr(Csr::RESET) & 0x80000000) == 0 {
                break;
            }
            for _ in 0..1000 {
                core::hint::spin_loop();
            }
        }
        
        // Initialize TX queues (32 queues for QoS)
        for i in 0..32 {
            self.tx_queues[i] = Some(TxQueue::new(i as u8, 256));
        }
        
        // Initialize RX queue
        self.rx_queue = Some(RxQueue::new(256));
        
        Ok(())
    }
    
    /// Load firmware
    pub fn load_firmware(&mut self) -> Result<(), IwlError> {
        let loader = CombinedFirmwareLoader::new();
        
        // Determine firmware file based on hardware revision
        let fw_name = if self.hw_rev.is_ax210() {
            "iwlwifi-ty-a0-gf-a0.ucode"
        } else if self.hw_rev.is_ax200() {
            "iwlwifi-cc-a0-72.ucode"
        } else {
            "iwlwifi-so-a0-gf-a0.ucode"
        };
        
        self.fw_state = FirmwareState::Loading;
        
        // Load firmware
        let _fw_data = loader.load(fw_name)?;
        
        // Parse firmware sections and load to device
        self.parse_and_load_firmware(&_fw_data)?;
        
        self.fw_state = FirmwareState::Loaded;
        self.state = IwlState::FirmwareLoaded;
        
        Ok(())
    }
    
    /// Start firmware execution
    pub fn start_firmware(&mut self) -> Result<(), IwlError> {
        if self.fw_state != FirmwareState::Loaded {
            return Err(IwlError::InvalidState);
        }
        
        // Trigger firmware execution
        self.trigger_fw_execution()?;
        
        self.fw_state = FirmwareState::Running;
        self.state = IwlState::Ready;
        
        Ok(())
    }
    
    /// Send host command to firmware
    fn send_cmd(&mut self, cmd: u8, data: &[u8]) -> Result<(), IwlError> {
        if self.fw_state != FirmwareState::Running {
            return Err(IwlError::InvalidState);
        }
        
        let header = HostCmdHeader::new(cmd, data.len() as u16, self.cmd_sequence);
        self.cmd_sequence = self.cmd_sequence.wrapping_add(1);
        
        // Build command TFD (Transfer Descriptor) and submit to command queue
        self.submit_command(&header, data)?;
        
        // Wait for response
        self.wait_cmd_response()?;
        
        Ok(())
    }
    
    /// Submit command to TX queue 4 (command queue)
    fn submit_command(&mut self, header: &HostCmdHeader, data: &[u8]) -> Result<(), IwlError> {
        if self.mmio_base == 0 {
            return Err(IwlError::NotInitialized);
        }
        
        // Build TFD (Transfer Frame Descriptor)
        // TFD format: header + data in contiguous buffer
        let mut cmd_buf = [0u8; 256];
        let header_bytes = header.to_bytes();
        cmd_buf[..header_bytes.len()].copy_from_slice(&header_bytes);
        let data_len = data.len().min(256 - header_bytes.len());
        cmd_buf[header_bytes.len()..header_bytes.len() + data_len].copy_from_slice(&data[..data_len]);
        
        let base = self.mmio_base as *mut u32;
        
        unsafe {
            // Write TFD to command queue (queue 4)
            // FH_MEM_WCSR_CHNL0 + queue_offset
            let queue_base = 0x1000 + 4 * 0x100; // Queue 4
            
            // Write buffer address (simplified - real impl needs DMA)
            core::ptr::write_volatile(base.add(queue_base / 4), cmd_buf.as_ptr() as u32);
            
            // Write length
            core::ptr::write_volatile(base.add((queue_base + 4) / 4), (header_bytes.len() + data_len) as u32);
            
            // Kick queue
            core::ptr::write_volatile(base.add((queue_base + 8) / 4), 1);
        }
        
        Ok(())
    }
    
    /// Wait for command response
    fn wait_cmd_response(&self) -> Result<(), IwlError> {
        if self.mmio_base == 0 {
            return Err(IwlError::NotInitialized);
        }
        
        let base = self.mmio_base as *const u32;
        
        unsafe {
            // Poll for command completion
            for _ in 0..100000 {
                let status = core::ptr::read_volatile(base.add(0x28 / 4));
                if status & 0x1 != 0 {
                    return Ok(());
                }
            }
        }
        
        Err(IwlError::Timeout)
    }
    
    /// Start a scan
    pub fn scan(&mut self) -> Result<(), IwlError> {
        if self.state != IwlState::Ready {
            return Err(IwlError::InvalidState);
        }
        
        self.state = IwlState::Scanning;
        
        // Build scan request
        let scan_req = [0u8; 64]; // Simplified scan request
        self.send_cmd(HostCmd::SCAN_REQ, &scan_req)?;
        
        Ok(())
    }
    
    /// Connect to a network
    pub fn connect(&mut self, ssid: &[u8], bssid: &[u8; 6]) -> Result<(), IwlError> {
        if self.state != IwlState::Ready && self.state != IwlState::Scanning {
            return Err(IwlError::InvalidState);
        }
        
        self.state = IwlState::Connecting;
        
        // Send PHY_CONTEXT command - configure physical layer
        let mut phy_ctx = [0u8; 32];
        phy_ctx[0] = 1; // PHY context ID
        phy_ctx[1] = 1; // Action: ADD
        self.send_cmd(HostCmd::PHY_CONTEXT, &phy_ctx)?;
        
        // Send MAC_CONTEXT command - configure MAC layer
        let mut mac_ctx = [0u8; 64];
        mac_ctx[0] = 1; // MAC context ID
        mac_ctx[1] = 1; // Action: ADD
        mac_ctx[2] = 3; // Type: BSS_STA
        mac_ctx[8..14].copy_from_slice(bssid);
        self.send_cmd(HostCmd::MAC_CONTEXT, &mac_ctx)?;
        
        // Send BINDING command - bind PHY to MAC
        let mut binding = [0u8; 16];
        binding[0] = 1; // Binding ID
        binding[1] = 1; // Action: ADD
        binding[2] = 1; // PHY context ID
        binding[3] = 1; // MAC context ID
        self.send_cmd(HostCmd::BINDING, &binding)?;
        
        // Send ADD_STA command - add AP as station
        let mut add_sta = [0u8; 64];
        add_sta[0] = 0; // Station ID
        add_sta[1] = 1; // Action: ADD
        add_sta[8..14].copy_from_slice(bssid);
        self.send_cmd(HostCmd::ADD_STA, &add_sta)?;
        
        // Send RXON command - enable reception
        let mut rxon = [0u8; 48];
        rxon[0..6].copy_from_slice(bssid);
        let ssid_len = ssid.len().min(32);
        rxon[8] = ssid_len as u8;
        rxon[9..9 + ssid_len].copy_from_slice(&ssid[..ssid_len]);
        self.send_cmd(HostCmd::RXON, &rxon)?;
        
        self.state = IwlState::Connected;
        
        Ok(())
    }
    
    /// Disconnect from network
    pub fn disconnect(&mut self) -> Result<(), IwlError> {
        if self.state != IwlState::Connected {
            return Ok(());
        }
        
        // Send REMOVE_STA command
        let mut rm_sta = [0u8; 8];
        rm_sta[0] = 0; // Station ID
        rm_sta[1] = 1; // Action: REMOVE
        self.send_cmd(HostCmd::REMOVE_STA, &rm_sta)?;
        
        // Send RXON with no BSSID (disable)
        let rxon = [0u8; 48];
        self.send_cmd(HostCmd::RXON, &rxon)?;
        
        self.state = IwlState::Ready;
        
        Ok(())
    }
    
    /// Get driver state
    pub fn state(&self) -> IwlState {
        self.state
    }
    
    /// Get firmware state
    pub fn firmware_state(&self) -> FirmwareState {
        self.fw_state
    }
    
    /// Get hardware revision
    pub fn hw_revision(&self) -> HwRevision {
        self.hw_rev
    }
    
    /// Get MAC address
    pub fn mac_address(&self) -> [u8; 6] {
        self.mac_addr
    }
    
    /// Parse firmware and load sections to device
    fn parse_and_load_firmware(&mut self, fw_data: &[u8]) -> Result<(), IwlError> {
        if fw_data.len() < 16 {
            return Err(IwlError::FirmwareError);
        }
        
        // Intel firmware header format (simplified)
        // Magic: 4 bytes
        // Version: 4 bytes
        // Num sections: 4 bytes
        // Each section: type(4) + offset(4) + size(4)
        
        let magic = u32::from_le_bytes([fw_data[0], fw_data[1], fw_data[2], fw_data[3]]);
        if magic != 0x57464949 { // "IWFW"
            return Err(IwlError::FirmwareError);
        }
        
        // Load IRAM section
        if let Some(iram) = self.find_fw_section(fw_data, 1) {
            self.write_mem(0x00000000, iram)?;
        }
        
        // Load DRAM section
        if let Some(dram) = self.find_fw_section(fw_data, 2) {
            self.write_mem(0x00400000, dram)?;
        }
        
        Ok(())
    }
    
    /// Find firmware section by type
    fn find_fw_section(&self, fw_data: &[u8], section_type: u32) -> Option<&[u8]> {
        if fw_data.len() < 16 {
            return None;
        }
        
        let num_sections = u32::from_le_bytes([fw_data[8], fw_data[9], fw_data[10], fw_data[11]]) as usize;
        let mut offset = 12;
        
        for _ in 0..num_sections {
            if offset + 12 > fw_data.len() {
                break;
            }
            
            let sec_type = u32::from_le_bytes([fw_data[offset], fw_data[offset+1], fw_data[offset+2], fw_data[offset+3]]);
            let sec_offset = u32::from_le_bytes([fw_data[offset+4], fw_data[offset+5], fw_data[offset+6], fw_data[offset+7]]) as usize;
            let sec_size = u32::from_le_bytes([fw_data[offset+8], fw_data[offset+9], fw_data[offset+10], fw_data[offset+11]]) as usize;
            
            if sec_type == section_type {
                if sec_offset + sec_size <= fw_data.len() {
                    return Some(&fw_data[sec_offset..sec_offset + sec_size]);
                }
            }
            
            offset += 12;
        }
        
        None
    }
    
    /// Write to device memory
    fn write_mem(&mut self, addr: u32, data: &[u8]) -> Result<(), IwlError> {
        if self.mmio_base == 0 {
            return Err(IwlError::NotInitialized);
        }
        
        // Write memory through HBUS_TARG_MEM registers
        let base = self.mmio_base as *mut u32;
        
        unsafe {
            for (i, chunk) in data.chunks(4).enumerate() {
                let word = if chunk.len() == 4 {
                    u32::from_le_bytes([chunk[0], chunk[1], chunk[2], chunk[3]])
                } else {
                    let mut buf = [0u8; 4];
                    buf[..chunk.len()].copy_from_slice(chunk);
                    u32::from_le_bytes(buf)
                };
                
                // HBUS_TARG_MEM_WADDR = 0x410000
                // HBUS_TARG_MEM_WDAT = 0x418000
                core::ptr::write_volatile(base.add(0x410000 / 4), addr + (i as u32) * 4);
                core::ptr::write_volatile(base.add(0x418000 / 4), word);
            }
        }
        
        Ok(())
    }
    
    /// Trigger firmware execution
    fn trigger_fw_execution(&mut self) -> Result<(), IwlError> {
        if self.mmio_base == 0 {
            return Err(IwlError::NotInitialized);
        }
        
        let base = self.mmio_base as *mut u32;
        
        unsafe {
            // Clear GP_CNTRL sleep bit
            let gp_cntrl = core::ptr::read_volatile(base.add(0x24 / 4));
            core::ptr::write_volatile(base.add(0x24 / 4), gp_cntrl & !0x10);
            
            // Wait for INIT_DONE
            for _ in 0..10000 {
                let status = core::ptr::read_volatile(base.add(0x20 / 4));
                if status & 0x4 != 0 {
                    return Ok(());
                }
            }
        }
        
        Err(IwlError::Timeout)
    }
}

impl Default for IwlwifiDriver {
    fn default() -> Self {
        Self::new()
    }
}

/// Supported Intel WiFi device list
pub const SUPPORTED_DEVICES: &[(u16, &str)] = &[
    (IntelWifiIds::AX200_1, "Intel Wi-Fi 6 AX200"),
    (IntelWifiIds::AX200_2, "Intel Wi-Fi 6 AX200 (2)"),
    (IntelWifiIds::AX210_1, "Intel Wi-Fi 6E AX210"),
    (IntelWifiIds::AX210_2, "Intel Wi-Fi 6E AX210 (2)"),
    (IntelWifiIds::AX211_1, "Intel Wi-Fi 6E AX211"),
    (IntelWifiIds::AX211_2, "Intel Wi-Fi 6E AX211 (2)"),
    (IntelWifiIds::AX211_3, "Intel Wi-Fi 6E AX211 (3)"),
    (IntelWifiIds::AX411, "Intel Wi-Fi 6E AX411 vPro"),
];

/// Get device name from device ID
pub fn device_name(device_id: u16) -> Option<&'static str> {
    SUPPORTED_DEVICES.iter()
        .find(|(id, _)| *id == device_id)
        .map(|(_, name)| *name)
}
