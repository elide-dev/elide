//! Memory Management for LLM Inference
//!
//! Handles memory-mapped file loading, layer offloading, and
//! memory allocation strategies for running large models.

use std::ptr::NonNull;

/// Memory mapping strategy
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MmapStrategy {
    /// Load entire model into RAM
    Eager,
    /// Memory-map file, load on demand
    Lazy,
    /// Memory-map with prefetch hints
    Prefetch,
    /// Lock pages in RAM (no swap)
    Locked,
}

/// Memory region type
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum MemoryType {
    /// System RAM
    Ram,
    /// GPU VRAM
    Vram,
    /// Disk/SSD (via mmap)
    Disk,
    /// Unified memory (shared RAM/VRAM)
    Unified,
}

/// Memory allocation for model weights
#[derive(Debug)]
pub struct WeightBuffer {
    pub ptr: NonNull<u8>,
    pub size: usize,
    pub memory_type: MemoryType,
    pub layer_start: u32,
    pub layer_end: u32,
}

impl WeightBuffer {
    /// Create a new weight buffer
    pub fn new(size: usize, memory_type: MemoryType) -> Option<Self> {
        if size == 0 {
            return None;
        }
        
        // Placeholder - actual allocation depends on memory type
        let layout = std::alloc::Layout::from_size_align(size, 64).ok()?;
        let ptr = unsafe { std::alloc::alloc(layout) };
        let ptr = NonNull::new(ptr)?;
        
        Some(Self {
            ptr,
            size,
            memory_type,
            layer_start: 0,
            layer_end: 0,
        })
    }
    
    /// Get slice of buffer
    pub fn as_slice(&self) -> &[u8] {
        unsafe { std::slice::from_raw_parts(self.ptr.as_ptr(), self.size) }
    }
    
    /// Get mutable slice
    pub fn as_slice_mut(&mut self) -> &mut [u8] {
        unsafe { std::slice::from_raw_parts_mut(self.ptr.as_ptr(), self.size) }
    }
}

impl Drop for WeightBuffer {
    fn drop(&mut self) {
        if self.memory_type == MemoryType::Ram {
            let layout = std::alloc::Layout::from_size_align(self.size, 64)
                .expect("Invalid layout");
            unsafe {
                std::alloc::dealloc(self.ptr.as_ptr(), layout);
            }
        }
        // VRAM/Unified handled by GPU runtime
    }
}

/// KV Cache for attention
#[derive(Debug)]
pub struct KvCache {
    /// Key cache buffer
    pub k: Vec<f32>,
    /// Value cache buffer
    pub v: Vec<f32>,
    /// Current sequence length
    pub seq_len: usize,
    /// Maximum context length
    pub max_len: usize,
    /// Number of layers
    pub n_layers: usize,
    /// Number of KV heads
    pub n_kv_heads: usize,
    /// Head dimension
    pub head_dim: usize,
}

impl KvCache {
    pub fn new(max_len: usize, n_layers: usize, n_kv_heads: usize, head_dim: usize) -> Self {
        let cache_size = max_len * n_layers * n_kv_heads * head_dim;
        Self {
            k: vec![0.0; cache_size],
            v: vec![0.0; cache_size],
            seq_len: 0,
            max_len,
            n_layers,
            n_kv_heads,
            head_dim,
        }
    }
    
    /// Size in bytes
    pub fn size_bytes(&self) -> usize {
        (self.k.len() + self.v.len()) * std::mem::size_of::<f32>()
    }
    
    /// Size in megabytes
    pub fn size_mb(&self) -> f32 {
        self.size_bytes() as f32 / (1024.0 * 1024.0)
    }
    
    /// Reset cache for new sequence
    pub fn reset(&mut self) {
        self.seq_len = 0;
    }
    
    /// Trim cache to shorter length
    pub fn trim(&mut self, new_len: usize) {
        if new_len < self.seq_len {
            self.seq_len = new_len;
        }
    }
    
