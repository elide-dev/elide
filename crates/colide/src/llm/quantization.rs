//! Quantization Utilities
//!
//! Support for GGML/GGUF quantization formats and dequantization.

use super::gguf::GgmlType;

/// Dequantize Q4_0 block
pub fn dequantize_q4_0(block: &[u8], output: &mut [f32]) {
    if block.len() < 18 || output.len() < 32 {
        return;
    }
    
    // Q4_0: 32 values in 18 bytes
    // First 2 bytes: f16 scale
    // Next 16 bytes: 32 x 4-bit values
    let scale = f16_to_f32(u16::from_le_bytes([block[0], block[1]]));
    
    for i in 0..16 {
        let byte = block[2 + i];
        let lo = (byte & 0x0F) as i8 - 8;
        let hi = ((byte >> 4) & 0x0F) as i8 - 8;
        
        output[i * 2] = scale * lo as f32;
        output[i * 2 + 1] = scale * hi as f32;
    }
}

/// Dequantize Q4_1 block
pub fn dequantize_q4_1(block: &[u8], output: &mut [f32]) {
    if block.len() < 20 || output.len() < 32 {
        return;
    }
    
    // Q4_1: 32 values in 20 bytes
    // First 2 bytes: f16 scale
    // Next 2 bytes: f16 min
    // Next 16 bytes: 32 x 4-bit values
    let scale = f16_to_f32(u16::from_le_bytes([block[0], block[1]]));
    let min = f16_to_f32(u16::from_le_bytes([block[2], block[3]]));
    
    for i in 0..16 {
        let byte = block[4 + i];
        let lo = (byte & 0x0F) as f32;
        let hi = ((byte >> 4) & 0x0F) as f32;
        
        output[i * 2] = scale * lo + min;
        output[i * 2 + 1] = scale * hi + min;
    }
}

/// Dequantize Q8_0 block
pub fn dequantize_q8_0(block: &[u8], output: &mut [f32]) {
    if block.len() < 34 || output.len() < 32 {
        return;
    }
    
    // Q8_0: 32 values in 34 bytes
    // First 2 bytes: f16 scale
    // Next 32 bytes: 32 x int8 values
    let scale = f16_to_f32(u16::from_le_bytes([block[0], block[1]]));
    
    for i in 0..32 {
        let val = block[2 + i] as i8;
        output[i] = scale * val as f32;
    }
}

/// Dequantize Q5_0 block
pub fn dequantize_q5_0(block: &[u8], output: &mut [f32]) {
    if block.len() < 22 || output.len() < 32 {
        return;
    }
    
    // Q5_0: 32 values in 22 bytes
    // First 2 bytes: f16 scale
    // Next 4 bytes: 32 high bits
    // Next 16 bytes: 32 x 4-bit low values
    let scale = f16_to_f32(u16::from_le_bytes([block[0], block[1]]));
    let high_bits = u32::from_le_bytes([block[2], block[3], block[4], block[5]]);
    
    for i in 0..16 {
        let byte = block[6 + i];
        let lo_0 = (byte & 0x0F) as i8;
        let lo_1 = ((byte >> 4) & 0x0F) as i8;
        let hi_0 = ((high_bits >> (i * 2)) & 1) as i8;
        let hi_1 = ((high_bits >> (i * 2 + 1)) & 1) as i8;
        
        let val_0 = ((hi_0 << 4) | lo_0) - 16;
        let val_1 = ((hi_1 << 4) | lo_1) - 16;
        
        output[i * 2] = scale * val_0 as f32;
        output[i * 2 + 1] = scale * val_1 as f32;
    }
}

