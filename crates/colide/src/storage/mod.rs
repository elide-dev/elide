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

//! # Storage Subsystem
//!
//! Storage drivers for Colide OS:
//! - ATA/AHCI: SATA disk controller
//! - FAT32: File system implementation
//! - Block device abstraction

pub mod ata;
pub mod ahci;
pub mod fat32;
pub mod block;

use std::sync::{Arc, Mutex};
use lazy_static::lazy_static;

/// Block device trait for storage abstraction.
pub trait BlockDevice: Send + Sync {
    /// Read sectors from the device.
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError>;
    
    /// Write sectors to the device.
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError>;
    
    /// Get the sector size (typically 512 bytes).
    fn sector_size(&self) -> u32;
    
    /// Get the total number of sectors.
    fn sector_count(&self) -> u64;
    
    /// Flush any cached writes.
    fn flush(&self) -> Result<(), StorageError>;
}

/// Storage error types.
#[derive(Debug, Clone)]
pub enum StorageError {
    NotFound,
    IoError(String),
    InvalidSector,
    DeviceBusy,
    Timeout,
    NotSupported,
    CorruptedData,
    NoSpace,
    InvalidPath,
    NotAFile,
    NotADirectory,
    AlreadyExists,
}

impl std::fmt::Display for StorageError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            StorageError::NotFound => write!(f, "Device or file not found"),
            StorageError::IoError(msg) => write!(f, "I/O error: {}", msg),
            StorageError::InvalidSector => write!(f, "Invalid sector address"),
            StorageError::DeviceBusy => write!(f, "Device is busy"),
            StorageError::Timeout => write!(f, "Operation timed out"),
            StorageError::NotSupported => write!(f, "Operation not supported"),
            StorageError::CorruptedData => write!(f, "Data corruption detected"),
            StorageError::NoSpace => write!(f, "No space left on device"),
            StorageError::InvalidPath => write!(f, "Invalid path"),
            StorageError::NotAFile => write!(f, "Not a file"),
            StorageError::NotADirectory => write!(f, "Not a directory"),
            StorageError::AlreadyExists => write!(f, "Already exists"),
        }
    }
}

impl std::error::Error for StorageError {}

/// Storage manager for device enumeration and access.
pub struct StorageManager {
    devices: Vec<Arc<Mutex<dyn BlockDevice>>>,
}

lazy_static! {
    static ref STORAGE_MANAGER: Mutex<StorageManager> = Mutex::new(StorageManager::new());
}

impl StorageManager {
    pub fn new() -> Self {
        StorageManager {
            devices: Vec::new(),
        }
    }
    
    /// Initialize storage subsystem, detect devices.
    pub fn init() -> Result<(), StorageError> {
        let mut manager = STORAGE_MANAGER.lock().unwrap();
        
        // Try AHCI first (modern SATA)
        if let Ok(devices) = ahci::detect_devices() {
            for dev in devices {
                manager.devices.push(Arc::new(Mutex::new(dev)));
            }
        }
        
        // Fall back to legacy ATA
        if manager.devices.is_empty() {
            if let Ok(devices) = ata::detect_devices() {
                for dev in devices {
                    manager.devices.push(Arc::new(Mutex::new(dev)));
                }
            }
        }
        
        Ok(())
    }
    
    /// Get the number of detected storage devices.
    pub fn device_count() -> usize {
        STORAGE_MANAGER.lock().unwrap().devices.len()
    }
    
    /// Get a storage device by index.
    pub fn get_device(index: usize) -> Option<Arc<Mutex<dyn BlockDevice>>> {
        let manager = STORAGE_MANAGER.lock().unwrap();
        manager.devices.get(index).cloned()
    }
}

impl Default for StorageManager {
    fn default() -> Self {
        Self::new()
    }
}

/// Partition table entry.
#[derive(Debug, Clone)]
pub struct Partition {
    pub bootable: bool,
    pub partition_type: u8,
    pub start_lba: u64,
    pub sector_count: u64,
}

/// Read MBR partition table.
pub fn read_mbr_partitions(device: &dyn BlockDevice) -> Result<Vec<Partition>, StorageError> {
    let mut mbr = [0u8; 512];
    device.read_sectors(0, 1, &mut mbr)?;
    
    // Check MBR signature
    if mbr[510] != 0x55 || mbr[511] != 0xAA {
        return Err(StorageError::CorruptedData);
    }
    
    let mut partitions = Vec::new();
    
    // Parse 4 partition entries starting at offset 446
    for i in 0..4 {
        let offset = 446 + i * 16;
        let entry = &mbr[offset..offset + 16];
        
        let bootable = entry[0] == 0x80;
        let partition_type = entry[4];
        
        // Skip empty entries
        if partition_type == 0 {
            continue;
        }
        
        let start_lba = u32::from_le_bytes([entry[8], entry[9], entry[10], entry[11]]) as u64;
        let sector_count = u32::from_le_bytes([entry[12], entry[13], entry[14], entry[15]]) as u64;
        
        partitions.push(Partition {
            bootable,
            partition_type,
            start_lba,
            sector_count,
        });
    }
    
    Ok(partitions)
}

/// Read GPT partition table.
pub fn read_gpt_partitions(device: &dyn BlockDevice) -> Result<Vec<Partition>, StorageError> {
    // Read protective MBR
    let mut mbr = [0u8; 512];
    device.read_sectors(0, 1, &mut mbr)?;
    
    // Read GPT header at LBA 1
    let mut gpt_header = [0u8; 512];
    device.read_sectors(1, 1, &mut gpt_header)?;
    
    // Check GPT signature "EFI PART"
    if &gpt_header[0..8] != b"EFI PART" {
        return Err(StorageError::CorruptedData);
    }
    
    let partition_entry_lba = u64::from_le_bytes([
        gpt_header[72], gpt_header[73], gpt_header[74], gpt_header[75],
        gpt_header[76], gpt_header[77], gpt_header[78], gpt_header[79],
    ]);
    
    let num_partition_entries = u32::from_le_bytes([
        gpt_header[80], gpt_header[81], gpt_header[82], gpt_header[83],
    ]);
    
    let partition_entry_size = u32::from_le_bytes([
        gpt_header[84], gpt_header[85], gpt_header[86], gpt_header[87],
    ]);
    
    let mut partitions = Vec::new();
    let entries_per_sector = 512 / partition_entry_size;
    let sectors_needed = (num_partition_entries + entries_per_sector - 1) / entries_per_sector;
    
    let mut entry_buffer = vec![0u8; (sectors_needed * 512) as usize];
    device.read_sectors(partition_entry_lba, sectors_needed, &mut entry_buffer)?;
    
    for i in 0..num_partition_entries {
        let offset = (i * partition_entry_size) as usize;
        let entry = &entry_buffer[offset..offset + partition_entry_size as usize];
        
        // Check if partition type GUID is all zeros (unused entry)
        let is_empty = entry[0..16].iter().all(|&b| b == 0);
        if is_empty {
            continue;
        }
        
        let start_lba = u64::from_le_bytes([
            entry[32], entry[33], entry[34], entry[35],
            entry[36], entry[37], entry[38], entry[39],
        ]);
        
        let end_lba = u64::from_le_bytes([
            entry[40], entry[41], entry[42], entry[43],
            entry[44], entry[45], entry[46], entry[47],
        ]);
        
        partitions.push(Partition {
            bootable: false,
            partition_type: 0xEE, // GPT protective
            start_lba,
            sector_count: end_lba - start_lba + 1,
        });
    }
    
    Ok(partitions)
}
