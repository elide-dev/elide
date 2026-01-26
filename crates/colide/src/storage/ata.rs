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

//! # ATA/IDE Driver
//!
//! Legacy ATA/IDE disk controller driver for Colide OS.
//! Supports PIO mode for maximum compatibility.

use super::{BlockDevice, StorageError};

/// ATA I/O port bases.
const ATA_PRIMARY_IO: u16 = 0x1F0;
const ATA_PRIMARY_CTRL: u16 = 0x3F6;
const ATA_SECONDARY_IO: u16 = 0x170;
const ATA_SECONDARY_CTRL: u16 = 0x376;

/// ATA register offsets.
const ATA_REG_DATA: u16 = 0;
const ATA_REG_ERROR: u16 = 1;
const ATA_REG_FEATURES: u16 = 1;
const ATA_REG_SECCOUNT: u16 = 2;
const ATA_REG_LBA_LO: u16 = 3;
const ATA_REG_LBA_MID: u16 = 4;
const ATA_REG_LBA_HI: u16 = 5;
const ATA_REG_DRIVE: u16 = 6;
const ATA_REG_STATUS: u16 = 7;
const ATA_REG_COMMAND: u16 = 7;

/// ATA commands.
const ATA_CMD_READ_PIO: u8 = 0x20;
const ATA_CMD_READ_PIO_EXT: u8 = 0x24;
const ATA_CMD_WRITE_PIO: u8 = 0x30;
const ATA_CMD_WRITE_PIO_EXT: u8 = 0x34;
const ATA_CMD_CACHE_FLUSH: u8 = 0xE7;
const ATA_CMD_CACHE_FLUSH_EXT: u8 = 0xEA;
const ATA_CMD_IDENTIFY: u8 = 0xEC;

/// ATA status bits.
const ATA_SR_BSY: u8 = 0x80;
const ATA_SR_DRDY: u8 = 0x40;
const ATA_SR_DF: u8 = 0x20;
const ATA_SR_DSC: u8 = 0x10;
const ATA_SR_DRQ: u8 = 0x08;
const ATA_SR_CORR: u8 = 0x04;
const ATA_SR_IDX: u8 = 0x02;
const ATA_SR_ERR: u8 = 0x01;

/// ATA device type.
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AtaDeviceType {
    None,
    Ata,
    Atapi,
    Sata,
    Satapi,
}

/// ATA device information.
#[derive(Debug, Clone)]
pub struct AtaDevice {
    pub channel: u8,
    pub drive: u8,
    pub device_type: AtaDeviceType,
    pub signature: u16,
    pub capabilities: u16,
    pub command_sets: u32,
    pub size: u64,
    pub model: String,
    io_base: u16,
    ctrl_base: u16,
}

impl AtaDevice {
    /// Create a new ATA device.
    pub fn new(channel: u8, drive: u8) -> Self {
        let (io_base, ctrl_base) = if channel == 0 {
            (ATA_PRIMARY_IO, ATA_PRIMARY_CTRL)
        } else {
            (ATA_SECONDARY_IO, ATA_SECONDARY_CTRL)
        };
        
        AtaDevice {
            channel,
            drive,
            device_type: AtaDeviceType::None,
            signature: 0,
            capabilities: 0,
            command_sets: 0,
            size: 0,
            model: String::new(),
            io_base,
            ctrl_base,
        }
    }
    
