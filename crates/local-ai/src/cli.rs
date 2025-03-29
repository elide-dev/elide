/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

use crate::aimodel::Model;
use anyhow::{Context, Result, anyhow, bail};
use clap::Parser;
use llama_cpp_2::context::params::LlamaContextParams;
use llama_cpp_2::llama_backend::LlamaBackend;
use llama_cpp_2::llama_batch::LlamaBatch;
use llama_cpp_2::model::params::LlamaModelParams;
use llama_cpp_2::model::params::kv_overrides::ParamOverrideValue;
use llama_cpp_2::model::{AddBos, LlamaModel, Special};
use llama_cpp_2::sampling::LlamaSampler;
use llama_cpp_2::{LogOptions, ggml_time_us, send_logs_to_tracing};
use std::ffi::CString;
use std::io::Write;
use std::num::NonZeroU32;
use std::pin::pin;
use std::str::FromStr;
use std::time::Duration;

/// Parse a single key-value pair
fn parse_key_val(s: &str) -> Result<(String, ParamOverrideValue)> {
  let pos = s
    .find('=')
    .ok_or_else(|| anyhow!("invalid KEY=value: no `=` found in `{}`", s))?;
  let key = s[..pos].parse()?;
  let value: String = s[pos + 1..].parse()?;
  let value = i64::from_str(&value)
    .map(ParamOverrideValue::Int)
    .or_else(|_| f64::from_str(&value).map(ParamOverrideValue::Float))
    .or_else(|_| bool::from_str(&value).map(ParamOverrideValue::Bool))
    .map_err(|_| anyhow!("must be one of i64, f64, or bool"))?;

  Ok((key, value))
}

#[cfg(feature = "aitool")]
#[derive(Parser, Debug, Clone)]
pub struct AiArgs {
  /// The path to the model
  #[command(subcommand)]
  model: Model,
  /// The prompt
  #[clap(short = 'p', long)]
  prompt: Option<String>,
  /// Read the prompt from a file
  #[clap(short = 'f', long, help = "prompt file to start generation")]
  file: Option<String>,
  /// set the length of the prompt + output in tokens
  #[arg(long, default_value_t = 32)]
  n_len: i32,
  /// override some parameters of the model
  #[arg(short = 'o', value_parser = parse_key_val)]
  key_value_overrides: Vec<(String, ParamOverrideValue)>,
  /// Disable offloading layers to the gpu
  #[cfg(any(feature = "cuda", feature = "vulkan"))]
  #[clap(long)]
  disable_gpu: bool,
  #[arg(short = 's', long, help = "RNG seed (default: 1234)")]
  seed: Option<u32>,
  #[arg(
    short = 't',
    long,
    help = "number of threads to use during generation (default: use all available threads)"
  )]
  threads: Option<i32>,
  #[arg(
    long,
    help = "number of threads to use during batch and prompt processing (default: use all available threads)"
  )]
  threads_batch: Option<i32>,
  #[arg(
    short = 'c',
    long,
    help = "size of the prompt context (default: loaded from themodel)"
  )]
  ctx_size: Option<NonZeroU32>,
  #[arg(short = 'v', long, help = "enable verbose llama.cpp logs")]
  verbose: bool,
}

