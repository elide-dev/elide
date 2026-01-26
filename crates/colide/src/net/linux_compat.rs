// Linux Kernel Compatibility Shim for Colide OS
// This module provides Linux kernel-like primitives needed for WiFi driver porting
// Inspired by Fuchsia's iwlwifi compatibility layer

use core::sync::atomic::{AtomicBool, AtomicU32, AtomicUsize, Ordering};
use core::cell::UnsafeCell;
use core::marker::PhantomData;

#[cfg(feature = "std")]
use std::collections::VecDeque;
#[cfg(feature = "std")]
use std::sync::{Mutex, Condvar};
#[cfg(feature = "std")]
use std::time::{Duration, Instant};

/// Linux-style error codes
pub mod errno {
    pub const ENOMEM: i32 = -12;
    pub const EBUSY: i32 = -16;
    pub const ENODEV: i32 = -19;
    pub const EINVAL: i32 = -22;
    pub const ENOSPC: i32 = -28;
    pub const ETIMEDOUT: i32 = -110;
    pub const ENOENT: i32 = -2;
    pub const EIO: i32 = -5;
    pub const EAGAIN: i32 = -11;
    pub const EOPNOTSUPP: i32 = -95;
}

/// Linux-style GFP flags for memory allocation
pub mod gfp {
    pub const GFP_KERNEL: u32 = 0x0;
    pub const GFP_ATOMIC: u32 = 0x1;
    pub const GFP_DMA: u32 = 0x2;
    pub const __GFP_ZERO: u32 = 0x100;
}

/// Simple spinlock implementation using atomics
pub struct SpinLock<T> {
    locked: AtomicBool,
    data: UnsafeCell<T>,
}

unsafe impl<T: Send> Send for SpinLock<T> {}
unsafe impl<T: Send> Sync for SpinLock<T> {}

impl<T> SpinLock<T> {
    pub const fn new(data: T) -> Self {
        Self {
            locked: AtomicBool::new(false),
            data: UnsafeCell::new(data),
        }
    }
    
    pub fn lock(&self) -> SpinLockGuard<'_, T> {
        while self.locked.compare_exchange_weak(
            false, true, Ordering::Acquire, Ordering::Relaxed
        ).is_err() {
            core::hint::spin_loop();
        }
        SpinLockGuard { lock: self }
    }
    
    pub fn try_lock(&self) -> Option<SpinLockGuard<'_, T>> {
        if self.locked.compare_exchange(
            false, true, Ordering::Acquire, Ordering::Relaxed
        ).is_ok() {
            Some(SpinLockGuard { lock: self })
        } else {
            None
        }
    }
}

pub struct SpinLockGuard<'a, T> {
    lock: &'a SpinLock<T>,
}

impl<'a, T> core::ops::Deref for SpinLockGuard<'a, T> {
    type Target = T;
    fn deref(&self) -> &T {
        unsafe { &*self.lock.data.get() }
    }
}

impl<'a, T> core::ops::DerefMut for SpinLockGuard<'a, T> {
    fn deref_mut(&mut self) -> &mut T {
        unsafe { &mut *self.lock.data.get() }
    }
}

impl<'a, T> Drop for SpinLockGuard<'a, T> {
    fn drop(&mut self) {
        self.lock.locked.store(false, Ordering::Release);
    }
}

/// Linux completion primitive
#[cfg(feature = "std")]
pub struct Completion {
    done: AtomicBool,
    mutex: Mutex<()>,
    condvar: Condvar,
}

#[cfg(feature = "std")]
impl Completion {
    pub fn new() -> Self {
        Self {
            done: AtomicBool::new(false),
            mutex: Mutex::new(()),
            condvar: Condvar::new(),
        }
    }
    
    pub fn complete(&self) {
        self.done.store(true, Ordering::Release);
        self.condvar.notify_all();
    }
    
    pub fn wait(&self) {
        let guard = self.mutex.lock().unwrap();
        let _guard = self.condvar.wait_while(guard, |_| {
            !self.done.load(Ordering::Acquire)
        }).unwrap();
    }
    
    pub fn wait_timeout(&self, timeout_ms: u64) -> bool {
        let guard = self.mutex.lock().unwrap();
        let (_, result) = self.condvar.wait_timeout_while(
            guard,
            Duration::from_millis(timeout_ms),
            |_| !self.done.load(Ordering::Acquire)
        ).unwrap();
        !result.timed_out()
    }
    
    pub fn reset(&self) {
        self.done.store(false, Ordering::Release);
    }
}

