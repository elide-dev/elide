#ifndef __LIBELIDEMAIN_H
#define __LIBELIDEMAIN_H

#include <graal_isolate_dynamic.h>


#if defined(__cplusplus)
extern "C" {
#endif

typedef int (*elide_main_init_fn_t)(graal_isolatethread_t* thread);

typedef int (*elide_main_entry_fn_t)(graal_isolatethread_t* thread, el_entry_invocation* invocation);

typedef int (*detach_thread_fn_t)(graal_isolatethread_t* thread);

typedef graal_isolatethread_t* (*create_isolate_fn_t)();

typedef void (*tear_down_isolate_fn_t)(graal_isolatethread_t* thread);

typedef graal_isolate_t* (*get_isolate_fn_t)(graal_isolatethread_t* thread);

typedef graal_isolatethread_t* (*attach_thread_fn_t)(graal_isolate_t* isolate);

typedef graal_isolatethread_t* (*get_current_thread_fn_t)(graal_isolate_t* isolate);

#if defined(__cplusplus)
}
#endif
#endif