#[cfg(feature = "aitool")]
#[allow(clippy::too_many_lines)]
pub async fn entry() -> Result<()> {
  let AiArgs {
    n_len,
    model,
    prompt,
    file,
    #[cfg(any(feature = "cuda", feature = "vulkan"))]
    disable_gpu,
    key_value_overrides,
    seed,
    threads,
    threads_batch,
    ctx_size,
    verbose,
  } = AiArgs::parse();

  if verbose {
    tracing_subscriber::fmt().init();
  }
  send_logs_to_tracing(LogOptions::default().with_logs_enabled(verbose));

  // init LLM
  let backend = LlamaBackend::init()?;

  // offload all layers to the gpu
  let model_params = {
    #[cfg(any(feature = "cuda", feature = "vulkan"))]
    if !disable_gpu {
      LlamaModelParams::default().with_n_gpu_layers(1000)
    } else {
      LlamaModelParams::default()
    }
    #[cfg(not(any(feature = "cuda", feature = "vulkan")))]
    LlamaModelParams::default()
  };

  let prompt = if let Some(str) = prompt {
    if file.is_some() {
      bail!("either prompt or file must be specified, but not both")
    }
    str
  } else if let Some(file) = file {
    std::fs::read_to_string(&file).with_context(|| format!("unable to read {file}"))?
  } else {
    "Hello my name is".to_string()
  };

  let mut model_params = pin!(model_params);

  for (k, v) in &key_value_overrides {
    let k = CString::new(k.as_bytes()).with_context(|| format!("invalid key: {k}"))?;
    model_params.as_mut().append_kv_override(k.as_c_str(), *v);
  }

  let model_path = model
    .get_or_load()
    .await
    .with_context(|| "failed to get model from args")?;

  let model = LlamaModel::load_from_file(&backend, model_path, &model_params)
    .with_context(|| "unable to load model")?;

  // initialize the context
  let mut ctx_params =
    LlamaContextParams::default().with_n_ctx(ctx_size.or(Some(NonZeroU32::new(2048).unwrap())));

  if let Some(threads) = threads {
    ctx_params = ctx_params.with_n_threads(threads);
  }
  if let Some(threads_batch) = threads_batch.or(threads) {
    ctx_params = ctx_params.with_n_threads_batch(threads_batch);
  }

  let mut ctx = model
    .new_context(&backend, ctx_params)
    .with_context(|| "unable to create the llama_context")?;

  // tokenize the prompt

  let tokens_list = model
    .str_to_token(&prompt, AddBos::Always)
    .with_context(|| format!("failed to tokenize {prompt}"))?;

  let n_cxt = ctx.n_ctx() as i32;
  let n_kv_req = tokens_list.len() as i32 + (n_len - tokens_list.len() as i32);

  eprintln!("n_len = {n_len}, n_ctx = {n_cxt}, k_kv_req = {n_kv_req}");

  // make sure the KV cache is big enough to hold all the prompt and generated tokens
  if n_kv_req > n_cxt {
    bail!(
      "n_kv_req > n_ctx, the required kv cache size is not big enough
either reduce n_len or increase n_ctx"
    )
  }

  if tokens_list.len() >= usize::try_from(n_len)? {
    bail!("the prompt is too long, it has more tokens than n_len")
  }

  // print the prompt token-by-token
  eprintln!();

  for token in &tokens_list {
    eprint!("{}", model.token_to_str(*token, Special::Tokenize)?);
  }

  std::io::stderr().flush()?;

  // create a llama_batch with size 512
  // we use this object to submit token data for decoding
  let mut batch = LlamaBatch::new(512, 1);

  let last_index: i32 = (tokens_list.len() - 1) as i32;
  for (i, token) in (0_i32..).zip(tokens_list.into_iter()) {
    // llama_decode will output logits only for the last token of the prompt
    let is_last = i == last_index;
    batch.add(token, i, &[0], is_last)?;
  }

  ctx
    .decode(&mut batch)
    .with_context(|| "llama_decode() failed")?;

  // main loop

  let mut n_cur = batch.n_tokens();
  let mut n_decode = 0;

  let t_main_start = ggml_time_us();

  // The `Decoder`
  let mut decoder = encoding_rs::UTF_8.new_decoder();

  let mut sampler = LlamaSampler::chain_simple([
    LlamaSampler::dist(seed.unwrap_or(1234)),
    LlamaSampler::greedy(),
  ]);

  while n_cur <= n_len {
    // sample the next token
    {
      let token = sampler.sample(&ctx, batch.n_tokens() - 1);

      sampler.accept(token);

      // is it an end of stream?
      if model.is_eog_token(token) {
        eprintln!();
        break;
      }

      let output_bytes = model.token_to_bytes(token, Special::Tokenize)?;
      // use `Decoder.decode_to_string()` to avoid the intermediate buffer
      let mut output_string = String::with_capacity(32);
      let _decode_result = decoder.decode_to_string(&output_bytes, &mut output_string, false);
      print!("{output_string}");
      std::io::stdout().flush()?;

      batch.clear();
      batch.add(token, n_cur, &[0], true)?;
    }

    n_cur += 1;

    ctx.decode(&mut batch).with_context(|| "failed to eval")?;

    n_decode += 1;
  }

  eprintln!("\n");

  let t_main_end = ggml_time_us();

  let duration = Duration::from_micros((t_main_end - t_main_start) as u64);

  eprintln!(
    "decoded {} tokens in {:.2} s, speed {:.2} t/s\n",
    n_decode,
    duration.as_secs_f32(),
    n_decode as f32 / duration.as_secs_f32()
  );

  println!("{}", ctx.timings());

  Ok(())
}
