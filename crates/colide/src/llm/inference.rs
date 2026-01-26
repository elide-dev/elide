//! LLM Inference Engine
//!
//! Core inference loop for transformer models using GGUF format.
//! Supports CPU and GPU acceleration with layer offloading.

use super::memory::{KvCache, MemoryManager, MemoryType};
use super::gguf::GgufFile;
use super::{ModelConfig, OffloadConfig, Quantization};

/// Inference state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum InferenceState {
    /// Not initialized
    Uninitialized,
    /// Model loaded and ready
    Ready,
    /// Currently generating
    Generating,
    /// Paused (can resume)
    Paused,
    /// Error state
    Error,
}

/// Sampling parameters
#[derive(Debug, Clone)]
pub struct SamplingParams {
    /// Temperature (0.0 = greedy, higher = more random)
    pub temperature: f32,
    /// Top-p nucleus sampling
    pub top_p: f32,
    /// Top-k sampling (0 = disabled)
    pub top_k: u32,
    /// Repetition penalty
    pub repeat_penalty: f32,
    /// Repetition penalty window
    pub repeat_last_n: u32,
    /// Frequency penalty
    pub frequency_penalty: f32,
    /// Presence penalty
    pub presence_penalty: f32,
    /// Mirostat sampling mode (0 = disabled)
    pub mirostat: u8,
    /// Mirostat tau
    pub mirostat_tau: f32,
    /// Mirostat eta
    pub mirostat_eta: f32,
    /// Random seed (-1 = random)
    pub seed: i64,
}

impl Default for SamplingParams {
    fn default() -> Self {
        Self {
            temperature: 0.7,
            top_p: 0.9,
            top_k: 40,
            repeat_penalty: 1.1,
            repeat_last_n: 64,
            frequency_penalty: 0.0,
            presence_penalty: 0.0,
            mirostat: 0,
            mirostat_tau: 5.0,
            mirostat_eta: 0.1,
            seed: -1,
        }
    }
}

impl SamplingParams {
    /// Greedy sampling (deterministic)
    pub fn greedy() -> Self {
        Self {
            temperature: 0.0,
            top_p: 1.0,
            top_k: 0,
            ..Default::default()
        }
    }
    
    /// Creative sampling
    pub fn creative() -> Self {
        Self {
            temperature: 1.0,
            top_p: 0.95,
            top_k: 0,
            ..Default::default()
        }
    }
    
    /// Balanced sampling
    pub fn balanced() -> Self {
        Self::default()
    }
}

/// Generation parameters
#[derive(Debug, Clone)]
pub struct GenerationParams {
    /// Maximum tokens to generate
    pub max_tokens: u32,
    /// Stop sequences
    pub stop_sequences: Vec<String>,
    /// Stream tokens as generated
    pub stream: bool,
    /// Echo prompt in output
    pub echo: bool,
    /// Sampling parameters
    pub sampling: SamplingParams,
}

impl Default for GenerationParams {
    fn default() -> Self {
        Self {
            max_tokens: 512,
            stop_sequences: vec![],
            stream: false,
            echo: false,
            sampling: SamplingParams::default(),
        }
    }
}

/// Token with metadata
#[derive(Debug, Clone)]
pub struct Token {
    pub id: u32,
    pub text: String,
    pub logprob: f32,
}

/// Generation result
#[derive(Debug, Clone)]
pub struct GenerationResult {
    /// Generated tokens
    pub tokens: Vec<Token>,
    /// Full generated text
    pub text: String,
    /// Number of prompt tokens
    pub prompt_tokens: u32,
    /// Number of completion tokens
    pub completion_tokens: u32,
    /// Stop reason
    pub stop_reason: StopReason,
    /// Time to first token (ms)
    pub time_to_first_token_ms: u64,
    /// Total generation time (ms)
    pub total_time_ms: u64,
    /// Tokens per second
    pub tokens_per_second: f32,
}

