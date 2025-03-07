#ifndef __LIBENTRY_H
#define __LIBENTRY_H

#include <stdbool.h>
#include <stdint.h>
#include <graal_isolate.h>


#if defined(__cplusplus)
extern "C" {
#endif

int32_t elide_entry_init(graal_isolatethread_t*);

int32_t elide_entry_run(graal_isolatethread_t*);

#if defined(__cplusplus)
}
#endif
#endif
