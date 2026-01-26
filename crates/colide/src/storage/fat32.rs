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

//! # FAT32 Filesystem
//!
//! FAT32 filesystem implementation for Colide OS.
//! Supports long file names (VFAT) and standard FAT32 operations.

use super::{BlockDevice, StorageError};
use std::sync::Arc;

/// FAT32 Boot Sector.
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct Fat32BootSector {
    pub jump: [u8; 3],
    pub oem_name: [u8; 8],
    pub bytes_per_sector: u16,
    pub sectors_per_cluster: u8,
    pub reserved_sectors: u16,
    pub num_fats: u8,
    pub root_entry_count: u16,
    pub total_sectors_16: u16,
    pub media_type: u8,
    pub fat_size_16: u16,
    pub sectors_per_track: u16,
    pub num_heads: u16,
    pub hidden_sectors: u32,
    pub total_sectors_32: u32,
    // FAT32 specific
    pub fat_size_32: u32,
    pub ext_flags: u16,
    pub fs_version: u16,
    pub root_cluster: u32,
    pub fs_info: u16,
    pub backup_boot: u16,
    pub reserved: [u8; 12],
    pub drive_num: u8,
    pub reserved1: u8,
    pub boot_sig: u8,
    pub volume_id: u32,
    pub volume_label: [u8; 11],
    pub fs_type: [u8; 8],
}

/// Directory entry (short name).
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct DirEntry {
    pub name: [u8; 8],
    pub ext: [u8; 3],
    pub attr: u8,
    pub nt_reserved: u8,
    pub create_time_tenth: u8,
    pub create_time: u16,
    pub create_date: u16,
    pub access_date: u16,
    pub cluster_hi: u16,
    pub modify_time: u16,
    pub modify_date: u16,
    pub cluster_lo: u16,
    pub size: u32,
}

/// Long filename entry.
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct LfnEntry {
    pub order: u8,
    pub name1: [u16; 5],
    pub attr: u8,
    pub entry_type: u8,
    pub checksum: u8,
    pub name2: [u16; 6],
    pub cluster: u16,
    pub name3: [u16; 2],
}

/// File attributes.
pub mod attr {
    pub const READ_ONLY: u8 = 0x01;
    pub const HIDDEN: u8 = 0x02;
    pub const SYSTEM: u8 = 0x04;
    pub const VOLUME_ID: u8 = 0x08;
    pub const DIRECTORY: u8 = 0x10;
    pub const ARCHIVE: u8 = 0x20;
    pub const LFN: u8 = READ_ONLY | HIDDEN | SYSTEM | VOLUME_ID;
}

/// FAT entry values.
pub mod fat_entry {
    pub const FREE: u32 = 0x00000000;
    pub const BAD: u32 = 0x0FFFFFF7;
    pub const EOF: u32 = 0x0FFFFFF8;
    pub const EOF_MAX: u32 = 0x0FFFFFFF;
}

/// File information.
#[derive(Debug, Clone)]
pub struct FileInfo {
    pub name: String,
    pub size: u64,
    pub is_directory: bool,
    pub is_readonly: bool,
    pub is_hidden: bool,
    pub cluster: u32,
    pub create_time: u64,
    pub modify_time: u64,
}

/// FAT32 filesystem instance.
pub struct Fat32 {
    device: Arc<dyn BlockDevice>,
    partition_start: u64,
    bytes_per_sector: u32,
    sectors_per_cluster: u32,
    reserved_sectors: u32,
    num_fats: u32,
    fat_size: u32,
    root_cluster: u32,
    total_sectors: u64,
    data_start_sector: u64,
}

impl Fat32 {
    /// Mount a FAT32 filesystem from a block device.
    pub fn mount(device: Arc<dyn BlockDevice>, partition_start: u64) -> Result<Self, StorageError> {
        // Read boot sector
        let mut boot_sector = [0u8; 512];
        device.read_sectors(partition_start, 1, &mut boot_sector)?;
        
        // Parse boot sector
        let bs: Fat32BootSector = unsafe {
            std::ptr::read_unaligned(boot_sector.as_ptr() as *const Fat32BootSector)
        };
        
        // Validate FAT32 signature
        if &bs.fs_type[0..5] != b"FAT32" {
            return Err(StorageError::CorruptedData);
        }
        
        let bytes_per_sector = bs.bytes_per_sector as u32;
        let sectors_per_cluster = bs.sectors_per_cluster as u32;
        let reserved_sectors = bs.reserved_sectors as u32;
        let num_fats = bs.num_fats as u32;
        let fat_size = bs.fat_size_32;
        let root_cluster = bs.root_cluster;
        let total_sectors = if bs.total_sectors_16 != 0 {
            bs.total_sectors_16 as u64
        } else {
            bs.total_sectors_32 as u64
        };
        
        let data_start_sector = partition_start
            + reserved_sectors as u64
            + (num_fats as u64 * fat_size as u64);
        
        Ok(Fat32 {
            device,
            partition_start,
            bytes_per_sector,
            sectors_per_cluster,
            reserved_sectors,
            num_fats,
            fat_size,
            root_cluster,
            total_sectors,
            data_start_sector,
        })
    }
    
