#ifndef ELIDE_EMBEDDED_H
#define ELIDE_EMBEDDED_H

// error codes
#define ELIDE_OK 0
#define ELIDE_ERR_UNKNOWN 1
#define ELIDE_ERR_UNINITIALIZED 2
#define ELIDE_ERR_ALREADY_INITIALIZED 3

/* Defines the version of the interop protocol for the runtime. */
typedef enum {
  /* Defines version 1.0 of the embedded protocol. */
  V1_0,
} elide_protocol_version_t;

/* Defines the format for exchanging data with the runtime. */
typedef enum {
  /* Selects the Protobuf format for data exchange in runtime operations. */
  PROTOBUF,

  /* Selects the Cap'n'Proto format for data exchange in runtime operations. */
  CAPNPROTO,
} elide_protocol_format_t;

/* Defines the dispatch mode used by a guest app. */
typedef enum {
  /*
   * Use a fetch-style invocation API, where guest code exposes a 'fetch'
   * function that handles incoming requests.
   */
  FETCH,
} elide_app_mode_t;

/* Defines the programming language used by a guest application. */
typedef enum {
  /* Use JavaScript as guest language. */
  JS,

  /* Use Python as guest language. */
  PYTHON,
} elide_app_lang_t;

/* Configuration struct for the embedded runtime. */
typedef struct elide_config_t {
  /* Dispatch protocol version. */
  elide_protocol_version_t version;

  /* Dispatch protocol serial format. */
  elide_protocol_format_t format;

  /** Path to the guest resources directory. */
  char *guest_root;
} elide_config_t;

/* Configuration struct for an embedded application. */
typedef struct elide_app_config_t {
  /* Unique identifier for the application. */
  char *id;

  /* Path string, relative to the application root, of the application entrypoint. */
  char *entrypoint;

  /* Language of the guest application. */
  elide_app_lang_t language;

  /* Dispatch style for the application (e.g. 'fetch'). */
  elide_app_mode_t mode;
} elide_app_config_t;

#endif // ELIDE_EMBEDDED_H