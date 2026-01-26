//! DMA Memory Allocator for PCIe Devices
//!
//! Provides physically contiguous memory allocation for DMA operations.
//! Essential for PCIe device drivers that need to share memory with hardware.

use core::ptr::NonNull;

/// DMA allocation error types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DmaError {
    OutOfMemory,
    InvalidSize,
    InvalidAlignment,
    NotAllocated,
}

/// DMA memory coherency mode
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum DmaDirection {
    /// Memory written by CPU, read by device
    ToDevice,
    /// Memory written by device, read by CPU
    FromDevice,
    /// Memory accessed by both CPU and device
    Bidirectional,
}

/// DMA address type (physical address visible to devices)
pub type DmaAddr = u64;

/// A DMA-coherent memory allocation
#[derive(Debug)]
pub struct DmaAllocation {
    /// Virtual address (CPU accessible)
    pub virt_addr: NonNull<u8>,
    /// Physical/DMA address (device accessible)
    pub dma_addr: DmaAddr,
    /// Size of allocation in bytes
    pub size: usize,
    /// Alignment of allocation
    pub align: usize,
}

impl DmaAllocation {
    /// Get virtual address as pointer
    pub fn as_ptr(&self) -> *mut u8 {
        self.virt_addr.as_ptr()
    }
    
    /// Get virtual address as slice
    pub unsafe fn as_slice(&self) -> &[u8] {
        core::slice::from_raw_parts(self.virt_addr.as_ptr(), self.size)
    }
    
    /// Get virtual address as mutable slice
    pub unsafe fn as_mut_slice(&mut self) -> &mut [u8] {
        core::slice::from_raw_parts_mut(self.virt_addr.as_ptr(), self.size)
    }
    
    /// Zero the allocation
    pub fn zero(&mut self) {
        unsafe {
            core::ptr::write_bytes(self.virt_addr.as_ptr(), 0, self.size);
        }
    }
}

/// DMA pool for allocating fixed-size buffers
pub struct DmaPool {
    /// Name for debugging
    pub name: &'static str,
    /// Size of each allocation
    pub elem_size: usize,
    /// Alignment requirement
    pub align: usize,
    /// Backing memory (if allocated)
    backing: Option<DmaAllocation>,
    /// Free list bitmap
    free_bitmap: u64,
    /// Maximum elements (up to 64)
    max_elems: usize,
}

impl DmaPool {
    /// Create a new DMA pool (not yet allocated)
    pub const fn new(name: &'static str, elem_size: usize, align: usize) -> Self {
        Self {
            name,
            elem_size,
            align,
            backing: None,
            free_bitmap: 0,
            max_elems: 0,
        }
    }
    
    /// Initialize the pool with backing memory
    pub fn init(&mut self, max_elems: usize) -> Result<(), DmaError> {
        if max_elems > 64 {
            return Err(DmaError::InvalidSize);
        }
        
        let total_size = self.elem_size * max_elems;
        
        // Allocate backing memory
        let backing = DmaAllocator::alloc_coherent(total_size, self.align)?;
        
        self.backing = Some(backing);
        self.max_elems = max_elems;
        self.free_bitmap = (1u64 << max_elems) - 1; // All elements free
        
        Ok(())
    }
    
    /// Allocate an element from the pool
    pub fn alloc(&mut self) -> Result<DmaAllocation, DmaError> {
        if self.free_bitmap == 0 {
            return Err(DmaError::OutOfMemory);
        }
        
        let backing = self.backing.as_ref().ok_or(DmaError::NotAllocated)?;
        
        // Find first free element
        let idx = self.free_bitmap.trailing_zeros() as usize;
        if idx >= self.max_elems {
            return Err(DmaError::OutOfMemory);
        }
        
        // Mark as allocated
        self.free_bitmap &= !(1u64 << idx);
        
        // Calculate addresses
        let offset = idx * self.elem_size;
        let virt_addr = unsafe {
            NonNull::new_unchecked(backing.virt_addr.as_ptr().add(offset))
        };
        let dma_addr = backing.dma_addr + offset as u64;
        
        Ok(DmaAllocation {
            virt_addr,
            dma_addr,
            size: self.elem_size,
            align: self.align,
        })
    }
    
    /// Free an element back to the pool
    pub fn free(&mut self, alloc: DmaAllocation) -> Result<(), DmaError> {
        let backing = self.backing.as_ref().ok_or(DmaError::NotAllocated)?;
        
        // Calculate index from address
        let offset = alloc.dma_addr.saturating_sub(backing.dma_addr) as usize;
        if offset % self.elem_size != 0 {
            return Err(DmaError::InvalidAlignment);
        }
        
        let idx = offset / self.elem_size;
        if idx >= self.max_elems {
            return Err(DmaError::InvalidSize);
        }
        
        // Mark as free
        self.free_bitmap |= 1u64 << idx;
        
        Ok(())
    }
    
    /// Get number of free elements
    pub fn free_count(&self) -> usize {
        self.free_bitmap.count_ones() as usize
    }
}

/// Global DMA allocator
pub struct DmaAllocator;

