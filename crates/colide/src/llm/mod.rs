//! LLM Runtime for Colide OS
//!
//! Provides large language model inference capabilities for bare-metal
//! and unikernel environments. Supports models up to 100GB+ via:
//! - Memory-mapped file loading (mmap)
//! - Layer offloading (GPU/RAM/disk)
//! - Quantized GGUF models (Q4_K_M, Q8_0, etc.)
//!
//! Architecture based on llamafile/llama.cpp patterns.

pub mod gguf;
pub mod inference;
pub mod memory;
pub mod quantization;

use std::path::Path;

/// Model size categories for memory planning
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ModelSize {
    /// Tiny models (< 2B params): ~1-2GB, runs on any hardware
    Tiny,
    /// Small models (2-7B params): ~4-8GB, needs 8GB+ RAM
    Small,
    /// Medium models (7-14B params): ~8-16GB, needs 16GB+ RAM
    Medium,
    /// Large models (14-34B params): ~16-40GB, needs 32GB+ RAM
    Large,
    /// XLarge models (34-72B params): ~40-80GB, needs 64GB+ RAM
    XLarge,
    /// Massive models (72B+ params): ~80-150GB+, needs 128GB+ RAM or offloading
    Massive,
}

impl ModelSize {
    pub fn from_param_count(params_billions: f32) -> Self {
        match params_billions {
            p if p < 2.0 => Self::Tiny,
            p if p < 7.0 => Self::Small,
            p if p < 14.0 => Self::Medium,
            p if p < 34.0 => Self::Large,
            p if p < 72.0 => Self::XLarge,
            _ => Self::Massive,
        }
    }
    
    pub fn min_ram_gb(&self) -> usize {
        match self {
            Self::Tiny => 4,
            Self::Small => 8,
            Self::Medium => 16,
            Self::Large => 32,
            Self::XLarge => 64,
            Self::Massive => 128,
        }
    }
    
    pub fn recommended_quantization(&self) -> &'static str {
        match self {
            Self::Tiny | Self::Small => "Q8_0",      // Quality matters more for small
            Self::Medium | Self::Large => "Q6_K",    // Balance quality/size
            Self::XLarge | Self::Massive => "Q4_K_M", // Size matters more for huge
        }
    }
}

/// Quantization format for GGUF models
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Quantization {
    /// Full precision (FP16) - 2 bytes per weight
    FP16,
    /// 8-bit quantization - 1 byte per weight
    Q8_0,
    /// 6-bit k-quant - ~0.75 bytes per weight
    Q6_K,
    /// 5-bit k-quant medium - ~0.625 bytes per weight
    Q5_K_M,
    /// 4-bit k-quant medium - ~0.5 bytes per weight (recommended)
    Q4_K_M,
    /// 4-bit k-quant small - ~0.5 bytes per weight
    Q4_K_S,
    /// 3-bit k-quant medium - ~0.375 bytes per weight
    Q3_K_M,
    /// 2-bit (1.58-bit) - experimental
    IQ2_XXS,
}

impl Quantization {
    /// Bytes per parameter (approximate)
    pub fn bytes_per_param(&self) -> f32 {
        match self {
            Self::FP16 => 2.0,
            Self::Q8_0 => 1.0,
            Self::Q6_K => 0.75,
            Self::Q5_K_M => 0.625,
            Self::Q4_K_M | Self::Q4_K_S => 0.5,
            Self::Q3_K_M => 0.375,
            Self::IQ2_XXS => 0.25,
        }
    }
    
    /// Estimate model file size for given param count
    pub fn estimate_size_gb(&self, params_billions: f32) -> f32 {
        (params_billions * 1e9 * self.bytes_per_param()) / 1e9
    }
    
    /// Quality score (0-100, subjective)
    pub fn quality_score(&self) -> u8 {
        match self {
            Self::FP16 => 100,
            Self::Q8_0 => 98,
            Self::Q6_K => 95,
            Self::Q5_K_M => 90,
            Self::Q4_K_M => 85,
            Self::Q4_K_S => 82,
            Self::Q3_K_M => 75,
            Self::IQ2_XXS => 60,
        }
    }
}

