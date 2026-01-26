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

//! # Block Device Utilities
//!
//! Utilities for working with block devices, including
//! memory-backed block devices for testing.

use super::{BlockDevice, StorageError};
use std::sync::Mutex;

/// Memory-backed block device for testing.
pub struct MemoryBlockDevice {
    data: Mutex<Vec<u8>>,
    sector_size: u32,
    sector_count: u64,
}

impl MemoryBlockDevice {
    /// Create a new memory block device.
    pub fn new(size_bytes: usize) -> Self {
        let sector_size = 512;
        let sector_count = (size_bytes / sector_size as usize) as u64;
        
        MemoryBlockDevice {
            data: Mutex::new(vec![0u8; size_bytes]),
            sector_size,
            sector_count,
        }
    }
    
    /// Create from existing data.
    pub fn from_data(data: Vec<u8>) -> Self {
        let sector_size = 512;
        let sector_count = (data.len() / sector_size as usize) as u64;
        
        MemoryBlockDevice {
            data: Mutex::new(data),
            sector_size,
            sector_count,
        }
    }
    
    /// Get the underlying data.
    pub fn into_data(self) -> Vec<u8> {
        self.data.into_inner().unwrap()
    }
}

impl BlockDevice for MemoryBlockDevice {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        let data = self.data.lock().unwrap();
        
        let start = (lba * self.sector_size as u64) as usize;
        let end = start + (count as usize * self.sector_size as usize);
        
        if end > data.len() {
            return Err(StorageError::InvalidSector);
        }
        
        if buffer.len() < (count as usize * self.sector_size as usize) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        buffer[..end - start].copy_from_slice(&data[start..end]);
        Ok(())
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        let mut data = self.data.lock().unwrap();
        
        let start = (lba * self.sector_size as u64) as usize;
        let end = start + (count as usize * self.sector_size as usize);
        
        if end > data.len() {
            return Err(StorageError::InvalidSector);
        }
        
        if buffer.len() < (count as usize * self.sector_size as usize) {
            return Err(StorageError::IoError("Buffer too small".to_string()));
        }
        
        data[start..end].copy_from_slice(&buffer[..end - start]);
        Ok(())
    }
    
    fn sector_size(&self) -> u32 {
        self.sector_size
    }
    
    fn sector_count(&self) -> u64 {
        self.sector_count
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        Ok(())
    }
}

/// Partition wrapper - presents a partition as a block device.
pub struct PartitionDevice<T: BlockDevice> {
    inner: T,
    start_sector: u64,
    sector_count: u64,
}

impl<T: BlockDevice> PartitionDevice<T> {
    /// Create a partition device.
    pub fn new(device: T, start_sector: u64, sector_count: u64) -> Self {
        PartitionDevice {
            inner: device,
            start_sector,
            sector_count,
        }
    }
}

impl<T: BlockDevice> BlockDevice for PartitionDevice<T> {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        if lba + count as u64 > self.sector_count {
            return Err(StorageError::InvalidSector);
        }
        self.inner.read_sectors(self.start_sector + lba, count, buffer)
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        if lba + count as u64 > self.sector_count {
            return Err(StorageError::InvalidSector);
        }
        self.inner.write_sectors(self.start_sector + lba, count, buffer)
    }
    
    fn sector_size(&self) -> u32 {
        self.inner.sector_size()
    }
    
    fn sector_count(&self) -> u64 {
        self.sector_count
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        self.inner.flush()
    }
}

/// Cached block device - adds read caching.
pub struct CachedBlockDevice<T: BlockDevice> {
    inner: T,
    cache: Mutex<std::collections::HashMap<u64, Vec<u8>>>,
    max_cached_sectors: usize,
}

impl<T: BlockDevice> CachedBlockDevice<T> {
    /// Create a cached block device.
    pub fn new(device: T, max_cached_sectors: usize) -> Self {
        CachedBlockDevice {
            inner: device,
            cache: Mutex::new(std::collections::HashMap::new()),
            max_cached_sectors,
        }
    }
    
    /// Clear the cache.
    pub fn clear_cache(&self) {
        self.cache.lock().unwrap().clear();
    }
}

impl<T: BlockDevice> BlockDevice for CachedBlockDevice<T> {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        let sector_size = self.inner.sector_size() as usize;
        let mut cache = self.cache.lock().unwrap();
        
        for i in 0..count {
            let sector_lba = lba + i as u64;
            let offset = i as usize * sector_size;
            
            if let Some(cached) = cache.get(&sector_lba) {
                buffer[offset..offset + sector_size].copy_from_slice(cached);
            } else {
                let mut sector_buf = vec![0u8; sector_size];
                self.inner.read_sectors(sector_lba, 1, &mut sector_buf)?;
                buffer[offset..offset + sector_size].copy_from_slice(&sector_buf);
                
                // Add to cache if not full
                if cache.len() < self.max_cached_sectors {
                    cache.insert(sector_lba, sector_buf);
                }
            }
        }
        
        Ok(())
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        // Invalidate cache for written sectors
        let mut cache = self.cache.lock().unwrap();
        for i in 0..count {
            cache.remove(&(lba + i as u64));
        }
        drop(cache);
        
        self.inner.write_sectors(lba, count, buffer)
    }
    
    fn sector_size(&self) -> u32 {
        self.inner.sector_size()
    }
    
    fn sector_count(&self) -> u64 {
        self.inner.sector_count()
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        self.inner.flush()
    }
}