/// Convert f16 (u16 bits) to f32
fn f16_to_f32(bits: u16) -> f32 {
    let sign = ((bits >> 15) & 1) as u32;
    let exp = ((bits >> 10) & 0x1F) as u32;
    let frac = (bits & 0x3FF) as u32;
    
    if exp == 0 {
        // Subnormal or zero
        if frac == 0 {
            f32::from_bits(sign << 31)
        } else {
            // Subnormal
            let mut e = -14i32;
            let mut f = frac;
            while (f & 0x400) == 0 {
                f <<= 1;
                e -= 1;
            }
            f &= 0x3FF;
            let exp32 = ((127 + e) as u32) << 23;
            let frac32 = f << 13;
            f32::from_bits((sign << 31) | exp32 | frac32)
        }
    } else if exp == 31 {
        // Inf or NaN
        if frac == 0 {
            f32::from_bits((sign << 31) | 0x7F800000)
        } else {
            f32::from_bits((sign << 31) | 0x7F800000 | (frac << 13))
        }
    } else {
        // Normal
        let exp32 = ((exp as i32 - 15 + 127) as u32) << 23;
        let frac32 = frac << 13;
        f32::from_bits((sign << 31) | exp32 | frac32)
    }
}

/// Convert f32 to f16 (u16 bits)
fn f32_to_f16(val: f32) -> u16 {
    let bits = val.to_bits();
    let sign = ((bits >> 31) & 1) as u16;
    let exp = ((bits >> 23) & 0xFF) as i32;
    let frac = bits & 0x7FFFFF;
    
    if exp == 255 {
        // Inf or NaN
        if frac == 0 {
            (sign << 15) | 0x7C00
        } else {
            (sign << 15) | 0x7C00 | ((frac >> 13) as u16)
        }
    } else if exp > 142 {
        // Overflow to inf
        (sign << 15) | 0x7C00
    } else if exp < 113 {
        // Underflow to zero or subnormal
        if exp < 103 {
            sign << 15
        } else {
            let shift = 113 - exp;
            let frac16 = ((0x800000 | frac) >> (shift + 13)) as u16;
            (sign << 15) | frac16
        }
    } else {
        // Normal
        let exp16 = ((exp - 112) as u16) << 10;
        let frac16 = (frac >> 13) as u16;
        (sign << 15) | exp16 | frac16
    }
}

/// Quantization statistics
#[derive(Debug, Clone)]
pub struct QuantStats {
    pub min: f32,
    pub max: f32,
    pub mean: f32,
    pub std: f32,
    pub absmax: f32,
}

impl QuantStats {
    pub fn compute(values: &[f32]) -> Self {
        if values.is_empty() {
            return Self {
                min: 0.0,
                max: 0.0,
                mean: 0.0,
                std: 0.0,
                absmax: 0.0,
            };
        }
        
        let min = values.iter().fold(f32::INFINITY, |a, &b| a.min(b));
        let max = values.iter().fold(f32::NEG_INFINITY, |a, &b| a.max(b));
        let sum: f32 = values.iter().sum();
        let mean = sum / values.len() as f32;
        
        let variance: f32 = values.iter()
            .map(|&x| (x - mean).powi(2))
            .sum::<f32>() / values.len() as f32;
        let std = variance.sqrt();
        
        let absmax = values.iter()
            .fold(0.0f32, |a, &b| a.max(b.abs()));
        
        Self { min, max, mean, std, absmax }
    }
    
    /// Compute optimal scale for symmetric quantization
    pub fn symmetric_scale(&self, bits: u8) -> f32 {
        let max_int = (1 << (bits - 1)) - 1;
        self.absmax / max_int as f32
    }
    
    /// Compute optimal scale and zero point for asymmetric
    pub fn asymmetric_params(&self, bits: u8) -> (f32, f32) {
        let max_int = (1 << bits) - 1;
        let scale = (self.max - self.min) / max_int as f32;
        let zero_point = -self.min / scale;
        (scale, zero_point)
    }
}

