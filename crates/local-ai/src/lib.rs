/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

#![allow(
  clippy::cast_possible_wrap,
  clippy::cast_possible_truncation,
  clippy::cast_precision_loss,
  clippy::cast_sign_loss,
  clippy::too_many_arguments
)]

pub mod aimodel;
pub mod cli;

pub mod blocking;
pub mod threaded;

use std::io::Write;
use std::num::NonZeroU32;
use std::path::PathBuf;
use std::pin::Pin;
use std::sync::Mutex;
use std::time::Duration;

// Re-exports.
#[cfg(feature = "aitool")]
pub use crate::cli::AiArgs;
#[cfg(feature = "aitool")]
pub use crate::cli::entry;

pub use crate::aimodel::Model;

use anyhow::{Context, Error, bail};
use java_native::jni;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{JNI_TRUE, jboolean};
use llama_cpp_2::context::params::LlamaContextParams;
use llama_cpp_2::ggml_time_us;
use llama_cpp_2::llama_backend::LlamaBackend;
use llama_cpp_2::llama_batch::LlamaBatch;
use llama_cpp_2::model::params::LlamaModelParams;
use llama_cpp_2::model::{AddBos, LlamaModel, Special};
use llama_cpp_2::sampling::LlamaSampler;

#[cfg(feature = "debug")]
const DEBUG_LOGS: bool = true;

#[cfg(not(feature = "debug"))]
const DEBUG_LOGS: bool = false;

const DEFAULT_THREADS: i32 = 1;
const DEFAULT_THREAD_BATCH: i32 = 1;
const DEFAULT_LENGTH: i32 = 42;
const DEFAULT_SEED: u32 = 1234;
const DEFAULT_CONTEXT_WINDOW: u32 = 2048;

// Llama backend; available after init.
static BACKEND: Mutex<Option<LlamaBackend>> = Mutex::new(None);

/// Prepare a model for use.
async fn prep_model(model: Model) -> Result<PathBuf, Box<dyn std::error::Error>> {
  // prep and load model
  let model_path = model.get_or_load().await.expect("failed to load model");
  Ok(model_path)
}

