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
        
        // TODO: Parse firmware sections and load to device
        // - Instruction memory (IRAM)
        // - Data memory (DRAM)
        // - Calibration data
        
        self.fw_state = FirmwareState::Loaded;
        self.state = IwlState::FirmwareLoaded;
        
        Ok(())
    }
    
    /// Start firmware execution
    pub fn start_firmware(&mut self) -> Result<(), IwlError> {
        if self.fw_state != FirmwareState::Loaded {
            return Err(IwlError::InvalidState);
        }
        
        // TODO: Trigger firmware execution
        // - Set DRAM_INT_TBL_REG
        // - Clear GP_CNTRL sleep bit
        // - Wait for INIT_DONE
        
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
        
        // TODO: Build command TFD (Transfer Descriptor)
        // TODO: Submit to TX queue 4 (command queue)
        // TODO: Wait for response
        
        let _ = header;
        
        Ok(())
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
    pub fn connect(&mut self, _ssid: &[u8], _bssid: &[u8; 6]) -> Result<(), IwlError> {
        if self.state != IwlState::Ready && self.state != IwlState::Scanning {
            return Err(IwlError::InvalidState);
        }
        
        self.state = IwlState::Connecting;
        
        // TODO: Send PHY_CONTEXT command
        // TODO: Send MAC_CONTEXT command
        // TODO: Send BINDING command
        // TODO: Send ADD_STA command
        // TODO: Send RXON command
        
        self.state = IwlState::Connected;
        
        Ok(())
    }
    
    /// Disconnect from network
    pub fn disconnect(&mut self) -> Result<(), IwlError> {
        if self.state != IwlState::Connected {
            return Ok(());
        }
        
        // TODO: Send REMOVE_STA command
        // TODO: Send RXON with no BSSID
        
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
