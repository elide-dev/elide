//! Hardware abstraction layer for bare-metal operation
//!
//! This module provides direct hardware access when running on bare metal
//! (Cosmopolitan IsMetal mode). On hosted systems, these are no-ops.

/// Port I/O operations for x86
#[cfg(all(target_arch = "x86_64", feature = "bare-metal"))]
pub mod port {
    use x86::io::{inb, outb};
    
    /// PS/2 keyboard data port
    pub const PS2_DATA: u16 = 0x60;
    /// PS/2 status/command port
    pub const PS2_STATUS: u16 = 0x64;
    
    /// Read from PS/2 data port
    #[inline]
    pub unsafe fn ps2_read_data() -> u8 {
        inb(PS2_DATA)
    }
    
    /// Read PS/2 status
    #[inline]
    pub unsafe fn ps2_read_status() -> u8 {
        inb(PS2_STATUS)
    }
    
    /// Write to PS/2 command port
    #[inline]
    pub unsafe fn ps2_write_cmd(cmd: u8) {
        outb(PS2_STATUS, cmd);
    }
    
    /// Write to PS/2 data port
    #[inline]
    pub unsafe fn ps2_write_data(data: u8) {
        outb(PS2_DATA, data);
    }
    
    /// Check if PS/2 output buffer is full (data available)
    #[inline]
    pub unsafe fn ps2_output_full() -> bool {
        (ps2_read_status() & 0x01) != 0
    }
    
    /// Check if PS/2 input buffer is full (can't write)
    #[inline]
    pub unsafe fn ps2_input_full() -> bool {
        (ps2_read_status() & 0x02) != 0
    }
    
    /// Wait for PS/2 output buffer
    pub unsafe fn ps2_wait_output() {
        for _ in 0..100000 {
            if ps2_output_full() {
                return;
            }
        }
    }
    
    /// Wait for PS/2 input buffer to clear
    pub unsafe fn ps2_wait_input() {
        for _ in 0..100000 {
            if !ps2_input_full() {
                return;
            }
        }
    }
}

/// Stub implementations for non-bare-metal builds
#[cfg(not(all(target_arch = "x86_64", feature = "bare-metal")))]
pub mod port {
    pub const PS2_DATA: u16 = 0x60;
    pub const PS2_STATUS: u16 = 0x64;
    
    #[inline]
    pub unsafe fn ps2_read_data() -> u8 { 0 }
    #[inline]
    pub unsafe fn ps2_read_status() -> u8 { 0 }
    #[inline]
    pub unsafe fn ps2_write_cmd(_cmd: u8) {}
    #[inline]
    pub unsafe fn ps2_write_data(_data: u8) {}
    #[inline]
    pub unsafe fn ps2_output_full() -> bool { false }
    #[inline]
    pub unsafe fn ps2_input_full() -> bool { false }
    pub unsafe fn ps2_wait_output() {}
    pub unsafe fn ps2_wait_input() {}
}

/// VESA framebuffer access
pub mod vesa {
    use core::ptr;
    
    /// Standard VGA text mode buffer address
    pub const VGA_TEXT_BUFFER: usize = 0xB8000;
    
    /// VESA linear framebuffer (set by bootloader)
    static mut FRAMEBUFFER_ADDR: usize = 0;
    static mut FRAMEBUFFER_WIDTH: u32 = 0;
    static mut FRAMEBUFFER_HEIGHT: u32 = 0;
    static mut FRAMEBUFFER_PITCH: u32 = 0;
    
    /// Initialize VESA framebuffer
    /// 
    /// # Safety
    /// Must be called with valid framebuffer address from bootloader
    pub unsafe fn init(addr: usize, width: u32, height: u32, pitch: u32) {
        FRAMEBUFFER_ADDR = addr;
        FRAMEBUFFER_WIDTH = width;
        FRAMEBUFFER_HEIGHT = height;
        FRAMEBUFFER_PITCH = pitch;
    }
    
    /// Get framebuffer dimensions
    pub fn dimensions() -> (u32, u32) {
        unsafe { (FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT) }
    }
    
    /// Write pixel to framebuffer (32-bit ARGB)
    /// 
    /// # Safety
    /// Caller must ensure framebuffer is initialized
    #[inline]
    pub unsafe fn write_pixel(x: u32, y: u32, color: u32) {
        if FRAMEBUFFER_ADDR == 0 {
            return;
        }
        if x >= FRAMEBUFFER_WIDTH || y >= FRAMEBUFFER_HEIGHT {
            return;
        }
        let offset = (y * FRAMEBUFFER_PITCH + x * 4) as usize;
        let ptr = (FRAMEBUFFER_ADDR + offset) as *mut u32;
        ptr::write_volatile(ptr, color);
    }
    
    /// Fill rectangle with color
    /// 
    /// # Safety
    /// Caller must ensure framebuffer is initialized
    pub unsafe fn fill_rect(x: u32, y: u32, w: u32, h: u32, color: u32) {
        for py in y..y.saturating_add(h).min(FRAMEBUFFER_HEIGHT) {
            for px in x..x.saturating_add(w).min(FRAMEBUFFER_WIDTH) {
                write_pixel(px, py, color);
            }
        }
    }
    
    /// Clear screen with color
    /// 
    /// # Safety
    /// Caller must ensure framebuffer is initialized
    pub unsafe fn clear(color: u32) {
        fill_rect(0, 0, FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, color);
    }
}

/// Detect if running on bare metal
/// 
/// This checks for Cosmopolitan's IsMetal() equivalent
pub fn is_metal() -> bool {
    // In a real implementation, this would check Cosmopolitan's _IsMetal global
    // For now, check if we're running without a host OS
    #[cfg(feature = "bare-metal")]
    {
        // Check for common bare-metal indicators
        // This is a simplified check - real detection would use Cosmopolitan
        false // Default to false until properly integrated with Cosmo
    }
    #[cfg(not(feature = "bare-metal"))]
    {
        false
    }
}