/// Perform inference against the backend; this method returns a future.
async fn do_infer(
  verbose: jboolean,
  params: Pin<&mut LlamaModelParams>,
  prompt: String,
  model_path: PathBuf,
  ctx_size: Option<NonZeroU32>,
  threads: Option<i32>,
  threads_batch: Option<i32>,
  length: Option<i32>,
  seed: Option<u32>,
  cbk: Option<Box<dyn Fn(Result<String, Error>) + Send>>,
) -> Result<Option<String>, Error> {
  if DEBUG_LOGS {
    eprintln!("INF(do_infer): resolving backend");
  }
  //noinspection RsUnwrap
  let mut backend = BACKEND.lock().unwrap();
  let backend = backend.as_mut().expect("backend not initialized");
  if verbose != JNI_TRUE {
    backend.void_logs();
  }

  let model = LlamaModel::load_from_file(backend, model_path, &params)
    .with_context(|| "unable to load model")?;

  // initialize the context
  let mut ctx_params = LlamaContextParams::default()
    .with_n_ctx(ctx_size.or(Some(NonZeroU32::new(DEFAULT_CONTEXT_WINDOW).unwrap())));

  if let Some(threads) = threads {
    ctx_params = ctx_params.with_n_threads(threads);
  }
  if let Some(threads_batch) = threads_batch.or(threads) {
    ctx_params = ctx_params.with_n_threads_batch(threads_batch);
  }

  let mut ctx = model
    .new_context(backend, ctx_params)
    .with_context(|| "unable to create the llama_context")?;

  let tokens_list = model
    .str_to_token(&prompt, AddBos::Always)
    .with_context(|| format!("failed to tokenize {prompt}"))?;

  let n_cxt = ctx.n_ctx() as i32;
  let n_len: i32 = length.unwrap_or(DEFAULT_LENGTH);
  let n_kv_req = tokens_list.len() as i32 + (n_len - tokens_list.len() as i32);
  if tokens_list.len() >= usize::try_from(n_len)? {
    bail!("the prompt is too long, it has more tokens than n_len")
  }
  if DEBUG_LOGS {
    eprintln!("n_len = {n_len}, n_ctx = {n_cxt}, k_kv_req = {n_kv_req}");
  }
  // make sure the KV cache is big enough to hold all the prompt and generated tokens
  if n_kv_req > n_cxt {
    bail!(
      "n_kv_req > n_ctx, the required kv cache size is not big enough either reduce n_len or increase n_ctx"
    )
  }
  if tokens_list.len() >= usize::try_from(n_len)? {
    bail!("the prompt is too long, it has more tokens than n_len")
  }

  if DEBUG_LOGS {
    // print the prompt token-by-token
    eprintln!();

    for token in &tokens_list {
      eprint!("{}", model.token_to_str(*token, Special::Tokenize)?);
    }
    std::io::stderr().flush()?;
  }

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
  let mut decoder = encoding_rs::UTF_8.new_decoder();

  let mut sampler = LlamaSampler::chain_simple([
    LlamaSampler::dist(seed.unwrap_or(DEFAULT_SEED)),
    LlamaSampler::greedy(),
  ]);

  let rendered_outbuf = Mutex::new(Vec::new());
  let mut is_callback_mode = false;
  let use_chunk = if cbk.is_some() {
    is_callback_mode = true;
    Box::new(move |chunk: String| {
      if let Some(cbk) = &cbk {
        cbk(Ok(chunk));
      } else {
        panic!("failed to locate callback");
      }
      0
    }) as Box<dyn Fn(String) -> i32>
  } else {
    Box::new(|chunk: String| {
      let mut buf = rendered_outbuf.lock().unwrap();
      buf.push(chunk);
      0
    }) as Box<dyn Fn(String) -> i32>
  };

  while n_cur <= n_len {
    // sample the next token
    {
      let token = sampler.sample(&ctx, batch.n_tokens() - 1);
      sampler.accept(token);

      // is it an end of stream?
      if model.is_eog_token(token) {
        if DEBUG_LOGS {
          eprintln!();
        }
        break;
      }

      let output_bytes = model.token_to_bytes(token, Special::Tokenize)?;
      // use `Decoder.decode_to_string()` to avoid the intermediate buffer
      let mut output_string = String::with_capacity(32);
      let _decode_result = decoder.decode_to_string(&output_bytes, &mut output_string, false);
      if DEBUG_LOGS {
        use_chunk(output_string.clone());
        print!("{output_string}");
        std::io::stdout().flush()?;
      } else {
        use_chunk(output_string);
      }
      batch.clear();
      batch.add(token, n_cur, &[0], true)?;
    }

    n_cur += 1;
    ctx.decode(&mut batch).with_context(|| "failed to eval")?;
    n_decode += 1;
  }

  if DEBUG_LOGS {
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
  }
  if is_callback_mode {
    Ok(None)
  } else {
    let rendered_out = rendered_outbuf.lock().unwrap().join("");
    Ok(Some(rendered_out))
  }
}

/// Initialize the native AI backend.
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn initialize(_env: JNIEnv, _class: JClass<'_>, default_verbose: jboolean) -> jboolean {
  let mut backend = LlamaBackend::init().expect("failed to initialize native llama backend");
  let mut guard = BACKEND.lock().unwrap();
  if default_verbose != JNI_TRUE {
    backend.void_logs();
  }
  *guard = Some(backend);
  JNI_TRUE
}

/// De-initialize the native AI backend.
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn deinitialize(_env: JNIEnv, _class: JClass<'_>) {
  let mut guard = BACKEND.lock().unwrap();
  guard.take();
}
