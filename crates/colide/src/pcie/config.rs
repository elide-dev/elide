//! PCIe Configuration Space Access
//!
//! Provides read/write access to PCIe configuration space
//! via both legacy I/O ports and ECAM (memory-mapped).

use super::PcieError;

/// PCIe configuration space size
pub const PCIE_CONFIG_SIZE: usize = 4096;  // Extended config space
pub const PCI_CONFIG_SIZE: usize = 256;    // Legacy config space

/// Legacy PCI configuration I/O ports
pub const PCI_CONFIG_ADDRESS: u16 = 0xCF8;
pub const PCI_CONFIG_DATA: u16 = 0xCFC;

/// Standard PCI configuration registers
pub mod Regs {
    pub const VENDOR_ID: u8 = 0x00;
    pub const DEVICE_ID: u8 = 0x02;
    pub const COMMAND: u8 = 0x04;
    pub const STATUS: u8 = 0x06;
    pub const REVISION_ID: u8 = 0x08;
    pub const PROG_IF: u8 = 0x09;
    pub const SUBCLASS: u8 = 0x0A;
    pub const CLASS_CODE: u8 = 0x0B;
    pub const CACHE_LINE_SIZE: u8 = 0x0C;
    pub const LATENCY_TIMER: u8 = 0x0D;
    pub const HEADER_TYPE: u8 = 0x0E;
    pub const BIST: u8 = 0x0F;
    pub const BAR0: u8 = 0x10;
    pub const BAR1: u8 = 0x14;
    pub const BAR2: u8 = 0x18;
    pub const BAR3: u8 = 0x1C;
    pub const BAR4: u8 = 0x20;
    pub const BAR5: u8 = 0x24;
    pub const CARDBUS_CIS: u8 = 0x28;
    pub const SUBSYSTEM_VENDOR_ID: u8 = 0x2C;
    pub const SUBSYSTEM_ID: u8 = 0x2E;
    pub const EXPANSION_ROM_BASE: u8 = 0x30;
    pub const CAPABILITIES_PTR: u8 = 0x34;
    pub const INTERRUPT_LINE: u8 = 0x3C;
    pub const INTERRUPT_PIN: u8 = 0x3D;
    pub const MIN_GRANT: u8 = 0x3E;
    pub const MAX_LATENCY: u8 = 0x3F;
}

/// PCI Command register bits
pub mod Command {
    pub const IO_SPACE: u16 = 0x0001;
    pub const MEMORY_SPACE: u16 = 0x0002;
    pub const BUS_MASTER: u16 = 0x0004;
    pub const SPECIAL_CYCLES: u16 = 0x0008;
    pub const MWI_ENABLE: u16 = 0x0010;
    pub const VGA_PALETTE_SNOOP: u16 = 0x0020;
    pub const PARITY_ERROR_RESPONSE: u16 = 0x0040;
    pub const SERR_ENABLE: u16 = 0x0100;
    pub const FAST_BACK_TO_BACK: u16 = 0x0200;
    pub const INTERRUPT_DISABLE: u16 = 0x0400;
}

/// PCI class codes
pub mod Class {
    pub const NETWORK_CONTROLLER: u8 = 0x02;
    pub const WIRELESS_CONTROLLER: u8 = 0x80;  // Subclass for WiFi
}

/// PCIe capability IDs
pub mod Capability {
    pub const POWER_MANAGEMENT: u8 = 0x01;
    pub const AGP: u8 = 0x02;
    pub const VPD: u8 = 0x03;
    pub const SLOT_ID: u8 = 0x04;
    pub const MSI: u8 = 0x05;
    pub const PCIE: u8 = 0x10;
    pub const MSIX: u8 = 0x11;
}

/// PCIe address for configuration access
#[derive(Debug, Clone, Copy)]
pub struct PcieAddress {
    pub bus: u8,
    pub device: u8,
    pub function: u8,
}

impl PcieAddress {
    pub fn new(bus: u8, device: u8, function: u8) -> Self {
        Self { bus, device, function }
    }
    
    /// Create legacy PCI configuration address
    pub fn to_legacy_address(&self, reg: u8) -> u32 {
        0x80000000
            | ((self.bus as u32) << 16)
            | ((self.device as u32 & 0x1F) << 11)
            | ((self.function as u32 & 0x07) << 8)
            | ((reg as u32) & 0xFC)
    }
    