    /// Identify the device.
    pub fn identify(&mut self) -> Result<(), StorageError> {
        // Select drive
        self.select_drive();
        
        // Send IDENTIFY command
        self.write_reg(ATA_REG_COMMAND, ATA_CMD_IDENTIFY);
        
        // Wait for response
        if self.read_reg(ATA_REG_STATUS) == 0 {
            return Err(StorageError::NotFound);
        }
        
        // Poll until BSY clears
        self.poll_bsy()?;
        
        // Check for ATAPI
        let lba_mid = self.read_reg(ATA_REG_LBA_MID);
        let lba_hi = self.read_reg(ATA_REG_LBA_HI);
        
        if lba_mid == 0x14 && lba_hi == 0xEB {
            self.device_type = AtaDeviceType::Atapi;
        } else if lba_mid == 0x69 && lba_hi == 0x96 {
            self.device_type = AtaDeviceType::Satapi;
        } else if lba_mid == 0x3C && lba_hi == 0xC3 {
            self.device_type = AtaDeviceType::Sata;
        } else if lba_mid == 0 && lba_hi == 0 {
            self.device_type = AtaDeviceType::Ata;
        } else {
            return Err(StorageError::NotFound);
        }
        
        // Poll for DRQ or ERR
        self.poll_drq()?;
        
        // Read identification data
        let mut id_data = [0u16; 256];
        for word in &mut id_data {
            *word = self.read_data();
        }
        
        // Parse identification
        self.signature = id_data[0];
        self.capabilities = id_data[49];
        self.command_sets = ((id_data[83] as u32) << 16) | (id_data[82] as u32);
        
        // Get size
        if (self.command_sets & (1 << 26)) != 0 {
            // 48-bit LBA
            self.size = ((id_data[103] as u64) << 48)
                | ((id_data[102] as u64) << 32)
                | ((id_data[101] as u64) << 16)
                | (id_data[100] as u64);
        } else {
            // 28-bit LBA
            self.size = ((id_data[61] as u64) << 16) | (id_data[60] as u64);
        }
        
        // Get model string
        let mut model_bytes = Vec::new();
        for i in 27..47 {
            let word = id_data[i];
            model_bytes.push((word >> 8) as u8);
            model_bytes.push((word & 0xFF) as u8);
        }
        self.model = String::from_utf8_lossy(&model_bytes).trim().to_string();
        
        Ok(())
    }
    
    /// Select this drive.
    fn select_drive(&self) {
        let drive_sel = 0xA0 | (self.drive << 4);
        self.write_reg(ATA_REG_DRIVE, drive_sel);
        self.io_delay();
    }
    
    /// Wait for BSY to clear.
    fn poll_bsy(&self) -> Result<(), StorageError> {
        for _ in 0..100000 {
            let status = self.read_reg(ATA_REG_STATUS);
            if (status & ATA_SR_BSY) == 0 {
                return Ok(());
            }
        }
        Err(StorageError::Timeout)
    }
    
    /// Wait for DRQ to set.
    fn poll_drq(&self) -> Result<(), StorageError> {
        for _ in 0..100000 {
            let status = self.read_reg(ATA_REG_STATUS);
            if (status & ATA_SR_ERR) != 0 || (status & ATA_SR_DF) != 0 {
                return Err(StorageError::IoError("Device error".to_string()));
            }
            if (status & ATA_SR_DRQ) != 0 {
                return Ok(());
            }
        }
        Err(StorageError::Timeout)
    }
    
    /// Read a register.
    fn read_reg(&self, reg: u16) -> u8 {
        #[cfg(target_arch = "x86_64")]
        unsafe {
            let value: u8;
            std::arch::asm!(
                "in al, dx",
                out("al") value,
                in("dx") self.io_base + reg,
                options(nomem, nostack, preserves_flags)
            );
            value
        }
        #[cfg(not(target_arch = "x86_64"))]
        { 0 }
    }
    
    /// Write a register.
    fn write_reg(&self, reg: u16, value: u8) {
        #[cfg(target_arch = "x86_64")]
        unsafe {
            std::arch::asm!(
                "out dx, al",
                in("dx") self.io_base + reg,
                in("al") value,
                options(nomem, nostack, preserves_flags)
            );
        }
    }
    
    /// Read 16-bit data.
    fn read_data(&self) -> u16 {
        #[cfg(target_arch = "x86_64")]
        unsafe {
            let value: u16;
            std::arch::asm!(
                "in ax, dx",
                out("ax") value,
                in("dx") self.io_base + ATA_REG_DATA,
                options(nomem, nostack, preserves_flags)
            );
            value
        }
        #[cfg(not(target_arch = "x86_64"))]
        { 0 }
    }
    
    /// Write 16-bit data.
    fn write_data(&self, value: u16) {
        #[cfg(target_arch = "x86_64")]
        unsafe {
            std::arch::asm!(
                "out dx, ax",
                in("dx") self.io_base + ATA_REG_DATA,
                in("ax") value,
                options(nomem, nostack, preserves_flags)
            );
        }
    }
    
    /// I/O delay.
    fn io_delay(&self) {
        for _ in 0..4 {
            let _ = self.read_reg(ATA_REG_STATUS);
        }
    }
    
