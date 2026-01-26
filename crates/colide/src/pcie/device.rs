//! PCIe Device Enumeration and Management
//!
//! Provides device detection, BAR mapping, and driver binding.

use super::config::*;
use super::PcieError;
use heapless::Vec as HVec;

/// Maximum devices to track
pub const MAX_PCIE_DEVICES: usize = 32;

/// PCIe device information
#[derive(Debug, Clone)]
pub struct PcieDevice {
    pub addr: PcieAddress,
    pub vendor_id: u16,
    pub device_id: u16,
    pub class_code: u8,
    pub subclass: u8,
    pub prog_if: u8,
    pub revision: u8,
    pub header_type: u8,
    pub bars: [Bar; 6],
    pub irq_line: u8,
    pub irq_pin: u8,
}

/// Base Address Register (BAR)
#[derive(Debug, Clone, Copy, Default)]
pub struct Bar {
    pub base: u64,
    pub size: u64,
    pub is_io: bool,
    pub is_64bit: bool,
    pub prefetchable: bool,
}

impl PcieDevice {
    /// Create new device from configuration space
    pub fn from_config(addr: PcieAddress) -> Result<Option<Self>, PcieError> {
        let vendor_id = read_config16(addr, Regs::VENDOR_ID)?;
        
        // Check if device exists
        if vendor_id == 0xFFFF {
            return Ok(None);
        }
        
        let device_id = read_config16(addr, Regs::DEVICE_ID)?;
        let class_code = read_config8(addr, Regs::CLASS_CODE)?;
        let subclass = read_config8(addr, Regs::SUBCLASS)?;
        let prog_if = read_config8(addr, Regs::PROG_IF)?;
        let revision = read_config8(addr, Regs::REVISION_ID)?;
        let header_type = read_config8(addr, Regs::HEADER_TYPE)?;
        let irq_line = read_config8(addr, Regs::INTERRUPT_LINE)?;
        let irq_pin = read_config8(addr, Regs::INTERRUPT_PIN)?;
        
        let mut device = Self {
            addr,
            vendor_id,
            device_id,
            class_code,
            subclass,
            prog_if,
            revision,
            header_type,
            bars: [Bar::default(); 6],
            irq_line,
            irq_pin,
        };
        
        // Probe BARs
        device.probe_bars()?;
        
        Ok(Some(device))
    }
    
    /// Probe BAR sizes
    fn probe_bars(&mut self) -> Result<(), PcieError> {
        let mut bar_idx = 0;
        
        while bar_idx < 6 {
            let bar_reg = Regs::BAR0 + (bar_idx as u8 * 4);
            let original = read_config32(self.addr, bar_reg)?;
            
            if original == 0 {
                bar_idx += 1;
                continue;
            }
            
            let is_io = (original & 0x01) != 0;
            let is_64bit = !is_io && ((original & 0x06) == 0x04);
            let prefetchable = !is_io && ((original & 0x08) != 0);
            
            // Write all 1s to determine size
            write_config32(self.addr, bar_reg, 0xFFFFFFFF)?;
            let sized = read_config32(self.addr, bar_reg)?;
            write_config32(self.addr, bar_reg, original)?;
            
            let mask = if is_io { 0xFFFFFFFC } else { 0xFFFFFFF0 };
            let size = (!(sized & mask)).wrapping_add(1) as u64;
            
            let base = if is_io {
                (original & 0xFFFFFFFC) as u64
            } else if is_64bit {
                let high = read_config32(self.addr, bar_reg + 4)? as u64;
                ((high << 32) | (original & 0xFFFFFFF0) as u64)
            } else {
                (original & 0xFFFFFFF0) as u64
            };
            
            self.bars[bar_idx] = Bar {
                base,
                size,
                is_io,
                is_64bit,
                prefetchable,
            };
            
            bar_idx += if is_64bit { 2 } else { 1 };
        }
        
        Ok(())
    }
    
