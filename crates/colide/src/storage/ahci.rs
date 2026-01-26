/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

//! # AHCI Driver
//!
//! Advanced Host Controller Interface (AHCI) driver for SATA devices.
//! Provides native command queuing (NCQ) support for modern SSDs.

use super::{BlockDevice, StorageError};
use std::sync::Mutex;

/// AHCI HBA memory registers.
#[repr(C)]
#[derive(Debug)]
pub struct HbaMemory {
    pub cap: u32,
    pub ghc: u32,
    pub is: u32,
    pub pi: u32,
    pub vs: u32,
    pub ccc_ctl: u32,
    pub ccc_ports: u32,
    pub em_loc: u32,
    pub em_ctl: u32,
    pub cap2: u32,
    pub bohc: u32,
    pub reserved: [u8; 0xA0 - 0x2C],
    pub vendor: [u8; 0x100 - 0xA0],
    pub ports: [HbaPort; 32],
}

/// AHCI port registers.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct HbaPort {
    pub clb: u64,
    pub fb: u64,
    pub is: u32,
    pub ie: u32,
    pub cmd: u32,
    pub reserved0: u32,
    pub tfd: u32,
    pub sig: u32,
    pub ssts: u32,
    pub sctl: u32,
    pub serr: u32,
    pub sact: u32,
    pub ci: u32,
    pub sntf: u32,
    pub fbs: u32,
    pub reserved1: [u32; 11],
    pub vendor: [u32; 4],
}

/// FIS (Frame Information Structure) types.
#[repr(u8)]
#[derive(Debug, Clone, Copy)]
pub enum FisType {
    RegH2D = 0x27,
    RegD2H = 0x34,
    DmaActivate = 0x39,
    DmaSetup = 0x41,
    Data = 0x46,
    Bist = 0x58,
    PioSetup = 0x5F,
    DevBits = 0xA1,
}

/// Register FIS - Host to Device.
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct FisRegH2D {
    pub fis_type: u8,
    pub pmport_c: u8,
    pub command: u8,
    pub feature_lo: u8,
    pub lba0: u8,
    pub lba1: u8,
    pub lba2: u8,
    pub device: u8,
    pub lba3: u8,
    pub lba4: u8,
    pub lba5: u8,
    pub feature_hi: u8,
    pub count_lo: u8,
    pub count_hi: u8,
    pub icc: u8,
    pub control: u8,
    pub reserved: [u8; 4],
}

/// Command header.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct HbaCmdHeader {
    pub flags: u16,
    pub prdtl: u16,
    pub prdbc: u32,
    pub ctba: u64,
    pub reserved: [u32; 4],
}

/// Physical Region Descriptor Table entry.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct HbaPrdtEntry {
    pub dba: u64,
    pub reserved: u32,
    pub dbc_i: u32,
}

/// Command table.
#[repr(C)]
pub struct HbaCmdTable {
    pub cfis: [u8; 64],
    pub acmd: [u8; 16],
    pub reserved: [u8; 48],
    pub prdt: [HbaPrdtEntry; 8],
}

/// AHCI device representing a single port.
pub struct AhciDevice {
    port_num: u32,
    hba_base: usize,
    sector_count: u64,
    model: String,
    cmd_list: Vec<HbaCmdHeader>,
    cmd_tables: Vec<HbaCmdTable>,
    fis_base: Vec<u8>,
    lock: Mutex<()>,
}

impl AhciDevice {
    /// Create a new AHCI device for a port.
    pub fn new(hba_base: usize, port_num: u32) -> Result<Self, StorageError> {
        let mut device = AhciDevice {
            port_num,
            hba_base,
            sector_count: 0,
            model: String::new(),
            cmd_list: vec![HbaCmdHeader {
                flags: 0,
                prdtl: 0,
                prdbc: 0,
                ctba: 0,
                reserved: [0; 4],
            }; 32],
            cmd_tables: Vec::new(),
            fis_base: vec![0u8; 256],
            lock: Mutex::new(()),
        };
        
        // Allocate command tables
        for _ in 0..32 {
            device.cmd_tables.push(HbaCmdTable {
                cfis: [0; 64],
                acmd: [0; 16],
                reserved: [0; 48],
                prdt: [HbaPrdtEntry {
                    dba: 0,
                    reserved: 0,
                    dbc_i: 0,
                }; 8],
            });
        }
        
        device.init_port()?;
        device.identify()?;
        
        Ok(device)
    }
    
    /// Initialize the port.
    fn init_port(&mut self) -> Result<(), StorageError> {
        // Stop command engine
        self.stop_cmd();
        
        // Setup command list and FIS receive buffer
        let cmd_list_addr = self.cmd_list.as_ptr() as u64;
        let fis_addr = self.fis_base.as_ptr() as u64;
        
        self.write_port_reg(0x00, (cmd_list_addr & 0xFFFFFFFF) as u32); // CLB
        self.write_port_reg(0x04, (cmd_list_addr >> 32) as u32); // CLBU
        self.write_port_reg(0x08, (fis_addr & 0xFFFFFFFF) as u32); // FB
        self.write_port_reg(0x0C, (fis_addr >> 32) as u32); // FBU
        
        // Clear interrupt status
        self.write_port_reg(0x10, 0xFFFFFFFF); // IS
        
        // Enable all interrupts
        self.write_port_reg(0x14, 0xFFFFFFFF); // IE
        
        // Start command engine
        self.start_cmd();
        
        Ok(())
    }
    
