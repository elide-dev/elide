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

//! # USB Mass Storage Class Driver
//!
//! Implements the USB Mass Storage Class (MSC) for USB flash drives and external disks.
//! Supports Bulk-Only Transport (BOT) protocol.

use super::host::{UsbHostController, UsbDevice, UsbError, UsbSetupPacket, EndpointDirection};
use crate::storage::{BlockDevice, StorageError};
use std::sync::{Arc, Mutex};

/// Mass Storage Class subclass codes.
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MscSubclass {
    Rbc = 0x01,
    Mmc5 = 0x02,
    Ufi = 0x04,
    ScsiTransparent = 0x06,
    LsdFs = 0x07,
    Ieee1667 = 0x08,
}

/// Mass Storage Class protocol codes.
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MscProtocol {
    Cbi = 0x00,
    CbiNoInt = 0x01,
    BulkOnly = 0x50,
    Uas = 0x62,
}

/// Command Block Wrapper (CBW) for Bulk-Only Transport.
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct CommandBlockWrapper {
    pub signature: u32,
    pub tag: u32,
    pub data_transfer_length: u32,
    pub flags: u8,
    pub lun: u8,
    pub cb_length: u8,
    pub cb: [u8; 16],
}

impl CommandBlockWrapper {
    pub const SIGNATURE: u32 = 0x43425355; // "USBC"
    
    pub fn new(tag: u32, length: u32, direction_in: bool, lun: u8, command: &[u8]) -> Self {
        let mut cb = [0u8; 16];
        let len = command.len().min(16);
        cb[..len].copy_from_slice(&command[..len]);
        
        Self {
            signature: Self::SIGNATURE,
            tag,
            data_transfer_length: length,
            flags: if direction_in { 0x80 } else { 0x00 },
            lun,
            cb_length: len as u8,
            cb,
        }
    }
    
    pub fn to_bytes(&self) -> [u8; 31] {
        let mut bytes = [0u8; 31];
        bytes[0..4].copy_from_slice(&self.signature.to_le_bytes());
        bytes[4..8].copy_from_slice(&self.tag.to_le_bytes());
        bytes[8..12].copy_from_slice(&self.data_transfer_length.to_le_bytes());
        bytes[12] = self.flags;
        bytes[13] = self.lun;
        bytes[14] = self.cb_length;
        bytes[15..31].copy_from_slice(&self.cb);
        bytes
    }
}

/// Command Status Wrapper (CSW) for Bulk-Only Transport.
#[repr(C, packed)]
#[derive(Debug, Clone, Copy)]
pub struct CommandStatusWrapper {
    pub signature: u32,
    pub tag: u32,
    pub data_residue: u32,
    pub status: u8,
}

impl CommandStatusWrapper {
    pub const SIGNATURE: u32 = 0x53425355; // "USBS"
    pub const STATUS_PASSED: u8 = 0x00;
    pub const STATUS_FAILED: u8 = 0x01;
    pub const STATUS_PHASE_ERROR: u8 = 0x02;
    
    pub fn from_bytes(bytes: &[u8]) -> Option<Self> {
        if bytes.len() < 13 {
            return None;
        }
        
        let signature = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
        if signature != Self::SIGNATURE {
            return None;
        }
        
        Some(Self {
            signature,
            tag: u32::from_le_bytes([bytes[4], bytes[5], bytes[6], bytes[7]]),
            data_residue: u32::from_le_bytes([bytes[8], bytes[9], bytes[10], bytes[11]]),
            status: bytes[12],
        })
    }
}

/// SCSI commands.
pub mod scsi {
    pub const TEST_UNIT_READY: u8 = 0x00;
    pub const REQUEST_SENSE: u8 = 0x03;
    pub const INQUIRY: u8 = 0x12;
    pub const READ_CAPACITY_10: u8 = 0x25;
    pub const READ_10: u8 = 0x28;
    pub const WRITE_10: u8 = 0x2A;
    pub const READ_CAPACITY_16: u8 = 0x9E;
    pub const READ_16: u8 = 0x88;
    pub const WRITE_16: u8 = 0x8A;
}

/// USB Mass Storage device.
pub struct UsbMassStorage {
    controller: Arc<Mutex<dyn UsbHostController>>,
    device_address: u8,
    bulk_in_endpoint: u8,
    bulk_out_endpoint: u8,
    max_lun: u8,
    tag: u32,
    block_size: u32,
    block_count: u64,
    ready: bool,
}

