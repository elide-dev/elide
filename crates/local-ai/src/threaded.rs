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
use jni::objects::{JClass, JString, JValue};
use jni::sys::{jboolean, jint, jlong};
use lazy_static::lazy_static;
use llama_cpp_2::model::params::LlamaModelParams;
use std::num::NonZeroU32;
use std::pin::pin;
use tokio::runtime::{Builder, Runtime};

#[cfg(feature = "unsafe")]
use {jni::signature::Primitive::Void, jni::signature::ReturnType};

use crate::{
  DEBUG_LOGS, DEFAULT_CONTEXT_WINDOW, DEFAULT_LENGTH, DEFAULT_SEED, DEFAULT_THREAD_BATCH,
  DEFAULT_THREADS, Model, do_infer, prep_model,
};

lazy_static! {
  static ref INF_ENGINE: Runtime = Builder::new_multi_thread().enable_all().build().unwrap();
}

#[cfg(not(feature = "nonblocking"))]
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn inferAsync<'a>() {
  // no-op
}

/// Infer asynchronously against the configured model backend.
#[cfg(feature = "nonblocking")]
#[jni("elide.runtime.localai.NativeLocalAi")]
pub fn inferAsync<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  op_id: jint,
  verbose: jboolean,
  gpu_layers_j: jint,
  disable_gpu: jboolean,
  allow_download: jboolean,
  path_j: JString<'a>,
  prompt_j: JString<'a>,
  hugging_face_repo_j: JString<'a>,
  hugging_face_name_j: JString<'a>,
  hugging_face_token_j: JString<'a>,
  ctx_size_j: jint,
  thread_count_j: jint,
  thread_batch_count_j: jint,
  length_j: jint,
  seed_j: jlong,
) -> jint {
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

  let ctx_size_effective = if ctx_size_j > 0 {
    ctx_size_j
  } else {
    DEFAULT_CONTEXT_WINDOW as i32
  };
  let thread_count_effective = if thread_count_j > 0 {
    thread_count_j
  } else {
    DEFAULT_THREADS
  };
  let thread_batch_count_effective = if thread_batch_count_j > 0 {
    thread_batch_count_j
  } else {
    DEFAULT_THREAD_BATCH
  };
  let length_effective = if length_j > 0 {
    length_j
  } else {
    DEFAULT_LENGTH
  };
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

  // Create a global reference that outlives the current JNI call
  let vm = env.get_java_vm().expect("failed to get java vm");

  let cbk = Box::new(move |chunk_result: Result<String, anyhow::Error>| {
    // re-attach to jvm as we may be in a new thread
    let mut env = match vm.attach_current_thread() {
      Ok(env) => env,
      Err(e) => {
        eprintln!("Failed to attach to JVM: {:?}", e);
        return;
      }
    };
    let cbk_cls = env
      .find_class("elide/runtime/localai/InferenceCallbackRegistry")
      .expect("failed to find callback class");
    match chunk_result {
      Ok(tokens) => {
        let chunk_jstr = match env.new_string(tokens) {
          Ok(s) => s,
          Err(e) => {
            eprintln!("Failed to create Java string: {:?}", e);
            return;
          }
        };
        {
          let _ = env
            .call_static_method(
              cbk_cls,
              "onChunkReady",
              "(ILjava/lang/String;)V",
              &[JValue::Int(op_id), JValue::Object(&chunk_jstr)],
            )
            .expect("failed to dispatch safe callback");
        }
      }

      Err(err) => {
        if let Ok(exc) = env.find_class("java/lang/RuntimeException") {
          let _ = env.throw_new(exc, format!("inference failed: {:?}", err));
        } else {
          eprintln!("Failed to find exception class");
        }
      }
    };

    if DEBUG_LOGS {
      eprintln!("jvm inference callback dispatched")
    }
  });

  INF_ENGINE.block_on(async {
    let model_path = prep_model(model.clone())
      .await
      .expect("failed to prepare model");
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
      Some(cbk),
    )
    .await
    .expect("inference failed");
  });
  op_id
}