    /// Enable bus mastering
    pub fn enable_bus_master(&self) -> Result<(), PcieError> {
        let cmd = read_config16(self.addr, Regs::COMMAND)?;
        write_config16(self.addr, Regs::COMMAND, cmd | Command::BUS_MASTER)
    }
    
    /// Enable memory space access
    pub fn enable_memory_space(&self) -> Result<(), PcieError> {
        let cmd = read_config16(self.addr, Regs::COMMAND)?;
        write_config16(self.addr, Regs::COMMAND, cmd | Command::MEMORY_SPACE)
    }
    
    /// Enable I/O space access
    pub fn enable_io_space(&self) -> Result<(), PcieError> {
        let cmd = read_config16(self.addr, Regs::COMMAND)?;
        write_config16(self.addr, Regs::COMMAND, cmd | Command::IO_SPACE)
    }
    
    /// Check if device is a WiFi controller
    pub fn is_wifi(&self) -> bool {
        self.class_code == Class::NETWORK_CONTROLLER && 
        self.subclass == Class::WIRELESS_CONTROLLER
    }
    
    /// Check if device is Intel WiFi
    pub fn is_intel_wifi(&self) -> bool {
        self.vendor_id == 0x8086 && self.is_wifi()
    }
    
    /// Get primary memory BAR (usually BAR0)
    pub fn memory_bar(&self) -> Option<&Bar> {
        self.bars.iter().find(|b| !b.is_io && b.size > 0)
    }
}

/// Enumerate all PCIe devices
pub fn enumerate_devices() -> Result<HVec<PcieDevice, MAX_PCIE_DEVICES>, PcieError> {
    let mut devices = HVec::new();
    
    // Scan all buses, devices, functions
    for bus in 0..=255_u8 {
        for device in 0..32_u8 {
            let addr = PcieAddress::new(bus, device, 0);
            
            if let Some(dev) = PcieDevice::from_config(addr)? {
                let is_multifunction = (dev.header_type & 0x80) != 0;
                let _ = devices.push(dev);
                
                // Check additional functions
                if is_multifunction {
                    for func in 1..8_u8 {
                        let func_addr = PcieAddress::new(bus, device, func);
                        if let Some(func_dev) = PcieDevice::from_config(func_addr)? {
                            let _ = devices.push(func_dev);
                        }
                    }
                }
            }
        }
        
        // Early exit if we've filled the list
        if devices.is_full() {
            break;
        }
    }
    
    Ok(devices)
}

/// Find devices by vendor/device ID
pub fn find_device(vendor_id: u16, device_id: u16) -> Result<Option<PcieDevice>, PcieError> {
    let devices = enumerate_devices()?;
    
    for dev in devices {
        if dev.vendor_id == vendor_id && dev.device_id == device_id {
            return Ok(Some(dev));
        }
    }
    
    Ok(None)
}

/// Find Intel WiFi device
pub fn find_intel_wifi() -> Result<Option<PcieDevice>, PcieError> {
    let devices = enumerate_devices()?;
    
    for dev in devices {
        if dev.is_intel_wifi() {
            return Ok(Some(dev));
        }
    }
    
    Ok(None)
}

/// Known Intel WiFi device IDs (AX200, AX210, AX211)
pub mod IntelWifiIds {
    // AX200 (WiFi 6)
    pub const AX200_1: u16 = 0x2723;
    pub const AX200_2: u16 = 0x02F0;
    
    // AX210 (WiFi 6E)
    pub const AX210_1: u16 = 0x2725;
    pub const AX210_2: u16 = 0x2726;
    
    // AX211 (WiFi 6E, CNVio2)
    pub const AX211_1: u16 = 0x51F0;
    pub const AX211_2: u16 = 0x51F1;
    pub const AX211_3: u16 = 0x54F0;
    
    // AX411 (vPro)
    pub const AX411: u16 = 0x54F1;
    
    /// Check if device ID is Intel WiFi
    pub fn is_intel_wifi(device_id: u16) -> bool {
        matches!(device_id, 
            AX200_1 | AX200_2 |
            AX210_1 | AX210_2 |
            AX211_1 | AX211_2 | AX211_3 |
            AX411
        )
    }
}
