#ifndef __LIBELIDEMAIN_H
#define __LIBELIDEMAIN_H

#include <graal_isolate.h>


#if defined(__cplusplus)
extern "C" {
#endif

int elide_main_init(graal_isolatethread_t* thread);

int elide_main_entry(graal_isolatethread_t* thread, el_entry_invocation* invocation);

int detach_thread(graal_isolatethread_t* thread);

graal_isolatethread_t* create_isolate();

void tear_down_isolate(graal_isolatethread_t* thread);

graal_isolate_t* get_isolate(graal_isolatethread_t* thread);

graal_isolatethread_t* attach_thread(graal_isolate_t* isolate);

graal_isolatethread_t* get_current_thread(graal_isolate_t* isolate);

#if defined(__cplusplus)
}
#endif
#endif