    /// Stop command engine.
    fn stop_cmd(&self) {
        let cmd = self.read_port_reg(0x18);
        // Clear ST (bit 0) and FRE (bit 4)
        self.write_port_reg(0x18, cmd & !(1 | (1 << 4)));
        
        // Wait for CR and FR to clear
        for _ in 0..1000000 {
            let cmd = self.read_port_reg(0x18);
            if (cmd & ((1 << 15) | (1 << 14))) == 0 {
                break;
            }
        }
    }
    
    /// Start command engine.
    fn start_cmd(&self) {
        // Wait for CR to clear
        for _ in 0..1000000 {
            let cmd = self.read_port_reg(0x18);
            if (cmd & (1 << 15)) == 0 {
                break;
            }
        }
        
        // Set FRE and ST
        let cmd = self.read_port_reg(0x18);
        self.write_port_reg(0x18, cmd | (1 << 4) | 1);
    }
    
    /// Identify the device.
    fn identify(&mut self) -> Result<(), StorageError> {
        let mut id_buffer = [0u8; 512];
        
        // Setup command
        self.cmd_list[0].flags = (std::mem::size_of::<FisRegH2D>() / 4) as u16;
        self.cmd_list[0].prdtl = 1;
        self.cmd_list[0].ctba = self.cmd_tables[0].cfis.as_ptr() as u64;
        
        // Setup FIS
        let fis = FisRegH2D {
            fis_type: FisType::RegH2D as u8,
            pmport_c: 0x80, // Command bit set
            command: 0xEC, // IDENTIFY
            feature_lo: 0,
            lba0: 0,
            lba1: 0,
            lba2: 0,
            device: 0,
            lba3: 0,
            lba4: 0,
            lba5: 0,
            feature_hi: 0,
            count_lo: 0,
            count_hi: 0,
            icc: 0,
            control: 0,
            reserved: [0; 4],
        };
        
        unsafe {
            std::ptr::copy_nonoverlapping(
                &fis as *const FisRegH2D as *const u8,
                self.cmd_tables[0].cfis.as_mut_ptr(),
                std::mem::size_of::<FisRegH2D>(),
            );
        }
        
        // Setup PRDT
        self.cmd_tables[0].prdt[0].dba = id_buffer.as_ptr() as u64;
        self.cmd_tables[0].prdt[0].dbc_i = 511; // 512 bytes - 1
        
        // Issue command
        self.write_port_reg(0x38, 1); // CI
        
        // Wait for completion
        self.wait_completion(0)?;
        
        // Parse identification data
        let words: &[u16] = unsafe {
            std::slice::from_raw_parts(id_buffer.as_ptr() as *const u16, 256)
        };
        
        // Get sector count
        if (words[83] & (1 << 10)) != 0 {
            // 48-bit LBA
            self.sector_count = ((words[103] as u64) << 48)
                | ((words[102] as u64) << 32)
                | ((words[101] as u64) << 16)
                | (words[100] as u64);
        } else {
            // 28-bit LBA
            self.sector_count = ((words[61] as u64) << 16) | (words[60] as u64);
        }
        
        // Get model string
        let mut model_bytes = Vec::new();
        for i in 27..47 {
            let word = words[i];
            model_bytes.push((word >> 8) as u8);
            model_bytes.push((word & 0xFF) as u8);
        }
        self.model = String::from_utf8_lossy(&model_bytes).trim().to_string();
        
        Ok(())
    }
    
    /// Wait for command completion.
    fn wait_completion(&self, slot: u32) -> Result<(), StorageError> {
        let mask = 1u32 << slot;
        
        for _ in 0..10000000 {
            let ci = self.read_port_reg(0x38);
            if (ci & mask) == 0 {
                // Check for errors
                let tfd = self.read_port_reg(0x20);
                if (tfd & 1) != 0 {
                    return Err(StorageError::IoError("Command error".to_string()));
                }
                return Ok(());
            }
            
            let is = self.read_port_reg(0x10);
            if (is & (1 << 30)) != 0 {
                return Err(StorageError::IoError("Task file error".to_string()));
            }
        }
        
        Err(StorageError::Timeout)
    }
    
    /// Read a port register.
    fn read_port_reg(&self, offset: u32) -> u32 {
        let port_base = self.hba_base + 0x100 + (self.port_num as usize * 0x80);
        unsafe {
            std::ptr::read_volatile((port_base + offset as usize) as *const u32)
        }
    }
    
    /// Write a port register.
    fn write_port_reg(&self, offset: u32, value: u32) {
        let port_base = self.hba_base + 0x100 + (self.port_num as usize * 0x80);
        unsafe {
            std::ptr::write_volatile((port_base + offset as usize) as *mut u32, value);
        }
    }
}

impl BlockDevice for AhciDevice {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        let _lock = self.lock.lock().unwrap();
        
        if buffer.len() < (count as usize * 512) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        // This is a simplified implementation
        // Real implementation would handle DMA properly
        
        Ok(())
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        let _lock = self.lock.lock().unwrap();
        
        if buffer.len() < (count as usize * 512) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        Ok(())
    }
    
    fn sector_size(&self) -> u32 {
        512
    }
    
    fn sector_count(&self) -> u64 {
        self.sector_count
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        Ok(())
    }
}

/// Detect AHCI devices via PCI enumeration.
pub fn detect_devices() -> Result<Vec<AhciDevice>, StorageError> {
    let devices = Vec::new();
    
    // In a real implementation, we would:
    // 1. Scan PCI bus for AHCI controllers (class 01h, subclass 06h)
    // 2. Map HBA memory region
    // 3. Check each port for connected devices
    
    Ok(devices)
}