impl UsbMassStorage {
    /// Create a new mass storage device.
    pub fn new(
        controller: Arc<Mutex<dyn UsbHostController>>,
        device: &UsbDevice,
    ) -> Result<Self, UsbError> {
        // Find bulk endpoints
        let mut bulk_in = None;
        let mut bulk_out = None;
        
        for config in &device.configurations {
            for iface in &config.interfaces {
                // Check for mass storage interface
                if iface.interface_class == 0x08 {
                    for ep in &iface.endpoints {
                        match ep.direction {
                            EndpointDirection::In => bulk_in = Some(ep.address | 0x80),
                            EndpointDirection::Out => bulk_out = Some(ep.address),
                        }
                    }
                }
            }
        }
        
        let bulk_in_endpoint = bulk_in.ok_or(UsbError::InvalidEndpoint)?;
        let bulk_out_endpoint = bulk_out.ok_or(UsbError::InvalidEndpoint)?;
        
        let mut msc = Self {
            controller,
            device_address: device.address,
            bulk_in_endpoint,
            bulk_out_endpoint,
            max_lun: 0,
            tag: 1,
            block_size: 512,
            block_count: 0,
            ready: false,
        };
        
        // Get max LUN
        msc.get_max_lun()?;
        
        // Initialize device
        msc.init_device()?;
        
        Ok(msc)
    }
    
    /// Get max LUN.
    fn get_max_lun(&mut self) -> Result<(), UsbError> {
        let mut ctrl = self.controller.lock().unwrap();
        
        let setup = UsbSetupPacket {
            request_type: 0xA1, // Device to host, class, interface
            request: 0xFE,      // Get Max LUN
            value: 0,
            index: 0,
            length: 1,
        };
        
        let mut data = [0u8; 1];
        match ctrl.control_transfer(self.device_address, &setup, Some(&mut data), 1000) {
            Ok(_) => self.max_lun = data[0],
            Err(UsbError::Stall) => self.max_lun = 0, // Single LUN device
            Err(e) => return Err(e),
        }
        
        Ok(())
    }
    
    /// Initialize the device.
    fn init_device(&mut self) -> Result<(), UsbError> {
        // Test unit ready
        for _ in 0..10 {
            if self.test_unit_ready().is_ok() {
                break;
            }
            std::thread::sleep(std::time::Duration::from_millis(100));
        }
        
        // Read capacity
        self.read_capacity()?;
        self.ready = true;
        
        Ok(())
    }
    
    /// Execute a SCSI command.
    fn execute_command(
        &mut self,
        command: &[u8],
        data: Option<&mut [u8]>,
        direction_in: bool,
    ) -> Result<(), UsbError> {
        let data_len = data.as_ref().map(|d| d.len()).unwrap_or(0) as u32;
        let tag = self.tag;
        self.tag = self.tag.wrapping_add(1);
        
        // Send CBW
        let cbw = CommandBlockWrapper::new(tag, data_len, direction_in, 0, command);
        let cbw_bytes = cbw.to_bytes();
        
        {
            let mut ctrl = self.controller.lock().unwrap();
            let mut buf = cbw_bytes.to_vec();
            ctrl.bulk_transfer(self.device_address, self.bulk_out_endpoint, &mut buf, 5000)?;
        }
        
        // Data phase
        if let Some(data) = data {
            let mut ctrl = self.controller.lock().unwrap();
            let endpoint = if direction_in { self.bulk_in_endpoint } else { self.bulk_out_endpoint };
            ctrl.bulk_transfer(self.device_address, endpoint, data, 5000)?;
        }
        
        // Receive CSW
        let mut csw_bytes = [0u8; 13];
        {
            let mut ctrl = self.controller.lock().unwrap();
            ctrl.bulk_transfer(self.device_address, self.bulk_in_endpoint, &mut csw_bytes, 5000)?;
        }
        
        let csw = CommandStatusWrapper::from_bytes(&csw_bytes)
            .ok_or(UsbError::IoError)?;
        
        if csw.tag != tag {
            return Err(UsbError::IoError);
        }
        
        if csw.status != CommandStatusWrapper::STATUS_PASSED {
            return Err(UsbError::IoError);
        }
        
        Ok(())
    }
    
    /// Test unit ready.
    fn test_unit_ready(&mut self) -> Result<(), UsbError> {
        let command = [scsi::TEST_UNIT_READY, 0, 0, 0, 0, 0];
        self.execute_command(&command, None, false)
    }
    