    /// Get K slice for layer
    pub fn k_layer(&self, layer: usize) -> &[f32] {
        let layer_size = self.max_len * self.n_kv_heads * self.head_dim;
        let start = layer * layer_size;
        &self.k[start..start + self.seq_len * self.n_kv_heads * self.head_dim]
    }
    
    /// Get V slice for layer
    pub fn v_layer(&self, layer: usize) -> &[f32] {
        let layer_size = self.max_len * self.n_kv_heads * self.head_dim;
        let start = layer * layer_size;
        &self.v[start..start + self.seq_len * self.n_kv_heads * self.head_dim]
    }
}

/// Memory manager for LLM inference
#[derive(Debug)]
pub struct MemoryManager {
    /// Total available RAM in bytes
    pub ram_total: usize,
    /// Available RAM in bytes
    pub ram_available: usize,
    /// Total VRAM in bytes (0 if no GPU)
    pub vram_total: usize,
    /// Available VRAM in bytes
    pub vram_available: usize,
    /// Weight buffers
    pub weight_buffers: Vec<WeightBuffer>,
    /// Use memory mapping
    pub use_mmap: bool,
    /// Mmap strategy
    pub mmap_strategy: MmapStrategy,
}

impl MemoryManager {
    pub fn new(ram_gb: usize, vram_gb: usize) -> Self {
        Self {
            ram_total: ram_gb * 1024 * 1024 * 1024,
            ram_available: ram_gb * 1024 * 1024 * 1024,
            vram_total: vram_gb * 1024 * 1024 * 1024,
            vram_available: vram_gb * 1024 * 1024 * 1024,
            weight_buffers: Vec::new(),
            use_mmap: true,
            mmap_strategy: MmapStrategy::Lazy,
        }
    }
    
    /// Check if model fits in available memory
    pub fn can_fit(&self, model_size: usize, kv_cache_size: usize) -> bool {
        let total_needed = model_size + kv_cache_size;
        let total_available = self.ram_available + self.vram_available;
        total_needed < total_available
    }
    
    /// Plan layer distribution across memory types
    pub fn plan_layers(&self, n_layers: u32, layer_size: usize) -> Vec<(u32, u32, MemoryType)> {
        let mut result = Vec::new();
        let mut remaining_vram = self.vram_available;
        let mut remaining_ram = self.ram_available;
        let mut current_layer = 0u32;
        
        // Try to fit as many layers in VRAM as possible
        if remaining_vram > 0 {
            let vram_layers = (remaining_vram / layer_size).min(n_layers as usize) as u32;
            if vram_layers > 0 {
                result.push((0, vram_layers - 1, MemoryType::Vram));
                current_layer = vram_layers;
                remaining_vram -= vram_layers as usize * layer_size;
            }
        }
        
        // Put remaining layers in RAM
        if current_layer < n_layers {
            let ram_layers = n_layers - current_layer;
            if ram_layers as usize * layer_size <= remaining_ram {
                result.push((current_layer, n_layers - 1, MemoryType::Ram));
            } else {
                // Need disk offloading
                let ram_can_fit = (remaining_ram / layer_size) as u32;
                if ram_can_fit > 0 {
                    result.push((current_layer, current_layer + ram_can_fit - 1, MemoryType::Ram));
                    current_layer += ram_can_fit;
                }
                if current_layer < n_layers {
                    result.push((current_layer, n_layers - 1, MemoryType::Disk));
                }
            }
        }
        
        result
    }
    
    /// Allocate buffer for layers
    pub fn allocate_layers(
        &mut self,
        layer_start: u32,
        layer_end: u32,
        layer_size: usize,
        memory_type: MemoryType,
    ) -> Option<usize> {
        let n_layers = (layer_end - layer_start + 1) as usize;
        let total_size = n_layers * layer_size;
        
        let buffer = WeightBuffer {
            ptr: NonNull::new(unsafe {
                let layout = std::alloc::Layout::from_size_align(total_size, 64).ok()?;
                std::alloc::alloc(layout)
            })?,
            size: total_size,
            memory_type,
            layer_start,
            layer_end,
        };
        
        match memory_type {
            MemoryType::Ram => self.ram_available -= total_size,
            MemoryType::Vram => self.vram_available -= total_size,
            _ => {}
        }
        
        let index = self.weight_buffers.len();
        self.weight_buffers.push(buffer);
        Some(index)
    }
    