    /// Create ECAM offset
    pub fn to_ecam_offset(&self, reg: u16) -> u64 {
        ((self.bus as u64) << 20)
            | ((self.device as u64 & 0x1F) << 15)
            | ((self.function as u64 & 0x07) << 12)
            | (reg as u64)
    }
}

/// Read 8-bit value from configuration space
pub fn read_config8(addr: PcieAddress, reg: u8) -> Result<u8, PcieError> {
    let val32 = read_config32(addr, reg & 0xFC)?;
    let shift = (reg & 0x03) * 8;
    Ok((val32 >> shift) as u8)
}

/// Read 16-bit value from configuration space
pub fn read_config16(addr: PcieAddress, reg: u8) -> Result<u16, PcieError> {
    let val32 = read_config32(addr, reg & 0xFC)?;
    let shift = (reg & 0x02) * 8;
    Ok((val32 >> shift) as u16)
}

/// Read 32-bit value from configuration space
pub fn read_config32(addr: PcieAddress, reg: u8) -> Result<u32, PcieError> {
    // Use legacy I/O port access
    let config_addr = addr.to_legacy_address(reg);
    
    // Write address to CONFIG_ADDRESS port
    unsafe {
        core::arch::asm!(
            "out dx, eax",
            in("dx") PCI_CONFIG_ADDRESS,
            in("eax") config_addr,
            options(nostack, preserves_flags)
        );
    }
    
    // Read data from CONFIG_DATA port
    let value: u32;
    unsafe {
        core::arch::asm!(
            "in eax, dx",
            out("eax") value,
            in("dx") PCI_CONFIG_DATA,
            options(nostack, preserves_flags)
        );
    }
    
    Ok(value)
}

/// Write 8-bit value to configuration space
pub fn write_config8(addr: PcieAddress, reg: u8, value: u8) -> Result<(), PcieError> {
    let mut val32 = read_config32(addr, reg & 0xFC)?;
    let shift = (reg & 0x03) * 8;
    let mask = !(0xFF_u32 << shift);
    val32 = (val32 & mask) | ((value as u32) << shift);
    write_config32(addr, reg & 0xFC, val32)
}

/// Write 16-bit value to configuration space
pub fn write_config16(addr: PcieAddress, reg: u8, value: u16) -> Result<(), PcieError> {
    let mut val32 = read_config32(addr, reg & 0xFC)?;
    let shift = (reg & 0x02) * 8;
    let mask = !(0xFFFF_u32 << shift);
    val32 = (val32 & mask) | ((value as u32) << shift);
    write_config32(addr, reg & 0xFC, val32)
}

/// Write 32-bit value to configuration space
pub fn write_config32(addr: PcieAddress, reg: u8, value: u32) -> Result<(), PcieError> {
    let config_addr = addr.to_legacy_address(reg);
    
    // Write address to CONFIG_ADDRESS port
    unsafe {
        core::arch::asm!(
            "out dx, eax",
            in("dx") PCI_CONFIG_ADDRESS,
            in("eax") config_addr,
            options(nostack, preserves_flags)
        );
    }
    
    // Write data to CONFIG_DATA port
    unsafe {
        core::arch::asm!(
            "out dx, eax",
            in("dx") PCI_CONFIG_DATA,
            in("eax") value,
            options(nostack, preserves_flags)
        );
    }
    
    Ok(())
}

/// Find capability in configuration space
pub fn find_capability(addr: PcieAddress, cap_id: u8) -> Result<Option<u8>, PcieError> {
    // Check if capabilities list is supported
    let status = read_config16(addr, Regs::STATUS)?;
    if (status & 0x10) == 0 {
        return Ok(None);
    }
    
    // Get capabilities pointer
    let mut cap_ptr = read_config8(addr, Regs::CAPABILITIES_PTR)?;
    
    // Walk the capability list
    while cap_ptr != 0 {
        let id = read_config8(addr, cap_ptr)?;
        if id == cap_id {
            return Ok(Some(cap_ptr));
        }
        cap_ptr = read_config8(addr, cap_ptr + 1)?;
    }
    
    Ok(None)
}