    /// Get the first sector of a cluster.
    fn cluster_to_sector(&self, cluster: u32) -> u64 {
        self.data_start_sector + ((cluster - 2) as u64 * self.sectors_per_cluster as u64)
    }
    
    /// Read a cluster into a buffer.
    fn read_cluster(&self, cluster: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        let sector = self.cluster_to_sector(cluster);
        self.device.read_sectors(sector, self.sectors_per_cluster, buffer)
    }
    
    /// Write a cluster from a buffer.
    fn write_cluster(&self, cluster: u32, buffer: &[u8]) -> Result<(), StorageError> {
        let sector = self.cluster_to_sector(cluster);
        self.device.write_sectors(sector, self.sectors_per_cluster, buffer)
    }
    
    /// Read a FAT entry.
    fn read_fat_entry(&self, cluster: u32) -> Result<u32, StorageError> {
        let fat_offset = cluster * 4;
        let fat_sector = self.partition_start
            + self.reserved_sectors as u64
            + (fat_offset / self.bytes_per_sector) as u64;
        let entry_offset = (fat_offset % self.bytes_per_sector) as usize;
        
        let mut sector_buf = [0u8; 512];
        self.device.read_sectors(fat_sector, 1, &mut sector_buf)?;
        
        let entry = u32::from_le_bytes([
            sector_buf[entry_offset],
            sector_buf[entry_offset + 1],
            sector_buf[entry_offset + 2],
            sector_buf[entry_offset + 3],
        ]) & 0x0FFFFFFF;
        
        Ok(entry)
    }
    
    /// Write a FAT entry.
    fn write_fat_entry(&self, cluster: u32, value: u32) -> Result<(), StorageError> {
        let fat_offset = cluster * 4;
        let fat_sector = self.partition_start
            + self.reserved_sectors as u64
            + (fat_offset / self.bytes_per_sector) as u64;
        let entry_offset = (fat_offset % self.bytes_per_sector) as usize;
        
        let mut sector_buf = [0u8; 512];
        self.device.read_sectors(fat_sector, 1, &mut sector_buf)?;
        
        let bytes = (value & 0x0FFFFFFF).to_le_bytes();
        sector_buf[entry_offset..entry_offset + 4].copy_from_slice(&bytes);
        
        // Write to all FATs
        for fat_num in 0..self.num_fats {
            let target_sector = fat_sector + (fat_num as u64 * self.fat_size as u64);
            self.device.write_sectors(target_sector, 1, &sector_buf)?;
        }
        
        Ok(())
    }
    
    /// Get the cluster chain for a file.
    fn get_cluster_chain(&self, start_cluster: u32) -> Result<Vec<u32>, StorageError> {
        let mut chain = Vec::new();
        let mut cluster = start_cluster;
        
        while cluster >= 2 && cluster < fat_entry::BAD {
            chain.push(cluster);
            cluster = self.read_fat_entry(cluster)?;
        }
        
        Ok(chain)
    }
    
    /// List directory contents.
    pub fn list_directory(&self, path: &str) -> Result<Vec<FileInfo>, StorageError> {
        let cluster = if path == "/" || path.is_empty() {
            self.root_cluster
        } else {
            self.find_entry(path)?.cluster
        };
        
        self.list_directory_cluster(cluster)
    }
    
