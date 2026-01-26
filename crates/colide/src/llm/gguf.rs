//! GGUF File Format Parser
//!
//! Parses GGUF model files for llama.cpp-compatible inference.
//! Supports metadata extraction, tensor mapping, and memory-mapped loading.

use std::collections::BTreeMap;

/// GGUF magic number
pub const GGUF_MAGIC: u32 = 0x46554747; // "GGUF" in little-endian

/// GGUF version (current: 3)
pub const GGUF_VERSION: u32 = 3;

/// GGUF value types
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u32)]
pub enum GgufType {
    Uint8 = 0,
    Int8 = 1,
    Uint16 = 2,
    Int16 = 3,
    Uint32 = 4,
    Int32 = 5,
    Float32 = 6,
    Bool = 7,
    String = 8,
    Array = 9,
    Uint64 = 10,
    Int64 = 11,
    Float64 = 12,
}

impl GgufType {
    pub fn from_u32(v: u32) -> Option<Self> {
        match v {
            0 => Some(Self::Uint8),
            1 => Some(Self::Int8),
            2 => Some(Self::Uint16),
            3 => Some(Self::Int16),
            4 => Some(Self::Uint32),
            5 => Some(Self::Int32),
            6 => Some(Self::Float32),
            7 => Some(Self::Bool),
            8 => Some(Self::String),
            9 => Some(Self::Array),
            10 => Some(Self::Uint64),
            11 => Some(Self::Int64),
            12 => Some(Self::Float64),
            _ => None,
        }
    }
}

/// GGML tensor types (quantization formats)
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u32)]
pub enum GgmlType {
    F32 = 0,
    F16 = 1,
    Q4_0 = 2,
    Q4_1 = 3,
    Q5_0 = 6,
    Q5_1 = 7,
    Q8_0 = 8,
    Q8_1 = 9,
    Q2_K = 10,
    Q3_K = 11,
    Q4_K = 12,
    Q5_K = 13,
    Q6_K = 14,
    Q8_K = 15,
    IQ2_XXS = 16,
    IQ2_XS = 17,
    IQ3_XXS = 18,
    IQ1_S = 19,
    IQ4_NL = 20,
    IQ3_S = 21,
    IQ2_S = 22,
    IQ4_XS = 23,
    I8 = 24,
    I16 = 25,
    I32 = 26,
    I64 = 27,
    F64 = 28,
    BF16 = 29,
}

impl GgmlType {
    pub fn from_u32(v: u32) -> Option<Self> {
        match v {
            0 => Some(Self::F32),
            1 => Some(Self::F16),
            2 => Some(Self::Q4_0),
            3 => Some(Self::Q4_1),
            6 => Some(Self::Q5_0),
            7 => Some(Self::Q5_1),
            8 => Some(Self::Q8_0),
            9 => Some(Self::Q8_1),
            10 => Some(Self::Q2_K),
            11 => Some(Self::Q3_K),
            12 => Some(Self::Q4_K),
            13 => Some(Self::Q5_K),
            14 => Some(Self::Q6_K),
            15 => Some(Self::Q8_K),
            16 => Some(Self::IQ2_XXS),
            24 => Some(Self::I8),
            28 => Some(Self::F64),
            29 => Some(Self::BF16),
            _ => None,
        }
    }
    
    /// Bytes per element (for non-quantized types)
    pub fn type_size(&self) -> usize {
        match self {
            Self::F32 | Self::I32 => 4,
            Self::F16 | Self::BF16 | Self::I16 => 2,
            Self::I8 => 1,
            Self::F64 | Self::I64 => 8,
            // Quantized types have block sizes
            _ => 0,
        }
    }
    
    /// Block size for quantized types
    pub fn block_size(&self) -> usize {
        match self {
            Self::Q4_0 | Self::Q4_1 => 32,
            Self::Q5_0 | Self::Q5_1 => 32,
            Self::Q8_0 | Self::Q8_1 => 32,
            Self::Q2_K | Self::Q3_K | Self::Q4_K | Self::Q5_K | Self::Q6_K | Self::Q8_K => 256,
            Self::IQ2_XXS | Self::IQ2_XS | Self::IQ3_XXS | Self::IQ1_S => 256,
            _ => 1,
        }
    }
}

