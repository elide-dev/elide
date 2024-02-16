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

/**
 * # Elide Embedded: Native API
 *
 * This header defines the native API by which host applications can create, manage, and interact with an embedded Elide
 * runtime instance.
 */

#ifndef __ELIDE
#define __ELIDE

/**
 *
 */
typedef enum {
  ELIDE_PROTOBUF = 0,
  ELIDE_CAPNPROTO,
} elide_protocol_mode_t;

/**
 *
 */
typedef enum {
  ELIDE_INFLIGHT_PENDING,
  ELIDE_INFLIGHT_EXECUTING,
  ELIDE_INFLIGHT_ERR,
  ELIDE_INFLIGHT_COMPLETED,
} elide_invocation_status_t;

/**
 *
 */
typedef enum {
  ELIDE_BASELINE,
} elide_embedded_capability;

/**
 *
 */
struct elide_inflight_call {
  /** */
  void* f_call_handle;
};

typedef struct elide_inflight_call elide_inflight_call_t;

/**
 *
 */
struct elide_invocation {
  /** */
  long f_rq;

  /** */
  elide_protocol_mode_t f_mode;

  /** */
  unsigned long f_size;

  /** */
  elide_invocation_status_t f_status;

  /** */
  unsigned char* (*fn_tip)(void* thread, struct elide_invocation* invocation);

  /** */
  unsigned char* (*fn_consume)(void* thread, struct elide_invocation* invocation, int index);

  /** */
  void* f_payload_tip;
};

typedef struct elide_invocation elide_invocation_t;

/**
 *
 */
struct elide_configuration {
  /** */
  unsigned char* f_version;

  /** */
  elide_protocol_mode_t f_mode;

  /** */
  unsigned long f_size;

  /** */
  unsigned char* (*fn_tip)(void* thread, struct elide_invocation* invocation);

  /** */
  unsigned char* (*fn_consume)(void* thread, struct elide_invocation* invocation, int index);

  /** */
  void* f_config_tip;
};

typedef struct elide_configuration elide_configuration_t;

#endif
