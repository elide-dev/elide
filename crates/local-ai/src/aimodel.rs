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

use anyhow::{Context, Result};
use hf_hub::api::tokio::ApiBuilder;
use std::path::PathBuf;

/// Implements model-related parameters for CLI use
#[derive(clap::Subcommand, Debug, Clone)]
pub enum Model {
  /// Use an already downloaded model
  Local {
    /// The path to the model. e.g. `/.../models--TheBloke--Llama-2-7B-Chat-GGUF/blobs/<hash>`
    path: PathBuf,
  },
  /// Download a model from huggingface (or use a cached version)
  #[clap(name = "hf-model")]
  HuggingFace {
    /// the repo containing the model. e.g. `TheBloke/Llama-2-7B-Chat-GGUF`
    repo: String,
    /// the model name. e.g. `llama-2-7b-chat.Q4_K_M.gguf`
    model: String,
  },
}

// Implements `Model` download or resolve.
impl Model {
  /// Convert the model to a path - may download from huggingface
  pub(crate) async fn get_or_load(self) -> Result<PathBuf> {
    match self {
      Model::Local { path } => Ok(path),
      Model::HuggingFace { model, repo } => match ApiBuilder::new()
        .with_progress(true)
        .build()
        .with_context(|| "unable to create huggingface api")?
        .model(repo)
        .get(&model)
        .await
      {
        Ok(v) => Ok(v),
        Err(e) => {
          eprintln!("Error downloading model: {:?}", e);
          Err(anyhow::anyhow!("Error downloading model: {:?}", e))
        }
      },
    }
  }
}