    /// Check if LBA48 is supported.
    fn supports_lba48(&self) -> bool {
        (self.command_sets & (1 << 26)) != 0
    }
}

impl BlockDevice for AtaDevice {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        if buffer.len() < (count as usize * 512) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        let use_lba48 = self.supports_lba48() && (lba > 0x0FFFFFFF || count > 256);
        
        self.select_drive();
        self.poll_bsy()?;
        
        if use_lba48 {
            // LBA48 mode
            self.write_reg(ATA_REG_SECCOUNT, ((count >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, ((lba >> 24) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 32) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 40) & 0xFF) as u8);
            self.write_reg(ATA_REG_SECCOUNT, (count & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, (lba & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 16) & 0xFF) as u8);
            self.write_reg(ATA_REG_DRIVE, 0x40 | (self.drive << 4));
            self.write_reg(ATA_REG_COMMAND, ATA_CMD_READ_PIO_EXT);
        } else {
            // LBA28 mode
            self.write_reg(ATA_REG_SECCOUNT, (count & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, (lba & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 16) & 0xFF) as u8);
            self.write_reg(ATA_REG_DRIVE, 0xE0 | (self.drive << 4) | ((lba >> 24) & 0x0F) as u8);
            self.write_reg(ATA_REG_COMMAND, ATA_CMD_READ_PIO);
        }
        
        for sector in 0..count {
            self.poll_drq()?;
            let offset = sector as usize * 512;
            for i in (0..512).step_by(2) {
                let word = self.read_data();
                buffer[offset + i] = (word & 0xFF) as u8;
                buffer[offset + i + 1] = (word >> 8) as u8;
            }
        }
        
        Ok(())
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        if buffer.len() < (count as usize * 512) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        let use_lba48 = self.supports_lba48() && (lba > 0x0FFFFFFF || count > 256);
        
        self.select_drive();
        self.poll_bsy()?;
        
        if use_lba48 {
            self.write_reg(ATA_REG_SECCOUNT, ((count >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, ((lba >> 24) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 32) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 40) & 0xFF) as u8);
            self.write_reg(ATA_REG_SECCOUNT, (count & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, (lba & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 16) & 0xFF) as u8);
            self.write_reg(ATA_REG_DRIVE, 0x40 | (self.drive << 4));
            self.write_reg(ATA_REG_COMMAND, ATA_CMD_WRITE_PIO_EXT);
        } else {
            self.write_reg(ATA_REG_SECCOUNT, (count & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_LO, (lba & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_MID, ((lba >> 8) & 0xFF) as u8);
            self.write_reg(ATA_REG_LBA_HI, ((lba >> 16) & 0xFF) as u8);
            self.write_reg(ATA_REG_DRIVE, 0xE0 | (self.drive << 4) | ((lba >> 24) & 0x0F) as u8);
            self.write_reg(ATA_REG_COMMAND, ATA_CMD_WRITE_PIO);
        }
        
        for sector in 0..count {
            self.poll_drq()?;
            let offset = sector as usize * 512;
            for i in (0..512).step_by(2) {
                let word = (buffer[offset + i] as u16) | ((buffer[offset + i + 1] as u16) << 8);
                self.write_data(word);
            }
        }
        
        // Flush cache
        self.write_reg(ATA_REG_COMMAND, if use_lba48 { ATA_CMD_CACHE_FLUSH_EXT } else { ATA_CMD_CACHE_FLUSH });
        self.poll_bsy()?;
        
        Ok(())
    }
    
    fn sector_size(&self) -> u32 {
        512
    }
    
    fn sector_count(&self) -> u64 {
        self.size
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        let use_lba48 = self.supports_lba48();
        self.write_reg(ATA_REG_COMMAND, if use_lba48 { ATA_CMD_CACHE_FLUSH_EXT } else { ATA_CMD_CACHE_FLUSH });
        self.poll_bsy()
    }
}

/// Detect ATA devices on both channels.
pub fn detect_devices() -> Result<Vec<AtaDevice>, StorageError> {
    let mut devices = Vec::new();
    
    for channel in 0..2 {
        for drive in 0..2 {
            let mut device = AtaDevice::new(channel, drive);
            if device.identify().is_ok() && device.device_type == AtaDeviceType::Ata {
                devices.push(device);
            }
        }
    }
    
    Ok(devices)
}