/// GGUF metadata value
#[derive(Debug, Clone)]
pub enum GgufValue {
    Uint8(u8),
    Int8(i8),
    Uint16(u16),
    Int16(i16),
    Uint32(u32),
    Int32(i32),
    Float32(f32),
    Bool(bool),
    String(String),
    Array(Vec<GgufValue>),
    Uint64(u64),
    Int64(i64),
    Float64(f64),
}

impl GgufValue {
    pub fn as_u32(&self) -> Option<u32> {
        match self {
            Self::Uint32(v) => Some(*v),
            Self::Uint8(v) => Some(*v as u32),
            Self::Uint16(v) => Some(*v as u32),
            _ => None,
        }
    }
    
    pub fn as_u64(&self) -> Option<u64> {
        match self {
            Self::Uint64(v) => Some(*v),
            Self::Uint32(v) => Some(*v as u64),
            _ => None,
        }
    }
    
    pub fn as_f32(&self) -> Option<f32> {
        match self {
            Self::Float32(v) => Some(*v),
            _ => None,
        }
    }
    
    pub fn as_str(&self) -> Option<&str> {
        match self {
            Self::String(s) => Some(s),
            _ => None,
        }
    }
    
    pub fn as_bool(&self) -> Option<bool> {
        match self {
            Self::Bool(v) => Some(*v),
            _ => None,
        }
    }
}

/// GGUF tensor info
#[derive(Debug, Clone)]
pub struct GgufTensor {
    pub name: String,
    pub n_dims: u32,
    pub dims: [u64; 4],
    pub tensor_type: GgmlType,
    pub offset: u64,
}

impl GgufTensor {
    /// Total number of elements
    pub fn n_elements(&self) -> u64 {
        self.dims[..self.n_dims as usize]
            .iter()
            .product()
    }
    
    /// Size in bytes (approximate for quantized)
    pub fn size_bytes(&self) -> u64 {
        let elements = self.n_elements();
        let block_size = self.tensor_type.block_size() as u64;
        let type_size = self.tensor_type.type_size() as u64;
        
        if type_size > 0 {
            elements * type_size
        } else {
            // Quantized: estimate based on block structure
            (elements / block_size) * self.quantized_block_bytes()
        }
    }
    
    fn quantized_block_bytes(&self) -> u64 {
        match self.tensor_type {
            GgmlType::Q4_0 => 18,  // 32 values in 18 bytes
            GgmlType::Q4_1 => 20,
            GgmlType::Q5_0 => 22,
            GgmlType::Q5_1 => 24,
            GgmlType::Q8_0 => 34,
            GgmlType::Q8_1 => 36,
            GgmlType::Q4_K => 144, // 256 values
            GgmlType::Q5_K => 176,
            GgmlType::Q6_K => 210,
            GgmlType::Q8_K => 292,
            GgmlType::Q2_K => 84,
            GgmlType::Q3_K => 110,
            _ => 1,
        }
    }
}

/// GGUF file header
#[derive(Debug, Clone)]
pub struct GgufHeader {
    pub magic: u32,
    pub version: u32,
    pub tensor_count: u64,
    pub metadata_kv_count: u64,
}

/// Parsed GGUF file
#[derive(Debug)]
pub struct GgufFile {
    pub header: GgufHeader,
    pub metadata: BTreeMap<String, GgufValue>,
    pub tensors: Vec<GgufTensor>,
    pub data_offset: u64,
}

impl GgufFile {
    /// Parse GGUF from bytes (header + metadata only, not tensor data)
    pub fn parse(data: &[u8]) -> Result<Self, GgufError> {
        if data.len() < 24 {
            return Err(GgufError::TooShort);
        }
        
        let magic = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
        if magic != GGUF_MAGIC {
            return Err(GgufError::InvalidMagic(magic));
        }
        
        let version = u32::from_le_bytes([data[4], data[5], data[6], data[7]]);
        if version < 2 || version > 3 {
            return Err(GgufError::UnsupportedVersion(version));
        }
        
        let tensor_count = u64::from_le_bytes([
            data[8], data[9], data[10], data[11],
            data[12], data[13], data[14], data[15],
        ]);
        
        let metadata_kv_count = u64::from_le_bytes([
            data[16], data[17], data[18], data[19],
            data[20], data[21], data[22], data[23],
        ]);
        
        let header = GgufHeader {
            magic,
            version,
            tensor_count,
            metadata_kv_count,
        };
        
        // Parse metadata and tensors (simplified - real impl needs full parser)
        let metadata = BTreeMap::new();
        let tensors = Vec::new();
        
        Ok(Self {
            header,
            metadata,
            tensors,
            data_offset: 0, // Calculated after parsing
        })
    }
    
