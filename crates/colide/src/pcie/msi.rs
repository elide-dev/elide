//! MSI and MSI-X Interrupt Support for PCIe
//!
//! Message Signaled Interrupts allow PCIe devices to signal interrupts
//! by writing to a memory address rather than using dedicated IRQ lines.
//! This provides better performance and scalability for modern devices.

use super::config::{PcieAddress, Capability, read_config16, read_config32, write_config16, write_config32};
use super::PcieError;

/// MSI capability structure offsets
pub mod MsiRegs {
    pub const CONTROL: u8 = 0x02;
    pub const ADDRESS_LO: u8 = 0x04;
    pub const ADDRESS_HI: u8 = 0x08;
    pub const DATA: u8 = 0x08;  // Or 0x0C if 64-bit capable
    pub const DATA_64: u8 = 0x0C;
    pub const MASK: u8 = 0x10;
    pub const PENDING: u8 = 0x14;
}

/// MSI control register bits
pub mod MsiControl {
    pub const ENABLE: u16 = 0x0001;
    pub const MULTI_MSG_CAPABLE_MASK: u16 = 0x000E;
    pub const MULTI_MSG_ENABLE_MASK: u16 = 0x0070;
    pub const CAP_64BIT: u16 = 0x0080;
    pub const PER_VECTOR_MASK: u16 = 0x0100;
}

/// MSI-X capability structure offsets
pub mod MsixRegs {
    pub const CONTROL: u8 = 0x02;
    pub const TABLE_OFFSET: u8 = 0x04;
    pub const PBA_OFFSET: u8 = 0x08;
}

/// MSI-X control register bits
pub mod MsixControl {
    pub const ENABLE: u16 = 0x8000;
    pub const FUNCTION_MASK: u16 = 0x4000;
    pub const TABLE_SIZE_MASK: u16 = 0x07FF;
}

/// MSI-X table entry structure
#[repr(C)]
#[derive(Debug, Clone, Copy, Default)]
pub struct MsixTableEntry {
    pub msg_addr_lo: u32,
    pub msg_addr_hi: u32,
    pub msg_data: u32,
    pub vector_ctrl: u32,
}

impl MsixTableEntry {
    /// Check if entry is masked
    pub fn is_masked(&self) -> bool {
        (self.vector_ctrl & 0x01) != 0
    }
    
    /// Mask this entry
    pub fn mask(&mut self) {
        self.vector_ctrl |= 0x01;
    }
    
    /// Unmask this entry
    pub fn unmask(&mut self) {
        self.vector_ctrl &= !0x01;
    }
}

/// MSI configuration for a device
#[derive(Debug, Clone)]
pub struct MsiConfig {
    pub cap_offset: u8,
    pub is_64bit: bool,
    pub max_vectors: u8,
    pub allocated_vectors: u8,
    pub enabled: bool,
}

impl MsiConfig {
    /// Read MSI configuration from device
    pub fn from_device(addr: PcieAddress, cap_offset: u8) -> Result<Self, PcieError> {
        let control = read_config16(addr, cap_offset + MsiRegs::CONTROL)?;
        
        let is_64bit = (control & MsiControl::CAP_64BIT) != 0;
        let max_vectors = 1 << ((control & MsiControl::MULTI_MSG_CAPABLE_MASK) >> 1);
        
        Ok(Self {
            cap_offset,
            is_64bit,
            max_vectors,
            allocated_vectors: 0,
            enabled: (control & MsiControl::ENABLE) != 0,
        })
    }
    
    /// Configure MSI for device
    pub fn configure(&mut self, addr: PcieAddress, msg_addr: u64, msg_data: u16, vectors: u8) -> Result<(), PcieError> {
        // Disable MSI first
        let control = read_config16(addr, self.cap_offset + MsiRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsiRegs::CONTROL, control & !MsiControl::ENABLE)?;
        
        // Set address
        write_config32(addr, self.cap_offset + MsiRegs::ADDRESS_LO, msg_addr as u32)?;
        
        if self.is_64bit {
            write_config32(addr, self.cap_offset + MsiRegs::ADDRESS_HI, (msg_addr >> 32) as u32)?;
            write_config16(addr, self.cap_offset + MsiRegs::DATA_64, msg_data)?;
        } else {
            write_config16(addr, self.cap_offset + MsiRegs::DATA, msg_data)?;
        }
        
        // Set number of vectors
        let log2_vectors = vectors.trailing_zeros().min(3) as u16;
        let new_control = (control & !MsiControl::MULTI_MSG_ENABLE_MASK) | (log2_vectors << 4);
        
        // Enable MSI
        write_config16(addr, self.cap_offset + MsiRegs::CONTROL, new_control | MsiControl::ENABLE)?;
        
        self.allocated_vectors = vectors;
        self.enabled = true;
        
        Ok(())
    }
    
    /// Disable MSI
    pub fn disable(&mut self, addr: PcieAddress) -> Result<(), PcieError> {
        let control = read_config16(addr, self.cap_offset + MsiRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsiRegs::CONTROL, control & !MsiControl::ENABLE)?;
        self.enabled = false;
        Ok(())
    }
}

