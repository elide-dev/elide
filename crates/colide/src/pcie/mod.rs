//! PCIe (PCI Express) Driver Foundation for Colide OS
//!
//! Provides PCIe device enumeration and memory-mapped I/O access
//! for high-speed devices like Intel WiFi (iwlwifi).
//!
//! Based on PCIe 3.0 specification with x86_64 focus.

pub mod config;
pub mod device;
pub mod mmio;
pub mod msi;

pub use config::*;
pub use device::*;
pub use mmio::*;

/// PCIe configuration space access methods
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PcieAccessMethod {
    /// Legacy I/O port access (0xCF8/0xCFC)
    LegacyIo,
    /// Memory-mapped configuration (ECAM)
    Ecam,
}

/// Global PCIe state
static mut PCIE_METHOD: PcieAccessMethod = PcieAccessMethod::LegacyIo;
static mut ECAM_BASE: u64 = 0;

/// Initialize PCIe subsystem
pub fn init() -> Result<(), PcieError> {
    // Try to detect ECAM (Enhanced Configuration Access Mechanism)
    if let Some(base) = detect_ecam() {
        unsafe {
            ECAM_BASE = base;
            PCIE_METHOD = PcieAccessMethod::Ecam;
        }
    }
    
    Ok(())
}

/// Detect ECAM base address from ACPI MCFG table
fn detect_ecam() -> Option<u64> {
    // Would parse ACPI tables to find MCFG
    // For now, return None to use legacy I/O
    None
}

/// PCIe error types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum PcieError {
    DeviceNotFound,
    InvalidAddress,
    AccessDenied,
    Timeout,
    ConfigError,
}