    /// List directory by cluster.
    fn list_directory_cluster(&self, cluster: u32) -> Result<Vec<FileInfo>, StorageError> {
        let mut entries = Vec::new();
        let cluster_size = (self.sectors_per_cluster * self.bytes_per_sector) as usize;
        let mut buffer = vec![0u8; cluster_size];
        let mut lfn_buffer = String::new();
        
        let chain = self.get_cluster_chain(cluster)?;
        
        for clust in chain {
            self.read_cluster(clust, &mut buffer)?;
            
            for i in (0..cluster_size).step_by(32) {
                let entry = &buffer[i..i + 32];
                
                // End of directory
                if entry[0] == 0x00 {
                    return Ok(entries);
                }
                
                // Deleted entry
                if entry[0] == 0xE5 {
                    continue;
                }
                
                let attr = entry[11];
                
                // Long filename entry
                if attr == attr::LFN {
                    let lfn: LfnEntry = unsafe {
                        std::ptr::read_unaligned(entry.as_ptr() as *const LfnEntry)
                    };
                    
                    let mut name_part = String::new();
                    // Copy packed fields to local arrays to avoid unaligned access
                    let name1: [u16; 5] = lfn.name1;
                    let name2: [u16; 6] = lfn.name2;
                    let name3: [u16; 2] = lfn.name3;
                    for &ch in name1.iter().chain(name2.iter()).chain(name3.iter()) {
                        if ch == 0 || ch == 0xFFFF {
                            break;
                        }
                        if let Some(c) = char::from_u32(ch as u32) {
                            name_part.push(c);
                        }
                    }
                    
                    if (lfn.order & 0x40) != 0 {
                        lfn_buffer = name_part;
                    } else {
                        lfn_buffer = name_part + &lfn_buffer;
                    }
                    continue;
                }
                
                // Skip volume label
                if (attr & attr::VOLUME_ID) != 0 {
                    continue;
                }
                
                let dir_entry: DirEntry = unsafe {
                    std::ptr::read_unaligned(entry.as_ptr() as *const DirEntry)
                };
                
                // Get short name if no LFN
                let name = if lfn_buffer.is_empty() {
                    let short_name = std::str::from_utf8(&dir_entry.name)
                        .unwrap_or("")
                        .trim();
                    let ext = std::str::from_utf8(&dir_entry.ext)
                        .unwrap_or("")
                        .trim();
                    
                    if ext.is_empty() {
                        short_name.to_string()
                    } else {
                        format!("{}.{}", short_name, ext)
                    }
                } else {
                    std::mem::take(&mut lfn_buffer)
                };
                
                // Skip . and .. entries
                if name == "." || name == ".." {
                    continue;
                }
                
                let cluster_num = ((dir_entry.cluster_hi as u32) << 16) | (dir_entry.cluster_lo as u32);
                
                entries.push(FileInfo {
                    name,
                    size: dir_entry.size as u64,
                    is_directory: (attr & attr::DIRECTORY) != 0,
                    is_readonly: (attr & attr::READ_ONLY) != 0,
                    is_hidden: (attr & attr::HIDDEN) != 0,
                    cluster: cluster_num,
                    create_time: 0, // TODO: Parse FAT time
                    modify_time: 0,
                });
            }
        }
        
        Ok(entries)
    }
    
    /// Find a directory entry by path.
    fn find_entry(&self, path: &str) -> Result<FileInfo, StorageError> {
        let parts: Vec<&str> = path.trim_matches('/').split('/').filter(|s| !s.is_empty()).collect();
        let mut current_cluster = self.root_cluster;
        
        for (i, part) in parts.iter().enumerate() {
            let entries = self.list_directory_cluster(current_cluster)?;
            
            let entry = entries.iter()
                .find(|e| e.name.eq_ignore_ascii_case(part))
                .ok_or(StorageError::NotFound)?;
            
            if i < parts.len() - 1 {
                if !entry.is_directory {
                    return Err(StorageError::NotADirectory);
                }
                current_cluster = entry.cluster;
            } else {
                return Ok(entry.clone());
            }
        }
        
        Err(StorageError::NotFound)
    }
    
    /// Read file contents.
    pub fn read_file(&self, path: &str) -> Result<Vec<u8>, StorageError> {
        let entry = self.find_entry(path)?;
        
        if entry.is_directory {
            return Err(StorageError::NotAFile);
        }
        
        let cluster_size = (self.sectors_per_cluster * self.bytes_per_sector) as usize;
        let mut data = Vec::with_capacity(entry.size as usize);
        let mut buffer = vec![0u8; cluster_size];
        let mut remaining = entry.size as usize;
        
        let chain = self.get_cluster_chain(entry.cluster)?;
        
        for cluster in chain {
            self.read_cluster(cluster, &mut buffer)?;
            
            let to_copy = remaining.min(cluster_size);
            data.extend_from_slice(&buffer[..to_copy]);
            remaining -= to_copy;
            
            if remaining == 0 {
                break;
            }
        }
        
        Ok(data)
    }
    
    /// Check if a path exists.
    pub fn exists(&self, path: &str) -> bool {
        self.find_entry(path).is_ok()
    }
    
    /// Get file info.
    pub fn stat(&self, path: &str) -> Result<FileInfo, StorageError> {
        self.find_entry(path)
    }
}
