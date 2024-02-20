#ifndef ELIDE_EMBEDDED_H
#define ELIDE_EMBEDDED_H

// error codes
#define ELIDE_OK 0
#define ELIDE_ERR_UNKNOWN 1
#define ELIDE_ERR_UNINITIALIZED 2
#define ELIDE_ERR_ALREADY_INITIALIZED 3

/* Defines the version of the interop protocol for the runtime. */
enum elide_protocol_version_t {
  /* Defines version 1.0 of the embedded protocol. */
  V1_0,
};

/* Defines the format for exchanging data with the runtime. */
enum elide_protocol_format_t {
  /* Selects the Protobuf format for data exchange in runtime operations. */
  PROTOBUF,

  /* Selects the Cap'n'Proto format for data exchange in runtime operations. */
  CAPNPROTO,
};

/* Defines the dispatch mode used by a guest app. */
enum elide_app_mode_t {
  /*
   * Use a fetch-style invocation API, where guest code exposes a 'fetch'
   * function that handles incoming requests.
   */
  FETCH,
};

/* Configuration struct for the embedded runtime. */
typedef struct elide_config_t {
  /* Dispatch protocol version. */
  enum elide_protocol_version_t version;

  /* Dispatch protocol serial format. */
  enum elide_protocol_format_t format;

  /** Path to the guest resources directory. */
  char *guest_root;
} elide_config_t;

typedef struct elide_app_config_t {
  char *id;

  char *entrypoint;

  char *language;

  enum elide_app_mode_t mode;
} elide_app_config_t;

#endif // ELIDE_EMBEDDED_H