impl DmaAllocator {
    /// Allocate DMA-coherent memory
    /// Returns memory that is both CPU-accessible and device-accessible
    pub fn alloc_coherent(size: usize, align: usize) -> Result<DmaAllocation, DmaError> {
        if size == 0 {
            return Err(DmaError::InvalidSize);
        }
        
        // Round up size to alignment
        let aligned_size = (size + align - 1) & !(align - 1);
        
        // In a real implementation, this would:
        // 1. Allocate physically contiguous memory
        // 2. Map it as uncached/write-combining for coherency
        // 3. Return both virtual and physical addresses
        //
        // For now, use regular allocation (works in QEMU, not on real hardware)
        let layout = core::alloc::Layout::from_size_align(aligned_size, align)
            .map_err(|_| DmaError::InvalidAlignment)?;
        
        let ptr = unsafe { std::alloc::alloc_zeroed(layout) };
        if ptr.is_null() {
            return Err(DmaError::OutOfMemory);
        }
        
        let virt_addr = NonNull::new(ptr).ok_or(DmaError::OutOfMemory)?;
        
        // In identity-mapped memory (common in unikernels), virt == phys
        // Real implementation would use page tables to get physical address
        let dma_addr = ptr as DmaAddr;
        
        Ok(DmaAllocation {
            virt_addr,
            dma_addr,
            size: aligned_size,
            align,
        })
    }
    
    /// Free DMA-coherent memory
    pub fn free_coherent(alloc: DmaAllocation) {
        let layout = core::alloc::Layout::from_size_align(alloc.size, alloc.align)
            .expect("Invalid layout");
        
        unsafe {
            std::alloc::dealloc(alloc.virt_addr.as_ptr(), layout);
        }
    }
    
    /// Map existing buffer for DMA
    /// Returns the DMA address for the buffer
    pub fn map_single(buffer: &[u8], _direction: DmaDirection) -> DmaAddr {
        // In identity-mapped memory, just return the virtual address
        // Real implementation would:
        // 1. Flush/invalidate cache as needed
        // 2. Return physical address
        buffer.as_ptr() as DmaAddr
    }
    
    /// Unmap buffer from DMA
    pub fn unmap_single(_dma_addr: DmaAddr, _size: usize, _direction: DmaDirection) {
        // Real implementation would:
        // 1. Flush/invalidate cache as needed
        // 2. Remove IOMMU mapping if applicable
    }
    
    /// Sync buffer for CPU access (after device wrote)
    pub fn sync_for_cpu(_dma_addr: DmaAddr, _size: usize) {
        // Real implementation would invalidate cache
        core::sync::atomic::fence(core::sync::atomic::Ordering::SeqCst);
    }
    
    /// Sync buffer for device access (after CPU wrote)
    pub fn sync_for_device(_dma_addr: DmaAddr, _size: usize) {
        // Real implementation would flush cache
        core::sync::atomic::fence(core::sync::atomic::Ordering::SeqCst);
    }
}

/// Ring buffer for DMA descriptors (TX/RX queues)
pub struct DmaRingBuffer<const N: usize> {
    /// Descriptor ring memory
    pub descriptors: Option<DmaAllocation>,
    /// Data buffer pool
    pub buffers: Option<DmaAllocation>,
    /// Descriptor size
    pub desc_size: usize,
    /// Buffer size per descriptor
    pub buf_size: usize,
    /// Write pointer (producer)
    pub write_ptr: usize,
    /// Read pointer (consumer)
    pub read_ptr: usize,
}

impl<const N: usize> DmaRingBuffer<N> {
    /// Create new uninitialized ring buffer
    pub const fn new(desc_size: usize, buf_size: usize) -> Self {
        Self {
            descriptors: None,
            buffers: None,
            desc_size,
            buf_size,
            write_ptr: 0,
            read_ptr: 0,
        }
    }
    
    /// Initialize the ring buffer
    pub fn init(&mut self) -> Result<(), DmaError> {
        // Allocate descriptor ring (16-byte aligned for most hardware)
        let desc_alloc = DmaAllocator::alloc_coherent(self.desc_size * N, 16)?;
        
        // Allocate data buffers (page aligned)
        let buf_alloc = DmaAllocator::alloc_coherent(self.buf_size * N, 4096)?;
        
        self.descriptors = Some(desc_alloc);
        self.buffers = Some(buf_alloc);
        
        Ok(())
    }
    
    /// Check if ring is empty
    pub fn is_empty(&self) -> bool {
        self.write_ptr == self.read_ptr
    }
    
    /// Check if ring is full
    pub fn is_full(&self) -> bool {
        (self.write_ptr + 1) % N == self.read_ptr
    }
    
    /// Get number of entries in use
    pub fn len(&self) -> usize {
        if self.write_ptr >= self.read_ptr {
            self.write_ptr - self.read_ptr
        } else {
            N - self.read_ptr + self.write_ptr
        }
    }
    
    /// Get number of free entries
    pub fn free_count(&self) -> usize {
        N - 1 - self.len()
    }
    
    /// Get descriptor DMA address at index
    pub fn desc_dma_addr(&self, idx: usize) -> Option<DmaAddr> {
        self.descriptors.as_ref().map(|d| d.dma_addr + (idx * self.desc_size) as u64)
    }
    
    /// Get buffer DMA address at index
    pub fn buf_dma_addr(&self, idx: usize) -> Option<DmaAddr> {
        self.buffers.as_ref().map(|b| b.dma_addr + (idx * self.buf_size) as u64)
    }
    
    /// Advance write pointer
    pub fn advance_write(&mut self) {
        self.write_ptr = (self.write_ptr + 1) % N;
    }
    
    /// Advance read pointer
    pub fn advance_read(&mut self) {
        self.read_ptr = (self.read_ptr + 1) % N;
    }
}