/// Why generation stopped
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum StopReason {
    /// Hit max tokens limit
    MaxTokens,
    /// Hit stop sequence
    StopSequence,
    /// End of sequence token
    EndOfSequence,
    /// User cancelled
    Cancelled,
    /// Error occurred
    Error,
}

/// LLM Inference Engine
pub struct InferenceEngine {
    /// Current state
    state: InferenceState,
    /// Model configuration
    config: Option<ModelConfig>,
    /// Memory manager
    memory: MemoryManager,
    /// KV cache
    kv_cache: Option<KvCache>,
    /// Current position in sequence
    position: u32,
    /// Vocabulary size
    vocab_size: u32,
    /// Layer offload configuration
    offload: OffloadConfig,
}

impl InferenceEngine {
    pub fn new(ram_gb: usize, vram_gb: usize) -> Self {
        Self {
            state: InferenceState::Uninitialized,
            config: None,
            memory: MemoryManager::new(ram_gb, vram_gb),
            kv_cache: None,
            position: 0,
            vocab_size: 0,
            offload: OffloadConfig::default(),
        }
    }
    
    /// Load model from GGUF file
    pub fn load(&mut self, config: ModelConfig) -> Result<(), InferenceError> {
        // Validate hardware requirements
        let needed = config.estimate_memory_gb();
        let available = (self.memory.ram_available + self.memory.vram_available) as f32 / 1e9;
        
        if needed > available * 0.95 {
            return Err(InferenceError::InsufficientMemory {
                needed_gb: needed,
                available_gb: available,
            });
        }
        
        // Plan layer distribution
        let layer_size = (config.params_billions * 1e9 / config.num_layers as f32) as usize;
        let layer_plan = self.memory.plan_layers(config.num_layers, layer_size);
        
        // Allocate KV cache
        let kv_heads = config.num_heads; // Simplified, may differ for GQA
        let head_dim = config.hidden_dim / config.num_heads;
        self.kv_cache = Some(KvCache::new(
            config.offload.context_size as usize,
            config.num_layers as usize,
            kv_heads as usize,
            head_dim as usize,
        ));
        
        self.vocab_size = config.vocab_size;
        self.offload = config.offload.clone();
        self.config = Some(config);
        self.state = InferenceState::Ready;
        
        Ok(())
    }
    
    /// Generate completion for prompt
    pub fn generate(
        &mut self,
        prompt: &str,
        params: GenerationParams,
    ) -> Result<GenerationResult, InferenceError> {
        if self.state != InferenceState::Ready {
            return Err(InferenceError::NotReady);
        }
        
        self.state = InferenceState::Generating;
        let start_time = std::time::Instant::now();
        
        // Tokenize prompt (placeholder)
        let prompt_tokens = self.tokenize(prompt);
        let prompt_token_count = prompt_tokens.len() as u32;
        
        // Process prompt through model
        self.process_prompt(&prompt_tokens)?;
        let time_to_first = start_time.elapsed().as_millis() as u64;
        
        // Generate tokens
        let mut generated = Vec::new();
        let mut text = String::new();
        
        for _ in 0..params.max_tokens {
            let token = self.sample_next(&params.sampling)?;
            
            // Check for EOS
            if token.id == self.eos_token_id() {
                break;
            }
            
            text.push_str(&token.text);
            generated.push(token);
            
            // Check stop sequences
            let should_stop = params.stop_sequences.iter()
                .any(|seq| text.ends_with(seq));
            if should_stop {
                break;
            }
            
            self.position += 1;
        }
        
        let total_time = start_time.elapsed().as_millis() as u64;
        let completion_tokens = generated.len() as u32;
        let tps = if total_time > 0 {
            completion_tokens as f32 * 1000.0 / total_time as f32
        } else {
            0.0
        };
        
        self.state = InferenceState::Ready;
        
        Ok(GenerationResult {
            tokens: generated,
            text,
            prompt_tokens: prompt_token_count,
            completion_tokens,
            stop_reason: StopReason::MaxTokens,
            time_to_first_token_ms: time_to_first,
            total_time_ms: total_time,
            tokens_per_second: tps,
        })
    }
    