    /// Get buffer containing layer
    pub fn buffer_for_layer(&self, layer: u32) -> Option<&WeightBuffer> {
        self.weight_buffers.iter().find(|b| {
            layer >= b.layer_start && layer <= b.layer_end
        })
    }
    
    /// Free all buffers
    pub fn free_all(&mut self) {
        for buffer in self.weight_buffers.drain(..) {
            match buffer.memory_type {
                MemoryType::Ram => self.ram_available += buffer.size,
                MemoryType::Vram => self.vram_available += buffer.size,
                _ => {}
            }
            // Buffer dropped, memory freed
        }
    }
}

/// Memory requirements calculator
pub fn calculate_requirements(
    params_billions: f32,
    quantization_bits: f32,
    context_length: u32,
    batch_size: u32,
    hidden_dim: u32,
    n_layers: u32,
    n_heads: u32,
) -> MemoryRequirements {
    let params = (params_billions * 1e9) as u64;
    
    // Model weights
    let bytes_per_param = quantization_bits / 8.0;
    let model_weights = (params as f64 * bytes_per_param as f64) as u64;
    
    // KV cache: 2 * batch * ctx * layers * heads * head_dim * sizeof(f16)
    let head_dim = hidden_dim / n_heads;
    let kv_cache = 2 * batch_size as u64 * context_length as u64 * 
                   n_layers as u64 * n_heads as u64 * head_dim as u64 * 2;
    
    // Activation memory: ~2x hidden_dim * batch * context
    let activations = 2 * hidden_dim as u64 * batch_size as u64 * context_length as u64 * 4;
    
    // Overhead: scratch buffers, etc.
    let overhead = model_weights / 10; // ~10%
    
    MemoryRequirements {
        model_weights,
        kv_cache,
        activations,
        overhead,
        total: model_weights + kv_cache + activations + overhead,
    }
}

/// Memory requirements breakdown
#[derive(Debug, Clone)]
pub struct MemoryRequirements {
    pub model_weights: u64,
    pub kv_cache: u64,
    pub activations: u64,
    pub overhead: u64,
    pub total: u64,
}

impl MemoryRequirements {
    pub fn total_gb(&self) -> f32 {
        self.total as f32 / 1e9
    }
    
    pub fn weights_gb(&self) -> f32 {
        self.model_weights as f32 / 1e9
    }
    
    pub fn kv_cache_gb(&self) -> f32 {
        self.kv_cache as f32 / 1e9
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_kv_cache_size() {
        // Qwen 7B: 28 layers, 28 KV heads, 128 head dim, 4K context
        let cache = KvCache::new(4096, 28, 28, 128);
        let size_mb = cache.size_mb();
        // Should be around 3-4 GB for 4K context
        assert!(size_mb > 1000.0 && size_mb < 5000.0);
    }

    #[test]
    fn test_memory_requirements() {
        // Qwen 7B Q4_K_M
        let req = calculate_requirements(
            7.0,    // params
            4.0,    // bits
            4096,   // context
            1,      // batch
            3584,   // hidden
            28,     // layers
            28,     // heads
        );
        
        // Model should be ~3.5GB, total ~5-6GB
        assert!(req.weights_gb() > 3.0 && req.weights_gb() < 5.0);
        assert!(req.total_gb() > 4.0 && req.total_gb() < 10.0);
    }

    #[test]
    fn test_layer_planning() {
        let manager = MemoryManager::new(16, 8); // 16GB RAM, 8GB VRAM
        let plan = manager.plan_layers(32, 250_000_000); // 32 layers, 250MB each
        
        assert!(!plan.is_empty());
        // Should prioritize VRAM
        assert_eq!(plan[0].2, MemoryType::Vram);
    }
}