/// Quantize f32 values to Q4_0 format
pub fn quantize_q4_0(values: &[f32], output: &mut [u8]) -> usize {
    let n_blocks = values.len() / 32;
    let output_size = n_blocks * 18;
    
    if output.len() < output_size {
        return 0;
    }
    
    for b in 0..n_blocks {
        let block_values = &values[b * 32..(b + 1) * 32];
        let block_output = &mut output[b * 18..(b + 1) * 18];
        
        // Find absmax for scale
        let absmax = block_values.iter()
            .fold(0.0f32, |a, &v| a.max(v.abs()));
        let scale = absmax / 7.0; // 4-bit signed range is -8..7
        
        // Write scale as f16
        let scale_f16 = f32_to_f16(scale);
        block_output[0] = scale_f16 as u8;
        block_output[1] = (scale_f16 >> 8) as u8;
        
        // Quantize values
        let inv_scale = if scale != 0.0 { 1.0 / scale } else { 0.0 };
        for i in 0..16 {
            let v0 = block_values[i * 2];
            let v1 = block_values[i * 2 + 1];
            
            let q0 = ((v0 * inv_scale).round() as i8).clamp(-8, 7) + 8;
            let q1 = ((v1 * inv_scale).round() as i8).clamp(-8, 7) + 8;
            
            block_output[2 + i] = (q0 as u8) | ((q1 as u8) << 4);
        }
    }
    
    output_size
}

/// Quantize f32 values to Q8_0 format
pub fn quantize_q8_0(values: &[f32], output: &mut [u8]) -> usize {
    let n_blocks = values.len() / 32;
    let output_size = n_blocks * 34;
    
    if output.len() < output_size {
        return 0;
    }
    
    for b in 0..n_blocks {
        let block_values = &values[b * 32..(b + 1) * 32];
        let block_output = &mut output[b * 34..(b + 1) * 34];
        
        // Find absmax for scale
        let absmax = block_values.iter()
            .fold(0.0f32, |a, &v| a.max(v.abs()));
        let scale = absmax / 127.0;
        
        // Write scale as f16
        let scale_f16 = f32_to_f16(scale);
        block_output[0] = scale_f16 as u8;
        block_output[1] = (scale_f16 >> 8) as u8;
        
        // Quantize values
        let inv_scale = if scale != 0.0 { 1.0 / scale } else { 0.0 };
        for i in 0..32 {
            let v = block_values[i];
            let q = (v * inv_scale).round() as i8;
            block_output[2 + i] = q as u8;
        }
    }
    
    output_size
}

/// Perplexity calculation for model evaluation
pub fn calculate_perplexity(logprobs: &[f32]) -> f32 {
    if logprobs.is_empty() {
        return 0.0;
    }
    
    let avg_logprob: f32 = logprobs.iter().sum::<f32>() / logprobs.len() as f32;
    (-avg_logprob).exp()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_f16_conversion() {
        let val = 1.5f32;
        let f16 = f32_to_f16(val);
        let back = f16_to_f32(f16);
        assert!((val - back).abs() < 0.01);
    }

    #[test]
    fn test_q4_0_roundtrip() {
        let values: Vec<f32> = (0..32).map(|i| (i as f32 - 16.0) * 0.1).collect();
        let mut quantized = vec![0u8; 18];
        let mut output = vec![0.0f32; 32];
        
        quantize_q4_0(&values, &mut quantized);
        dequantize_q4_0(&quantized, &mut output);
        
        // Check approximate equality (quantization is lossy)
        for (orig, deq) in values.iter().zip(output.iter()) {
            assert!((orig - deq).abs() < 0.3);
        }
    }

    #[test]
    fn test_q8_0_roundtrip() {
        let values: Vec<f32> = (0..32).map(|i| (i as f32 - 16.0) * 0.1).collect();
        let mut quantized = vec![0u8; 34];
        let mut output = vec![0.0f32; 32];
        
        quantize_q8_0(&values, &mut quantized);
        dequantize_q8_0(&quantized, &mut output);
        
        // Q8 should be more precise
        for (orig, deq) in values.iter().zip(output.iter()) {
            assert!((orig - deq).abs() < 0.05);
        }
    }

    #[test]
    fn test_quant_stats() {
        let values = vec![1.0, 2.0, 3.0, 4.0, 5.0];
        let stats = QuantStats::compute(&values);
        
        assert_eq!(stats.min, 1.0);
        assert_eq!(stats.max, 5.0);
        assert_eq!(stats.mean, 3.0);
        assert_eq!(stats.absmax, 5.0);
    }
}