/// Memory offloading strategy
#[derive(Debug, Clone)]
pub struct OffloadConfig {
    /// Number of layers to keep in GPU VRAM
    pub gpu_layers: u32,
    /// Use memory mapping for model file
    pub use_mmap: bool,
    /// Lock model in RAM (prevent swapping)
    pub mlock: bool,
    /// Split layers across multiple GPUs
    pub tensor_split: Option<Vec<f32>>,
    /// Maximum context size (affects KV cache)
    pub context_size: u32,
    /// Batch size for processing
    pub batch_size: u32,
}

impl Default for OffloadConfig {
    fn default() -> Self {
        Self {
            gpu_layers: 0, // CPU-only by default
            use_mmap: true,
            mlock: false,
            tensor_split: None,
            context_size: 4096,
            batch_size: 512,
        }
    }
}

impl OffloadConfig {
    /// Configure for maximum GPU utilization
    pub fn gpu_max(vram_gb: usize) -> Self {
        Self {
            gpu_layers: 999, // All layers to GPU
            use_mmap: true,
            mlock: false,
            context_size: 8192,
            batch_size: 512,
            tensor_split: None,
        }
    }
    
    /// Configure for CPU-only inference
    pub fn cpu_only(ram_gb: usize) -> Self {
        Self {
            gpu_layers: 0,
            use_mmap: true,
            mlock: ram_gb >= 64, // Lock if we have plenty of RAM
            context_size: 4096,
            batch_size: 256,
            tensor_split: None,
        }
    }
    
    /// Configure for hybrid GPU+RAM+disk
    pub fn hybrid(gpu_layers: u32, context_size: u32) -> Self {
        Self {
            gpu_layers,
            use_mmap: true,
            mlock: false,
            context_size,
            batch_size: 512,
            tensor_split: None,
        }
    }
    
    /// Estimate RAM usage for KV cache
    pub fn kv_cache_size_mb(&self, model_dim: u32, num_layers: u32) -> u32 {
        // KV cache = 2 * context * dim * layers * sizeof(fp16)
        let cache_bytes = 2 * self.context_size * model_dim * num_layers * 2;
        cache_bytes / (1024 * 1024)
    }
}

/// LLM Model configuration
#[derive(Debug, Clone)]
pub struct ModelConfig {
    pub path: String,
    pub quantization: Quantization,
    pub offload: OffloadConfig,
    pub params_billions: f32,
    pub vocab_size: u32,
    pub hidden_dim: u32,
    pub num_layers: u32,
    pub num_heads: u32,
}

impl ModelConfig {
    /// Create config for Qwen 2.5 models
    pub fn qwen25(size: &str, quant: Quantization) -> Option<Self> {
        let (params, hidden, layers, heads) = match size {
            "0.5B" => (0.5, 896, 24, 14),
            "1.5B" => (1.5, 1536, 28, 12),
            "3B" => (3.0, 2048, 36, 16),
            "7B" => (7.0, 3584, 28, 28),
            "14B" => (14.0, 5120, 40, 40),
            "32B" => (32.0, 5120, 64, 40),
            "72B" => (72.0, 8192, 80, 64),
            _ => return None,
        };
        
        Some(Self {
            path: format!("qwen2.5-{}-instruct-{:?}.gguf", size, quant).to_lowercase(),
            quantization: quant,
            offload: OffloadConfig::default(),
            params_billions: params,
            vocab_size: 152064,
            hidden_dim: hidden,
            num_layers: layers,
            num_heads: heads,
        })
    }
    
    /// Estimate total memory requirements
    pub fn estimate_memory_gb(&self) -> f32 {
        let model_size = self.quantization.estimate_size_gb(self.params_billions);
        let kv_cache = self.offload.kv_cache_size_mb(self.hidden_dim, self.num_layers) as f32 / 1024.0;
        let overhead = 0.5; // Misc buffers, etc.
        model_size + kv_cache + overhead
    }
    
    /// Check if model can run on given hardware
    pub fn can_run(&self, ram_gb: usize, vram_gb: usize) -> bool {
        let needed = self.estimate_memory_gb();
        let available = (ram_gb + vram_gb) as f32;
        needed <= available * 0.9 // Leave 10% headroom
    }
}

/// Hardware detection for optimal configuration
#[derive(Debug, Clone)]
pub struct HardwareInfo {
    pub total_ram_gb: usize,
    pub available_ram_gb: usize,
    pub gpu_count: usize,
    pub gpu_vram_gb: Vec<usize>,
    pub cpu_cores: usize,
    pub has_avx2: bool,
    pub has_avx512: bool,
    pub has_neon: bool,
}