    /// Read device capacity.
    fn read_capacity(&mut self) -> Result<(), UsbError> {
        let command = [scsi::READ_CAPACITY_10, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        let mut data = [0u8; 8];
        
        self.execute_command(&command, Some(&mut data), true)?;
        
        let last_lba = u32::from_be_bytes([data[0], data[1], data[2], data[3]]);
        self.block_size = u32::from_be_bytes([data[4], data[5], data[6], data[7]]);
        self.block_count = (last_lba as u64) + 1;
        
        // If max LBA is 0xFFFFFFFF, use READ CAPACITY (16)
        if last_lba == 0xFFFFFFFF {
            let command = [scsi::READ_CAPACITY_16, 0x10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0];
            let mut data = [0u8; 32];
            
            self.execute_command(&command, Some(&mut data), true)?;
            
            self.block_count = u64::from_be_bytes([
                data[0], data[1], data[2], data[3],
                data[4], data[5], data[6], data[7],
            ]) + 1;
            self.block_size = u32::from_be_bytes([data[8], data[9], data[10], data[11]]);
        }
        
        Ok(())
    }
    
    /// Read sectors from the device.
    pub fn read_sectors(&mut self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), UsbError> {
        if !self.ready {
            return Err(UsbError::NotInitialized);
        }
        
        if lba + count as u64 > self.block_count {
            return Err(UsbError::IoError);
        }
        
        let transfer_length = count * self.block_size;
        if buffer.len() < transfer_length as usize {
            return Err(UsbError::BufferTooSmall);
        }
        
        // Use READ(10) for small LBAs, READ(16) for large
        if lba <= 0xFFFFFFFF && count <= 0xFFFF {
            let command = [
                scsi::READ_10,
                0,
                (lba >> 24) as u8,
                (lba >> 16) as u8,
                (lba >> 8) as u8,
                lba as u8,
                0,
                (count >> 8) as u8,
                count as u8,
                0,
            ];
            self.execute_command(&command, Some(&mut buffer[..transfer_length as usize]), true)
        } else {
            let command = [
                scsi::READ_16,
                0,
                (lba >> 56) as u8,
                (lba >> 48) as u8,
                (lba >> 40) as u8,
                (lba >> 32) as u8,
                (lba >> 24) as u8,
                (lba >> 16) as u8,
                (lba >> 8) as u8,
                lba as u8,
                (count >> 24) as u8,
                (count >> 16) as u8,
                (count >> 8) as u8,
                count as u8,
                0,
                0,
            ];
            self.execute_command(&command, Some(&mut buffer[..transfer_length as usize]), true)
        }
    }
    
    /// Write sectors to the device.
    pub fn write_sectors(&mut self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), UsbError> {
        if !self.ready {
            return Err(UsbError::NotInitialized);
        }
        
        if lba + count as u64 > self.block_count {
            return Err(UsbError::IoError);
        }
        
        let transfer_length = count * self.block_size;
        if buffer.len() < transfer_length as usize {
            return Err(UsbError::BufferTooSmall);
        }
        
        let mut write_buf = buffer[..transfer_length as usize].to_vec();
        
        if lba <= 0xFFFFFFFF && count <= 0xFFFF {
            let command = [
                scsi::WRITE_10,
                0,
                (lba >> 24) as u8,
                (lba >> 16) as u8,
                (lba >> 8) as u8,
                lba as u8,
                0,
                (count >> 8) as u8,
                count as u8,
                0,
            ];
            self.execute_command(&command, Some(&mut write_buf), false)
        } else {
            let command = [
                scsi::WRITE_16,
                0,
                (lba >> 56) as u8,
                (lba >> 48) as u8,
                (lba >> 40) as u8,
                (lba >> 32) as u8,
                (lba >> 24) as u8,
                (lba >> 16) as u8,
                (lba >> 8) as u8,
                lba as u8,
                (count >> 24) as u8,
                (count >> 16) as u8,
                (count >> 8) as u8,
                count as u8,
                0,
                0,
            ];
            self.execute_command(&command, Some(&mut write_buf), false)
        }
    }
    
    /// Get block size.
    pub fn block_size(&self) -> u32 {
        self.block_size
    }
    
    /// Get block count.
    pub fn block_count(&self) -> u64 {
        self.block_count
    }
}

/// Wrapper to implement BlockDevice for UsbMassStorage.
pub struct UsbStorageBlockDevice {
    inner: Mutex<UsbMassStorage>,
}

impl UsbStorageBlockDevice {
    pub fn new(msc: UsbMassStorage) -> Self {
        Self {
            inner: Mutex::new(msc),
        }
    }
}

impl BlockDevice for UsbStorageBlockDevice {
    fn read_sectors(&self, lba: u64, count: u32, buffer: &mut [u8]) -> Result<(), StorageError> {
        self.inner.lock().unwrap()
            .read_sectors(lba, count, buffer)
            .map_err(|_| StorageError::IoError("USB read error".to_string()))
    }
    
    fn write_sectors(&self, lba: u64, count: u32, buffer: &[u8]) -> Result<(), StorageError> {
        self.inner.lock().unwrap()
            .write_sectors(lba, count, buffer)
            .map_err(|_| StorageError::IoError("USB write error".to_string()))
    }
    
    fn sector_size(&self) -> u32 {
        self.inner.lock().unwrap().block_size()
    }
    
    fn sector_count(&self) -> u64 {
        self.inner.lock().unwrap().block_count()
    }
    
    fn flush(&self) -> Result<(), StorageError> {
        Ok(())
    }
}
