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

#ifndef __elide_header_plugin_api_h__
#define __elide_header_plugin_api_h__

#define ELIDE_PLUGIN_API_VERSION 1

typedef struct runtime_init {
  int f_apiversion;
  void* f_elide_boot_handle;
} el_runtime_init;

typedef struct lang_invoke {
  int f_apiversion;
  void* f_truffle_engine_handle;
  void* f_truffle_context_handle;
  void* f_elide_dispatch_handle;
} el_lang_invoke;

typedef struct lang_engine_config {
  int f_apiversion;
  void* f_truffle_engine_builder_handle;
} el_lang_engine_config;

typedef struct lang_context_config {
  int f_apiversion;
  void* f_truffle_engine_handle;
  void* f_truffle_context_builder_handle;
} el_lang_context_config;

typedef struct lang_info {
  int f_apiversion;
  char* f_lang_id;
  void (*f_init)(void *thread);
  void (*f_engine)(void *thread, el_lang_engine_config* invocation);
  void (*f_context)(void *thread, el_lang_context_config* invocation);
  void (*f_entry)(void *thread, el_lang_invoke* invocation);
} el_lang_info;

#endif // __elide_header_plugin_api_h__