impl HardwareInfo {
    /// Detect hardware capabilities (placeholder for bare-metal)
    pub fn detect() -> Self {
        Self {
            total_ram_gb: 16, // Default conservative estimate
            available_ram_gb: 12,
            gpu_count: 0,
            gpu_vram_gb: vec![],
            cpu_cores: 4,
            has_avx2: true,
            has_avx512: false,
            has_neon: false,
        }
    }
    
    /// Total available memory (RAM + VRAM)
    pub fn total_memory_gb(&self) -> usize {
        self.available_ram_gb + self.gpu_vram_gb.iter().sum::<usize>()
    }
    
    /// Recommend optimal model for this hardware
    pub fn recommend_model(&self) -> (ModelSize, Quantization) {
        let total = self.total_memory_gb();
        
        match total {
            0..=4 => (ModelSize::Tiny, Quantization::Q4_K_M),
            5..=8 => (ModelSize::Small, Quantization::Q4_K_M),
            9..=16 => (ModelSize::Small, Quantization::Q6_K),
            17..=32 => (ModelSize::Medium, Quantization::Q4_K_M),
            33..=64 => (ModelSize::Large, Quantization::Q4_K_M),
            65..=128 => (ModelSize::XLarge, Quantization::Q4_K_M),
            _ => (ModelSize::Massive, Quantization::Q4_K_M),
        }
    }
    
    /// Calculate optimal layer split for multi-GPU
    pub fn optimal_tensor_split(&self) -> Option<Vec<f32>> {
        if self.gpu_count < 2 {
            return None;
        }
        
        let total_vram: usize = self.gpu_vram_gb.iter().sum();
        if total_vram == 0 {
            return None;
        }
        
        Some(
            self.gpu_vram_gb
                .iter()
                .map(|&v| v as f32 / total_vram as f32)
                .collect()
        )
    }
}

/// Model size reference table
pub fn model_size_table() -> Vec<(&'static str, f32, f32, f32)> {
    // (model, params_B, Q4_K_M_GB, Q8_0_GB)
    vec![
        ("Qwen2.5-0.5B", 0.5, 0.4, 0.6),
        ("Qwen2.5-1.5B", 1.5, 1.1, 1.8),
        ("Qwen2.5-3B", 3.0, 2.0, 3.5),
        ("Qwen2.5-7B", 7.0, 4.7, 8.1),
        ("Qwen2.5-14B", 14.0, 9.0, 16.0),
        ("Qwen2.5-32B", 32.0, 20.0, 36.0),
        ("Qwen2.5-72B", 72.0, 43.0, 80.0),
        ("Llama-3.1-8B", 8.0, 5.0, 9.0),
        ("Llama-3.1-70B", 70.0, 42.0, 78.0),
        ("Llama-3.1-405B", 405.0, 230.0, 450.0),
        ("DeepSeek-R1-671B", 671.0, 400.0, 750.0),
    ]
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_quantization_sizes() {
        let q4 = Quantization::Q4_K_M;
        let q8 = Quantization::Q8_0;
        
        // 7B model sizes
        assert!((q4.estimate_size_gb(7.0) - 3.5).abs() < 0.5);
        assert!((q8.estimate_size_gb(7.0) - 7.0).abs() < 0.5);
    }

    #[test]
    fn test_model_config_qwen() {
        let config = ModelConfig::qwen25("7B", Quantization::Q4_K_M).unwrap();
        assert_eq!(config.num_layers, 28);
        assert!(config.estimate_memory_gb() < 10.0);
    }

    #[test]
    fn test_hardware_recommend() {
        let hw = HardwareInfo {
            total_ram_gb: 32,
            available_ram_gb: 28,
            gpu_count: 1,
            gpu_vram_gb: vec![8],
            cpu_cores: 8,
            has_avx2: true,
            has_avx512: false,
            has_neon: false,
        };
        
        let (size, quant) = hw.recommend_model();
        assert!(matches!(size, ModelSize::Large | ModelSize::Medium));
    }

    #[test]
    fn test_offload_config() {
        let config = OffloadConfig::hybrid(32, 8192);
        assert_eq!(config.gpu_layers, 32);
        assert!(config.use_mmap);
    }
}