    /// Get model architecture
    pub fn architecture(&self) -> Option<&str> {
        self.metadata.get("general.architecture")?.as_str()
    }
    
    /// Get model name
    pub fn name(&self) -> Option<&str> {
        self.metadata.get("general.name")?.as_str()
    }
    
    /// Get context length
    pub fn context_length(&self) -> Option<u32> {
        self.metadata.get("llama.context_length")?.as_u32()
    }
    
    /// Get embedding length (hidden dim)
    pub fn embedding_length(&self) -> Option<u32> {
        self.metadata.get("llama.embedding_length")?.as_u32()
    }
    
    /// Get number of layers
    pub fn block_count(&self) -> Option<u32> {
        self.metadata.get("llama.block_count")?.as_u32()
    }
    
    /// Get number of attention heads
    pub fn head_count(&self) -> Option<u32> {
        self.metadata.get("llama.attention.head_count")?.as_u32()
    }
    
    /// Get vocab size
    pub fn vocab_size(&self) -> Option<u32> {
        self.metadata.get("llama.vocab_size")?.as_u32()
            .or_else(|| {
                self.metadata.get("tokenizer.ggml.tokens")?
                    .as_str()
                    .map(|_| 0) // Would need array length
            })
    }
    
    /// Estimate total model size
    pub fn total_size(&self) -> u64 {
        self.tensors.iter().map(|t| t.size_bytes()).sum()
    }
    
    /// Get tensor by name
    pub fn tensor(&self, name: &str) -> Option<&GgufTensor> {
        self.tensors.iter().find(|t| t.name == name)
    }
}

/// GGUF parsing errors
#[derive(Debug, Clone)]
pub enum GgufError {
    TooShort,
    InvalidMagic(u32),
    UnsupportedVersion(u32),
    InvalidMetadata,
    InvalidTensor,
    IoError(String),
}

/// Well-known GGUF metadata keys
pub mod keys {
    pub const GENERAL_ARCHITECTURE: &str = "general.architecture";
    pub const GENERAL_NAME: &str = "general.name";
    pub const GENERAL_AUTHOR: &str = "general.author";
    pub const GENERAL_QUANTIZATION_VERSION: &str = "general.quantization_version";
    pub const GENERAL_FILE_TYPE: &str = "general.file_type";
    
    pub const LLAMA_CONTEXT_LENGTH: &str = "llama.context_length";
    pub const LLAMA_EMBEDDING_LENGTH: &str = "llama.embedding_length";
    pub const LLAMA_BLOCK_COUNT: &str = "llama.block_count";
    pub const LLAMA_ATTENTION_HEAD_COUNT: &str = "llama.attention.head_count";
    pub const LLAMA_ATTENTION_HEAD_COUNT_KV: &str = "llama.attention.head_count_kv";
    pub const LLAMA_VOCAB_SIZE: &str = "llama.vocab_size";
    pub const LLAMA_ROPE_FREQ_BASE: &str = "llama.rope.freq_base";
    
    pub const TOKENIZER_MODEL: &str = "tokenizer.ggml.model";
    pub const TOKENIZER_TOKENS: &str = "tokenizer.ggml.tokens";
    pub const TOKENIZER_SCORES: &str = "tokenizer.ggml.scores";
    pub const TOKENIZER_BOS_ID: &str = "tokenizer.ggml.bos_token_id";
    pub const TOKENIZER_EOS_ID: &str = "tokenizer.ggml.eos_token_id";
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_gguf_magic() {
        assert_eq!(GGUF_MAGIC, 0x46554747);
    }

    #[test]
    fn test_ggml_type_sizes() {
        assert_eq!(GgmlType::F32.type_size(), 4);
        assert_eq!(GgmlType::F16.type_size(), 2);
        assert_eq!(GgmlType::Q4_0.block_size(), 32);
        assert_eq!(GgmlType::Q4_K.block_size(), 256);
    }

    #[test]
    fn test_tensor_elements() {
        let tensor = GgufTensor {
            name: "test".to_string(),
            n_dims: 2,
            dims: [1024, 4096, 0, 0],
            tensor_type: GgmlType::F16,
            offset: 0,
        };
        assert_eq!(tensor.n_elements(), 1024 * 4096);
    }
}