    /// Reset KV cache for new conversation
    pub fn reset(&mut self) {
        if let Some(ref mut cache) = self.kv_cache {
            cache.reset();
        }
        self.position = 0;
    }
    
    /// Get current state
    pub fn state(&self) -> InferenceState {
        self.state
    }
    
    /// Tokenize text (placeholder - needs actual tokenizer)
    fn tokenize(&self, text: &str) -> Vec<u32> {
        // Placeholder: simple byte encoding
        text.bytes().map(|b| b as u32).collect()
    }
    
    /// Detokenize tokens (placeholder)
    fn detokenize(&self, tokens: &[u32]) -> String {
        // Placeholder: simple byte decoding
        tokens.iter()
            .filter_map(|&t| char::from_u32(t))
            .collect()
    }
    
    /// Process prompt tokens
    fn process_prompt(&mut self, tokens: &[u32]) -> Result<(), InferenceError> {
        // Placeholder for actual forward pass
        self.position = tokens.len() as u32;
        Ok(())
    }
    
    /// Sample next token
    fn sample_next(&self, params: &SamplingParams) -> Result<Token, InferenceError> {
        // Placeholder for actual sampling
        Ok(Token {
            id: 0,
            text: " ".to_string(),
            logprob: 0.0,
        })
    }
    
    /// Get EOS token ID
    fn eos_token_id(&self) -> u32 {
        // Common EOS for Qwen/Llama
        151643
    }
}

/// Inference errors
#[derive(Debug, Clone)]
pub enum InferenceError {
    NotReady,
    InsufficientMemory { needed_gb: f32, available_gb: f32 },
    ModelLoadFailed(String),
    TokenizationFailed,
    GenerationFailed(String),
    Cancelled,
}

/// Benchmark results
#[derive(Debug, Clone)]
pub struct BenchmarkResult {
    pub model_name: String,
    pub quantization: String,
    pub prompt_tokens: u32,
    pub generated_tokens: u32,
    pub prompt_eval_time_ms: u64,
    pub generation_time_ms: u64,
    pub prompt_eval_tps: f32,
    pub generation_tps: f32,
    pub memory_used_gb: f32,
}

/// Run inference benchmark
pub fn benchmark(
    engine: &mut InferenceEngine,
    prompt: &str,
    n_tokens: u32,
) -> Result<BenchmarkResult, InferenceError> {
    let config = engine.config.as_ref().ok_or(InferenceError::NotReady)?;
    let model_name = config.path.clone();
    let quantization = format!("{:?}", config.quantization);
    let memory_used_gb = config.estimate_memory_gb();
    
    let params = GenerationParams {
        max_tokens: n_tokens,
        sampling: SamplingParams::greedy(),
        ..Default::default()
    };
    
    let result = engine.generate(prompt, params)?;
    
    Ok(BenchmarkResult {
        model_name,
        quantization,
        prompt_tokens: result.prompt_tokens,
        generated_tokens: result.completion_tokens,
        prompt_eval_time_ms: result.time_to_first_token_ms,
        generation_time_ms: result.total_time_ms - result.time_to_first_token_ms,
        prompt_eval_tps: result.prompt_tokens as f32 * 1000.0 / result.time_to_first_token_ms.max(1) as f32,
        generation_tps: result.tokens_per_second,
        memory_used_gb,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sampling_params() {
        let greedy = SamplingParams::greedy();
        assert_eq!(greedy.temperature, 0.0);
        
        let creative = SamplingParams::creative();
        assert_eq!(creative.temperature, 1.0);
    }

    #[test]
    fn test_engine_creation() {
        let engine = InferenceEngine::new(16, 8);
        assert_eq!(engine.state(), InferenceState::Uninitialized);
    }

    #[test]
    fn test_model_load() {
        let mut engine = InferenceEngine::new(32, 0);
        let config = ModelConfig::qwen25("7B", Quantization::Q4_K_M).unwrap();
        
        let result = engine.load(config);
        assert!(result.is_ok());
        assert_eq!(engine.state(), InferenceState::Ready);
    }
}