/// Linux-style reference counted object
pub struct Kref {
    refcount: AtomicU32,
}

impl Kref {
    pub const fn new() -> Self {
        Self { refcount: AtomicU32::new(1) }
    }
    
    pub fn get(&self) {
        self.refcount.fetch_add(1, Ordering::Relaxed);
    }
    
    pub fn put<F>(&self, release: F) -> bool 
    where F: FnOnce()
    {
        if self.refcount.fetch_sub(1, Ordering::Release) == 1 {
            core::sync::atomic::fence(Ordering::Acquire);
            release();
            true
        } else {
            false
        }
    }
    
    pub fn count(&self) -> u32 {
        self.refcount.load(Ordering::Relaxed)
    }
}

/// Linux sk_buff equivalent - network packet buffer
pub struct SkBuff {
    pub data: Vec<u8>,
    pub head: usize,
    pub data_ptr: usize,
    pub tail: usize,
    pub end: usize,
    pub len: usize,
    pub protocol: u16,
    pub priority: u32,
    pub cb: [u8; 48], // Control buffer
}

impl SkBuff {
    pub fn alloc(size: usize) -> Option<Box<Self>> {
        let mut skb = Box::new(Self {
            data: vec![0u8; size],
            head: 0,
            data_ptr: 0,
            tail: 0,
            end: size,
            len: 0,
            protocol: 0,
            priority: 0,
            cb: [0u8; 48],
        });
        Some(skb)
    }
    
    pub fn reserve(&mut self, len: usize) {
        self.data_ptr += len;
        self.tail += len;
    }
    
    pub fn put(&mut self, len: usize) -> &mut [u8] {
        let start = self.tail;
        self.tail += len;
        self.len += len;
        &mut self.data[start..self.tail]
    }
    
    pub fn push(&mut self, len: usize) -> &mut [u8] {
        self.data_ptr -= len;
        self.len += len;
        &mut self.data[self.data_ptr..self.data_ptr + len]
    }
    
    pub fn pull(&mut self, len: usize) -> Option<&[u8]> {
        if len > self.len {
            return None;
        }
        let start = self.data_ptr;
        self.data_ptr += len;
        self.len -= len;
        Some(&self.data[start..start + len])
    }
    
    pub fn data_slice(&self) -> &[u8] {
        &self.data[self.data_ptr..self.tail]
    }
    
    pub fn headroom(&self) -> usize {
        self.data_ptr - self.head
    }
    
    pub fn tailroom(&self) -> usize {
        self.end - self.tail
    }
}

/// Linux workqueue simulation
#[cfg(feature = "std")]
pub struct Workqueue {
    name: String,
    queue: Mutex<VecDeque<Box<dyn FnOnce() + Send>>>,
    running: AtomicBool,
}

#[cfg(feature = "std")]
impl Workqueue {
    pub fn create(name: &str) -> Self {
        Self {
            name: name.to_string(),
            queue: Mutex::new(VecDeque::new()),
            running: AtomicBool::new(true),
        }
    }
    
    pub fn queue_work<F>(&self, work: F) 
    where F: FnOnce() + Send + 'static
    {
        let mut queue = self.queue.lock().unwrap();
        queue.push_back(Box::new(work));
    }
    
    pub fn flush(&self) {
        loop {
            let work = {
                let mut queue = self.queue.lock().unwrap();
                queue.pop_front()
            };
            match work {
                Some(f) => f(),
                None => break,
            }
        }
    }
    
    pub fn destroy(&self) {
        self.running.store(false, Ordering::Release);
        self.flush();
    }
}

/// DMA address type
pub type DmaAddr = u64;

/// Scatter-gather entry for DMA
#[derive(Clone, Copy)]
pub struct ScatterlistEntry {
    pub page_link: usize,
    pub offset: u32,
    pub length: u32,
    pub dma_address: DmaAddr,
}

/// IEEE 802.11 frame types
pub mod ieee80211 {
    pub const FTYPE_MGMT: u16 = 0x0000;
    pub const FTYPE_CTL: u16 = 0x0004;
    pub const FTYPE_DATA: u16 = 0x0008;
    
    pub const STYPE_ASSOC_REQ: u16 = 0x0000;
    pub const STYPE_ASSOC_RESP: u16 = 0x0010;
    pub const STYPE_PROBE_REQ: u16 = 0x0040;
    pub const STYPE_PROBE_RESP: u16 = 0x0050;
    pub const STYPE_BEACON: u16 = 0x0080;
    pub const STYPE_DISASSOC: u16 = 0x00A0;
    pub const STYPE_AUTH: u16 = 0x00B0;
    pub const STYPE_DEAUTH: u16 = 0x00C0;
    pub const STYPE_ACTION: u16 = 0x00D0;
    
