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

use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[cfg(feature = "blocking")]
use {
  crate::{
    DEBUG_LOGS, DEFAULT_CONTEXT_WINDOW, DEFAULT_LENGTH, DEFAULT_SEED, DEFAULT_THREAD_BATCH,
    DEFAULT_THREADS, Model, do_infer, prep_model,
  },
  anyhow::Error,
  jni::sys::{jboolean, jint},
  llama_cpp_2::model::params::LlamaModelParams,
  std::num::NonZeroU32,
  std::path::PathBuf,
  std::pin::pin,
};

#[cfg(not(feature = "blocking"))]
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn inferSync<'a>() -> jstring {
  // no-op
}

/// Infer synchronously against the configured model backend.
#[cfg(feature = "blocking")]
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn inferSync<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  verbose: jboolean,
  gpu_layers_j: jint,
  disable_gpu: jboolean,
  allow_download: jboolean,
  path_j: JString<'a>,
  prompt_j: JString<'a>,
  hugging_face_repo_j: JString<'a>,
  hugging_face_name_j: JString<'a>,
  hugging_face_token_j: JString<'a>,
  ctx_size: jint,
  thread_count: jint,
  thread_batch_count: jint,
  length: jint,
  seed_j: jint,
) -> jstring {
  // resolve params
  let model_params = {
    #[cfg(any(feature = "cuda", feature = "vulkan"))]
    if !(disable_gpu == JNI_TRUE) {
      LlamaModelParams::default().with_n_gpu_layers(gpu_layers_j)
    } else {
      LlamaModelParams::default()
    }
    #[cfg(not(any(feature = "cuda", feature = "vulkan")))]
    LlamaModelParams::default()
  };
  if DEBUG_LOGS {
    eprintln!("gpu layers: {:?}", gpu_layers_j);
    eprintln!("disable gpu: {:?}", disable_gpu);
    eprintln!("allow download: {:?}", allow_download);
    eprintln!("model params: {:?}", model_params);
  }

  // resolve input values
  let path_str: String = env
    .get_string(&path_j)
    .expect("path string is required")
    .into();
  let prompt_str: String = env
    .get_string(&prompt_j)
    .expect("prompt string is required")
    .into();
  let hugging_repo: String = env
    .get_string(&hugging_face_repo_j)
    .expect("hface repo string is required")
    .into();
  let hugging_name: String = env
    .get_string(&hugging_face_name_j)
    .expect("hface name string is required")
    .into();
  let hface_token: String = env
    .get_string(&hugging_face_token_j)
    .expect("hface token string is required")
    .into();

  if DEBUG_LOGS {
    eprintln!("huggingface repo: {:?}", hugging_repo);
    eprintln!("huggingface name: {:?}", hugging_name);
    eprintln!("huggingface token: {:?}", hface_token);
    eprintln!("path: {:?}", path_str);
    eprintln!("would prompt with: {:?}", prompt_str);
  }

  // if `hugging_repo` is non-empty, we configure a hugging face model
  let model = if !hugging_repo.is_empty() {
    Model::HuggingFace {
      repo: hugging_repo,
      model: hugging_name,
    }
  } else {
    Model::Local {
      path: path_str.into(),
    }
  };
  // pin for use
  let model_params = pin!(model_params);

  // execute synchronously via tokio
  let prepared_model: Result<PathBuf, Box<dyn std::error::Error>> =
    tokio::runtime::Builder::new_current_thread()
      .enable_all()
      .build()
      .unwrap()
      .block_on(async { prep_model(model.clone()).await });

  let ctx_size_effective = if ctx_size > 0 {
    ctx_size
  } else {
    DEFAULT_CONTEXT_WINDOW as i32
  };
  let thread_count_effective = if thread_count > 0 {
    thread_count
  } else {
    DEFAULT_THREADS
  };
  let thread_batch_count_effective = if thread_batch_count > 0 {
    thread_batch_count
  } else {
    DEFAULT_THREAD_BATCH
  };
  let length_effective = if length > 0 { length } else { DEFAULT_LENGTH };
  let seed_effective: u32 = if seed_j > 0 {
    seed_j as u32
  } else {
    DEFAULT_SEED
  };

  if DEBUG_LOGS {
    eprintln!("ctx size: {:?}", ctx_size_effective);
    eprintln!("thread count: {:?}", thread_count_effective);
    eprintln!("thread batch count: {:?}", thread_batch_count_effective);
    eprintln!("length: {:?}", length_effective);
  }

  match prepared_model {
    Ok(model_path) => {
      if DEBUG_LOGS {
        eprintln!("model path: {:?}", model_path);
      }

      let result: Result<Option<String>, Error> = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .unwrap()
        .block_on(async {
          do_infer(
            verbose,
            model_params,
            prompt_str,
            model_path,
            NonZeroU32::new(ctx_size_effective as u32),
            Some(thread_count_effective),
            Some(thread_batch_count_effective),
            Some(length_effective),
            Some(seed_effective),
            None, // no callback (sync mode)
          )
          .await
        });

      match result {
        Ok(result) => {
          let out = result.unwrap();
          if DEBUG_LOGS {
            eprintln!("result: {:?}", out);
          }
          let ret = env.new_string(out).expect("unable to create result string");
          (**ret).into()
        }
        Err(e) => {
          if DEBUG_LOGS {
            eprintln!("inference failed (sync): {:?}", e);
          }
          let ret = env
            .new_string(e.to_string())
            .expect("unable to create error string");
          (**ret).into()
        }
      }
    }
    Err(e) => {
      eprintln!("inference failed (sync): {:?}", e);
      let ret = env
        .new_string(e.to_string())
        .expect("unable to create error string");
      (**ret).into()
    }
  }
}