/// MSI-X configuration for a device
#[derive(Debug, Clone)]
pub struct MsixConfig {
    pub cap_offset: u8,
    pub table_size: u16,
    pub table_bar: u8,
    pub table_offset: u32,
    pub pba_bar: u8,
    pub pba_offset: u32,
    pub enabled: bool,
}

impl MsixConfig {
    /// Read MSI-X configuration from device
    pub fn from_device(addr: PcieAddress, cap_offset: u8) -> Result<Self, PcieError> {
        let control = read_config16(addr, cap_offset + MsixRegs::CONTROL)?;
        let table_info = read_config32(addr, cap_offset + MsixRegs::TABLE_OFFSET)?;
        let pba_info = read_config32(addr, cap_offset + MsixRegs::PBA_OFFSET)?;
        
        let table_size = (control & MsixControl::TABLE_SIZE_MASK) + 1;
        let table_bar = (table_info & 0x07) as u8;
        let table_offset = table_info & !0x07;
        let pba_bar = (pba_info & 0x07) as u8;
        let pba_offset = pba_info & !0x07;
        
        Ok(Self {
            cap_offset,
            table_size,
            table_bar,
            table_offset,
            pba_bar,
            pba_offset,
            enabled: (control & MsixControl::ENABLE) != 0,
        })
    }
    
    /// Enable MSI-X
    pub fn enable(&mut self, addr: PcieAddress) -> Result<(), PcieError> {
        let control = read_config16(addr, self.cap_offset + MsixRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsixRegs::CONTROL, 
            (control & !MsixControl::FUNCTION_MASK) | MsixControl::ENABLE)?;
        self.enabled = true;
        Ok(())
    }
    
    /// Disable MSI-X
    pub fn disable(&mut self, addr: PcieAddress) -> Result<(), PcieError> {
        let control = read_config16(addr, self.cap_offset + MsixRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsixRegs::CONTROL, control & !MsixControl::ENABLE)?;
        self.enabled = false;
        Ok(())
    }
    
    /// Mask all vectors
    pub fn mask_all(&self, addr: PcieAddress) -> Result<(), PcieError> {
        let control = read_config16(addr, self.cap_offset + MsixRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsixRegs::CONTROL, control | MsixControl::FUNCTION_MASK)?;
        Ok(())
    }
    
    /// Unmask all vectors
    pub fn unmask_all(&self, addr: PcieAddress) -> Result<(), PcieError> {
        let control = read_config16(addr, self.cap_offset + MsixRegs::CONTROL)?;
        write_config16(addr, self.cap_offset + MsixRegs::CONTROL, control & !MsixControl::FUNCTION_MASK)?;
        Ok(())
    }
}

/// Interrupt delivery mode
#[derive(Debug, Clone)]
pub enum InterruptMode {
    /// Legacy INTx interrupts
    Legacy,
    /// MSI (Message Signaled Interrupts)
    Msi(MsiConfig),
    /// MSI-X (Extended Message Signaled Interrupts)
    MsiX(MsixConfig),
}

impl InterruptMode {
    /// Check if this is legacy mode
    pub fn is_legacy(&self) -> bool {
        matches!(self, InterruptMode::Legacy)
    }
    
    /// Check if this is MSI mode
    pub fn is_msi(&self) -> bool {
        matches!(self, InterruptMode::Msi(_))
    }
    
    /// Check if this is MSI-X mode
    pub fn is_msix(&self) -> bool {
        matches!(self, InterruptMode::MsiX(_))
    }
}

/// Detect best interrupt mode for a device
pub fn detect_interrupt_mode(addr: PcieAddress) -> Result<InterruptMode, PcieError> {
    use super::config::find_capability;
    
    // Prefer MSI-X over MSI over legacy
    if let Some(cap_offset) = find_capability(addr, Capability::MSIX)? {
        let config = MsixConfig::from_device(addr, cap_offset)?;
        return Ok(InterruptMode::MsiX(config));
    }
    
    if let Some(cap_offset) = find_capability(addr, Capability::MSI)? {
        let config = MsiConfig::from_device(addr, cap_offset)?;
        return Ok(InterruptMode::Msi(config));
    }
    
    Ok(InterruptMode::Legacy)
}

/// x86_64 APIC MSI address format
pub mod ApicMsi {
    /// Base address for MSI messages (0xFEE00000)
    pub const BASE_ADDRESS: u64 = 0xFEE00000;
    
    /// Build MSI address for target CPU
    pub fn build_address(dest_id: u8, redirect_hint: bool, dest_mode_logical: bool) -> u64 {
        let mut addr = BASE_ADDRESS;
        addr |= (dest_id as u64) << 12;
        if redirect_hint {
            addr |= 1 << 3;
        }
        if dest_mode_logical {
            addr |= 1 << 2;
        }
        addr
    }
    
    /// Build MSI data for interrupt vector
    pub fn build_data(vector: u8, level_trigger: bool, assert: bool) -> u16 {
        let mut data = vector as u16;
        if level_trigger {
            data |= 1 << 15;
        }
        if assert {
            data |= 1 << 14;
        }
        data
    }
}