    #[repr(C, packed)]
    pub struct FrameHeader {
        pub frame_control: u16,
        pub duration_id: u16,
        pub addr1: [u8; 6],
        pub addr2: [u8; 6],
        pub addr3: [u8; 6],
        pub seq_ctrl: u16,
    }
    
    #[repr(C, packed)]
    pub struct MgmtHeader {
        pub frame_control: u16,
        pub duration: u16,
        pub da: [u8; 6],
        pub sa: [u8; 6],
        pub bssid: [u8; 6],
        pub seq_ctrl: u16,
    }
}

/// Jiffies simulation (time in ticks)
#[cfg(feature = "std")]
pub fn jiffies() -> u64 {
    static START: std::sync::OnceLock<Instant> = std::sync::OnceLock::new();
    let start = START.get_or_init(Instant::now);
    start.elapsed().as_millis() as u64
}

pub fn msecs_to_jiffies(msecs: u64) -> u64 {
    msecs // 1 jiffy = 1ms in our implementation
}

pub fn jiffies_to_msecs(jiffies: u64) -> u64 {
    jiffies
}

/// Memory allocation helpers
pub fn kzalloc(size: usize, _flags: u32) -> Option<Vec<u8>> {
    Some(vec![0u8; size])
}

pub fn kmalloc(size: usize, _flags: u32) -> Option<Vec<u8>> {
    Some(Vec::with_capacity(size))
}

/// Bit manipulation helpers
pub fn set_bit(nr: usize, addr: &AtomicUsize) {
    addr.fetch_or(1 << nr, Ordering::SeqCst);
}

pub fn clear_bit(nr: usize, addr: &AtomicUsize) {
    addr.fetch_and(!(1 << nr), Ordering::SeqCst);
}

pub fn test_bit(nr: usize, addr: &AtomicUsize) -> bool {
    (addr.load(Ordering::SeqCst) & (1 << nr)) != 0
}

pub fn test_and_set_bit(nr: usize, addr: &AtomicUsize) -> bool {
    let old = addr.fetch_or(1 << nr, Ordering::SeqCst);
    (old & (1 << nr)) != 0
}

pub fn test_and_clear_bit(nr: usize, addr: &AtomicUsize) -> bool {
    let old = addr.fetch_and(!(1 << nr), Ordering::SeqCst);
    (old & (1 << nr)) != 0
}

/// Logging macros (would map to kernel printk)
#[macro_export]
macro_rules! pr_info {
    ($($arg:tt)*) => {
        #[cfg(feature = "std")]
        eprintln!("[INFO] {}", format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! pr_warn {
    ($($arg:tt)*) => {
        #[cfg(feature = "std")]
        eprintln!("[WARN] {}", format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! pr_err {
    ($($arg:tt)*) => {
        #[cfg(feature = "std")]
        eprintln!("[ERROR] {}", format_args!($($arg)*));
    };
}

#[macro_export]
macro_rules! pr_debug {
    ($($arg:tt)*) => {
        #[cfg(all(feature = "std", debug_assertions))]
        eprintln!("[DEBUG] {}", format_args!($($arg)*));
    };
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_spinlock() {
        let lock = SpinLock::new(42);
        {
            let mut guard = lock.lock();
            assert_eq!(*guard, 42);
            *guard = 100;
        }
        {
            let guard = lock.lock();
            assert_eq!(*guard, 100);
        }
    }
    
    #[test]
    fn test_skbuff() {
        let mut skb = SkBuff::alloc(1500).unwrap();
        skb.reserve(100);
        assert_eq!(skb.headroom(), 100);
        
        let data = skb.put(50);
        data.copy_from_slice(&[0xAA; 50]);
        assert_eq!(skb.len, 50);
        
        let header = skb.push(14);
        header.copy_from_slice(&[0xBB; 14]);
        assert_eq!(skb.len, 64);
    }
    
    #[test]
    fn test_kref() {
        let kref = Kref::new();
        assert_eq!(kref.count(), 1);
        
        kref.get();
        assert_eq!(kref.count(), 2);
        
        let released = kref.put(|| {});
        assert!(!released);
        assert_eq!(kref.count(), 1);
        
        let released = kref.put(|| {});
        assert!(released);
    }
}
