#ifndef __LIBENTRY_H
#define __LIBENTRY_H

#include <stdbool.h>
#include <stdint.h>
#include <graal_isolate_dynamic.h>


#if defined(__cplusplus)
extern "C" {
#endif

typedef int32_t (*elide_entry_init_fn_t)(graal_isolatethread_t*);

typedef int32_t (*elide_entry_run_fn_t)(graal_isolatethread_t*);

#if defined(__cplusplus)
}
#endif
#endif
