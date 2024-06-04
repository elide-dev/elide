/* Generate this file with:
cbindgen --config ./tools/umbrella/cbindgen.toml --crate umbrella --output packages/cli-bridge/src/main/public/umbrella.h --cpp-compat tools/umbrella */
#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include "jni.h"
#include "jni_md.h"

#ifdef __cplusplus
extern "C" {
#endif // __cplusplus

jstring Java_dev_elide_cli_bridge_CliNativeBridge_libVersion(JNIEnv env, JClass _class);

jstring Java_dev_elide_cli_bridge_CliNativeBridge_apiVersion(JNIEnv env, JClass _class);

jobjectArray Java_dev_elide_cli_bridge_CliNativeBridge_supportedTools(JNIEnv env, JClass _class);

jobjectArray Java_dev_elide_cli_bridge_CliNativeBridge_relatesTo(JNIEnv env,
                                                                 JClass _class,
                                                                 JString tool);

jstring Java_dev_elide_cli_bridge_CliNativeBridge_toolVersion(JNIEnv env,
                                                              JClass _class,
                                                              JString tool);

jint Java_dev_elide_cli_bridge_CliNativeBridge_runToolOnFile(JNIEnv env,
                                                             JClass _class,
                                                             JString tool,
                                                             JString file);

#ifdef __cplusplus
} // extern "C"
#endif // __cplusplus
