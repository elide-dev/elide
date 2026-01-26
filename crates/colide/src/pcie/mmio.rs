//! Memory-Mapped I/O (MMIO) for PCIe devices
//!
//! Provides safe(r) access to device registers via memory mapping.

use core::ptr::{read_volatile, write_volatile};

/// MMIO region for a PCIe device
pub struct MmioRegion {
    pub base: u64,
    pub size: u64,
}

impl MmioRegion {
    /// Create new MMIO region
    pub fn new(base: u64, size: u64) -> Self {
        Self { base, size }
    }
    
    /// Check if offset is within region
    fn check_bounds(&self, offset: u64, size: usize) -> bool {
        offset + size as u64 <= self.size
    }
    
    /// Read 8-bit value
    pub fn read8(&self, offset: u64) -> u8 {
        if !self.check_bounds(offset, 1) {
            return 0;
        }
        unsafe {
            read_volatile((self.base + offset) as *const u8)
        }
    }
    
    /// Read 16-bit value
    pub fn read16(&self, offset: u64) -> u16 {
        if !self.check_bounds(offset, 2) {
            return 0;
        }
        unsafe {
            read_volatile((self.base + offset) as *const u16)
        }
    }
    
    /// Read 32-bit value
    pub fn read32(&self, offset: u64) -> u32 {
        if !self.check_bounds(offset, 4) {
            return 0;
        }
        unsafe {
            read_volatile((self.base + offset) as *const u32)
        }
    }
    
    /// Read 64-bit value
    pub fn read64(&self, offset: u64) -> u64 {
        if !self.check_bounds(offset, 8) {
            return 0;
        }
        unsafe {
            read_volatile((self.base + offset) as *const u64)
        }
    }
    
    /// Write 8-bit value
    pub fn write8(&self, offset: u64, value: u8) {
        if !self.check_bounds(offset, 1) {
            return;
        }
        unsafe {
            write_volatile((self.base + offset) as *mut u8, value);
        }
    }
    
    /// Write 16-bit value
    pub fn write16(&self, offset: u64, value: u16) {
        if !self.check_bounds(offset, 2) {
            return;
        }
        unsafe {
            write_volatile((self.base + offset) as *mut u16, value);
        }
    }
    
    /// Write 32-bit value
    pub fn write32(&self, offset: u64, value: u32) {
        if !self.check_bounds(offset, 4) {
            return;
        }
        unsafe {
            write_volatile((self.base + offset) as *mut u32, value);
        }
    }
    
    /// Write 64-bit value
    pub fn write64(&self, offset: u64, value: u64) {
        if !self.check_bounds(offset, 8) {
            return;
        }
        unsafe {
            write_volatile((self.base + offset) as *mut u64, value);
        }
    }
    
    /// Set bits in 32-bit register
    pub fn set_bits32(&self, offset: u64, bits: u32) {
        let val = self.read32(offset);
        self.write32(offset, val | bits);
    }
    
    /// Clear bits in 32-bit register
    pub fn clear_bits32(&self, offset: u64, bits: u32) {
        let val = self.read32(offset);
        self.write32(offset, val & !bits);
    }
    
    /// Wait for bit to be set (with timeout)
    pub fn wait_for_bit32(&self, offset: u64, bit: u32, timeout_us: u32) -> bool {
        for _ in 0..timeout_us {
            if (self.read32(offset) & bit) != 0 {
                return true;
            }
            // Busy-wait 1μs (approximate)
            for _ in 0..100 {
                core::hint::spin_loop();
            }
        }
        false
    }
    
    /// Wait for bit to be clear (with timeout)
    pub fn wait_for_bit_clear32(&self, offset: u64, bit: u32, timeout_us: u32) -> bool {
        for _ in 0..timeout_us {
            if (self.read32(offset) & bit) == 0 {
                return true;
            }
            for _ in 0..100 {
                core::hint::spin_loop();
            }
        }
        false
    }
    
    /// Read multiple 32-bit values into buffer
    pub fn read_buffer32(&self, offset: u64, buffer: &mut [u32]) {
        for (i, item) in buffer.iter_mut().enumerate() {
            *item = self.read32(offset + (i as u64 * 4));
        }
    }
    
    /// Write multiple 32-bit values from buffer
    pub fn write_buffer32(&self, offset: u64, buffer: &[u32]) {
        for (i, &item) in buffer.iter().enumerate() {
            self.write32(offset + (i as u64 * 4), item);
        }
    }
}

/// DMA-capable memory buffer
pub struct DmaBuffer {
    pub virt_addr: u64,
    pub phys_addr: u64,
    pub size: usize,
}

impl DmaBuffer {
    /// Allocate DMA buffer (stub - needs actual memory allocator)
    pub fn allocate(size: usize) -> Option<Self> {
        // In a real implementation, would allocate physically contiguous memory
        // For now, return None
        let _ = size;
        None
    }
    
    /// Get virtual address
    pub fn as_ptr<T>(&self) -> *mut T {
        self.virt_addr as *mut T
    }
    
    /// Get physical address for DMA
    pub fn dma_addr(&self) -> u64 {
        self.phys_addr
    }
}

/// Ring buffer for DMA descriptors
pub struct DmaRing<const N: usize> {
    pub descriptors: DmaBuffer,
    pub head: usize,
    pub tail: usize,
}

impl<const N: usize> DmaRing<N> {
    /// Check if ring is empty
    pub fn is_empty(&self) -> bool {
        self.head == self.tail
    }
    
    /// Check if ring is full
    pub fn is_full(&self) -> bool {
        (self.tail + 1) % N == self.head
    }
    
    /// Get number of entries in ring
    pub fn len(&self) -> usize {
        if self.tail >= self.head {
            self.tail - self.head
        } else {
            N - self.head + self.tail
        }
    }
    
    /// Advance tail pointer
    pub fn advance_tail(&mut self) {
        self.tail = (self.tail + 1) % N;
    }
    
    /// Advance head pointer
    pub fn advance_head(&mut self) {
        self.head = (self.head + 1) % N;
    }
}
